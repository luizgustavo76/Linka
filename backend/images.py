import os
import urllib.parse
import uuid
import boto3
from botocore.config import Config
import dotenv
from flask import Blueprint, jsonify, redirect, request

dotenv.load_dotenv("backend.env")

B2_KEY_ID = os.getenv("B2_KEY_ID")
B2_APPLICATION_KEY = os.getenv("B2_APPLICATION_KEY")
B2_ENDPOINT = os.getenv("B2_ENDPOINT", "https://s3.us-east-005.backblazeb2.com").rstrip("/")
BUCKET_NAME = os.getenv("B2_BUCKET", "linka-media")
MY_DOMINIO_PROXY = os.getenv("MY_DOMINIO_PROXY", "https://linkaProject.pythonanywhere.com")

b2_config = Config(
    signature_version='s3v4',
    s3={
        'addressing_style': 'path',
        'payload_signing_enabled': False
    },
    retries={'max_attempts': 3, 'mode': 'standard'}
)

s3_client = boto3.client(
    "s3",
    endpoint_url=B2_ENDPOINT,
    aws_access_key_id=B2_KEY_ID,
    aws_secret_access_key=B2_APPLICATION_KEY,
    config=b2_config
)

image_bp = Blueprint("image_bp", __name__)


def get_proxy_url(filename):
    return f"{MY_DOMINIO_PROXY}/midia/{filename}"


@image_bp.route("/midia/<filename>", methods=["GET"])
def proxy_midia(filename):
    try:
        # Gera uma URL assinada válida por 3600 segundos (1 hora)
        # O processamento leva menos de 2 milissegundos no Flask
        presigned_url = s3_client.generate_presigned_url(
            'get_object',
            Params={'Bucket': BUCKET_NAME, 'Key': filename},
            ExpiresIn=3600
        )
        # Redireciona o navegador do cliente direto pro Backblaze B2
        return redirect(presigned_url, code=302)
    except Exception as e:
        return jsonify({"error": "Media not found", "details": str(e)}), 404


def clean_reddit_url(url_str):
    url_str = urllib.parse.unquote(url_str).replace("&amp;", "&").strip()
    if "i.redd.it" in url_str:
        return url_str.split("?")[0]
    if "redd.it" in url_str and url_str.count("?") > 1:
        base, rest = url_str.split("?", 1)
        url_str = f"{base}?{rest.replace('?', '&')}"
    return url_str


def normalize_url(url_str):
    if url_str.startswith("https//"):
        url_str = "https://" + url_str[7:]
    elif url_str.startswith("http//"):
        url_str = "http://" + url_str[6:]
    elif not url_str.startswith(("http://", "https://")):
        url_str = "https://" + url_str
    return url_str


def get_domain(url_str):
    parsed = urllib.parse.urlparse(url_str)
    return parsed.netloc.lower()


@image_bp.route("/lite-render", methods=["GET"])
def lite_render():
    raw_url = request.args.get("url")
    if not raw_url:
        return jsonify({"error": "URL missing"}), 400

    target_url = urllib.parse.unquote(raw_url).replace("&amp;", "&").strip()
    while "lite-render?url=" in target_url:
        target_url = target_url.split("lite-render?url=")[-1]
        target_url = urllib.parse.unquote(target_url)

    target_url = normalize_url(target_url)
    target_url = clean_reddit_url(target_url)
    domain = get_domain(target_url)

    allowed_domains = ["backblazeb2.com", "i.redd.it", "preview.redd.it", "redd.it"]
    is_allowed = any(domain.endswith(d) or domain == d for d in allowed_domains)

    if not is_allowed:
        return redirect(target_url, code=302)

    return redirect(target_url, code=302)


@image_bp.route("/upload-image", methods=["POST"])
def upload_image():
    if "image" not in request.files:
        return jsonify({"error": "Missing 'image' field in multipart request"}), 400

    file = request.files["image"]
    raw_bytes = file.read()
    if not raw_bytes or len(raw_bytes) == 0:
        return jsonify({"error": "File is empty"}), 400

    filename = (file.filename or "image.jpg").lower()
    file_ext = os.path.splitext(filename)[1]

    mime_types = {
        ".jpg": "image/jpeg",
        ".jpeg": "image/jpeg",
        ".png": "image/png",
        ".webp": "image/webp",
    }

    content_type = file.content_type or ""

    if file_ext in mime_types:
        ct = mime_types[file_ext]
        ext = ".jpg" if file_ext == ".jpeg" else file_ext
    elif "png" in content_type:
        ct, ext = "image/png", ".png"
    elif "webp" in content_type:
        ct, ext = "image/webp", ".webp"
    elif "jpeg" in content_type or "jpg" in content_type:
        ct, ext = "image/jpeg", ".jpg"
    else:
        if raw_bytes.startswith(b"\xff\xd8\xff"):
            ct, ext = "image/jpeg", ".jpg"
        elif raw_bytes.startswith(b"\x89PNG\r\n\x1a\n"):
            ct, ext = "image/png", ".png"
        elif raw_bytes.startswith(b"RIFF") and b"WEBP" in raw_bytes[:16]:
            ct, ext = "image/webp", ".webp"
        else:
            return jsonify({"status": "Nice try, little boy; Big Brother is watching you..."}), 400

    try:
        file_name = f"post_{uuid.uuid4().hex}{ext}"

        s3_client.put_object(
            Bucket=BUCKET_NAME,
            Key=file_name,
            Body=raw_bytes,
            ContentType=ct,
        )

        public_url = get_proxy_url(file_name)

        return jsonify({"status": "success", "image_url": public_url}), 200
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500