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

PUBLIC_KEYS_CACHE = {}
# Armazena tuplas: (nonce, timestamp_de_insercao)
PROCESSED_NONCES = {}

def ensure_keys_exist():
    # 1. Se existir no ambiente (Render/Nuvem), ignora geração local
    if os.getenv("FEDERATION_PRIVATE_KEY"):
        return

    # 2. Fallback para desenvolvimento local
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
    env_key = os.getenv("FEDERATION_PRIVATE_KEY")
    if env_key:
        # Garante que os quebras de linha sejam interpretados corretamente
        return serialization.load_pem_private_key(env_key.encode('utf-8'), password=None)
    
    with open(PRIVATE_KEY_PATH, "rb") as f:
        return serialization.load_pem_private_key(f.read(), password=None)

def get_my_public_key_pem():
    env_key = os.getenv("FEDERATION_PUBLIC_KEY")
    if env_key:
        return env_key

    with open(PUBLIC_KEY_PATH, "r") as f:
        return f.read()

def cleanup_old_nonces(now):
    """ Remove nonces mais antigos que 60 segundos da RAM pra não estourar a memória """
    expired = [n for n, ts in PROCESSED_NONCES.items() if now - ts > 60]
    for n in expired:
        del PROCESSED_NONCES[n]

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

    try:
        req_timestamp = int(timestamp)
    except ValueError:
        return False, "Timestamp inválido", 400

    # Proteção contra Replay Attack
    now = int(time.time())
    if abs(now - req_timestamp) > 30:
        return False, "Requisição expirada (Janela > 30s)", 403

    # Limpeza preventiva de nonces antigos da memória
    cleanup_old_nonces(now)

    if nonce in PROCESSED_NONCES:
        return False, "Requisição duplicada detectada (Anti-Bot)", 403
    
    PROCESSED_NONCES[nonce] = now

    # Obter Chave Pública do Servidor de Origem
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