import base64
import json
import time
import uuid
import os
import requests
from cryptography.hazmat.primitives import hashes, serialization
from cryptography.hazmat.primitives.asymmetric import rsa, padding

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
PRIVATE_KEY_PATH = os.path.join(BASE_DIR, "private_key.pem")
PUBLIC_KEY_PATH = os.path.join(BASE_DIR, "public_key.pem")

# Cache em memória de chaves públicas de servidores parceiros
PUBLIC_KEYS_CACHE = {}
PROCESSED_NONCES = set()

def ensure_keys_exist():
    if not os.path.exists(PRIVATE_KEY_PATH) or not os.path.exists(PUBLIC_KEY_PATH):
        private_key = rsa.generate_private_key(public_exponent=65537, key_size=2048)

        with open(PRIVATE_KEY_PATH, "wb") as f:
            f.write(private_key.private_bytes(
                encoding=serialization.Encoding.PEM,
                format=serialization.PrivateFormat.PKCS8,
                encryption_algorithm=serialization.NoEncryption()
            ))

        with open(PUBLIC_KEY_PATH, "wb") as f:
            f.write(private_key.public_key().public_bytes(
                encoding=serialization.Encoding.PEM,
                format=serialization.PublicFormat.SubjectPublicKeyInfo
            ))

ensure_keys_exist()

def get_my_private_key():
    with open(PRIVATE_KEY_PATH, "rb") as f:
        return serialization.load_pem_private_key(f.read(), password=None)

def get_my_public_key_pem():
    with open(PUBLIC_KEY_PATH, "r") as f:
        return f.read()

def sign_payload(origin_url, payload):
    timestamp = str(int(time.time()))
    nonce = str(uuid.uuid4())

    body_bytes = json.dumps(payload, sort_keys=True).encode('utf-8')
    data_to_sign = f"{timestamp}.{nonce}.".encode('utf-8') + body_bytes

    private_key = get_my_private_key()
    signature = private_key.sign(
        data_to_sign,
        padding.PKCS1v15(),
        hashes.SHA256()
    )

    return {
        "X-Federation-Origin": origin_url,
        "X-Federation-Timestamp": timestamp,
        "X-Federation-Nonce": nonce,
        "X-Federation-Signature": base64.b64encode(signature).decode('utf-8')
    }

def verify_incoming_request(request):
    headers = request.headers
    origin = headers.get("X-Federation-Origin")
    timestamp = headers.get("X-Federation-Timestamp")
    nonce = headers.get("X-Federation-Nonce")
    signature_b64 = headers.get("X-Federation-Signature")

    if not all([origin, timestamp, nonce, signature_b64]):
        return False, "Cabeçalhos de autenticação federada ausentes", 401

    # Proteção contra Replay Attack (Anti-Bot)
    now = int(time.time())
    if abs(now - int(timestamp)) > 30:
        return False, "Requisição expirada (Janela > 30s)", 403

    if nonce in PROCESSED_NONCES:
        return False, "Requisição duplicada detectada (Anti-Bot)", 403
    PROCESSED_NONCES.add(nonce)

    # Obter Chave Pública do Servidor de Origem (Confirma se o servidor realmente existe)
    public_key_pem = PUBLIC_KEYS_CACHE.get(origin)
    if not public_key_pem:
        try:
            resp = requests.get(f"{origin}/.well-known/federation-key", timeout=5)
            if resp.status_code == 200:
                public_key_pem = resp.json().get("public_key")
                PUBLIC_KEYS_CACHE[origin] = public_key_pem
            else:
                return False, f"Servidor {origin} não respondeu ao handshake", 404
        except Exception as e:
            return False, f"Falha ao conectar no nó de origem {origin}: {str(e)}", 502

    # Validar Assinatura Criptográfica
    try:
        public_key = serialization.load_pem_public_key(public_key_pem.encode('utf-8'))

        payload_data = request.get_json(silent=True) or {}
        body_bytes = json.dumps(payload_data, sort_keys=True).encode('utf-8')
        data_expected = f"{timestamp}.{nonce}.".encode('utf-8') + body_bytes

        signature = base64.b64decode(signature_b64)

        public_key.verify(
            signature,
            data_expected,
            padding.PKCS1v15(),
            hashes.SHA256()
        )
        return True, "OK", 200
    except Exception:
        return False, "Assinatura inválida! Origem não autenticada.", 403
