import os
import urllib.parse
import uuid
import dotenv
from flask import Blueprint, jsonify, redirect, request
from supabase import Client, create_client

dotenv.load_dotenv("backend.env")
supabase: Client = create_client(
    os.getenv("SUPABASE_URL"), os.getenv("SUPABASE_KEY")
)

# URL do seu Cloudflare Worker (pode salvar no backend.env ou usar a padrão)
WORKER_URL = os.getenv(
    "CLOUDFLARE_WORKER_URL", 
    "https://fancy-fire-49d2.luizsgustavo76.workers.dev"
).rstrip("/")

image_bp = Blueprint("image_bp", __name__)


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
    encoded_target = urllib.parse.quote(target_url, safe="")
    worker_redirect_url = f"{WORKER_URL}/?url={encoded_target}"
    return redirect(worker_redirect_url, code=302)


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
        bucket = "linka-media"

        supabase.storage.from_(bucket).upload(
            path=file_name,
            file=raw_bytes,
            file_options={"content-type": ct, "upsert": "true"},
        )
        public_url = supabase.storage.from_(bucket).get_public_url(file_name)

        return jsonify({"status": "success", "image_url": public_url}), 200
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500