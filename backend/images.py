import os
import urllib.parse
import uuid
import dotenv
import requests
from flask import Blueprint, Response, jsonify, redirect, request
from supabase import Client, create_client

dotenv.load_dotenv("backend.env")
supabase: Client = create_client(
    os.getenv("SUPABASE_URL"), os.getenv("SUPABASE_KEY")
)

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

    allowed_domains = ["supabase.co", "i.redd.it", "preview.redd.it", "redd.it"]
    is_allowed = any(domain.endswith(d) or domain == d for d in allowed_domains)

    if not is_allowed:
        return redirect(target_url, code=302)

    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/123.0.0.0 Safari/537.36",
        "Accept": "image/avif,image/webp,image/apng,image/*,*/*;q=0.8",
    }

    try:
        # Busca direta da imagem sem Cloudflare Worker
        response = requests.get(target_url, headers=headers, timeout=12)

        if response.status_code != 200 or not response.content:
            return jsonify({
                "error": "Failed to fetch image directly", 
                "status_code": response.status_code
            }), 502

        content_type = response.headers.get("Content-Type", "").lower()

        if not content_type.startswith("image/"):
            return jsonify({
                "error": "Target URL is not a valid image",
                "received_content_type": content_type
            }), 502

        resp = Response(
            response.content,
            status=200,
            mimetype=content_type,
        )
        resp.headers["Cache-Control"] = "public, max-age=86400"
        resp.headers["Content-Type"] = content_type

        return resp

    except Exception as e:
        print(f"[DEBUG] Exception na rota lite-render: {e}", flush=True)
        return jsonify({"error": "Direct image fetch failed", "details": str(e)}), 502


@image_bp.route("/upload-image", methods=["POST"])
def upload_image():
    if "image" not in request.files or not request.files["image"].filename:
        return jsonify({"error": "Invalid or missing image file"}), 400

    file = request.files["image"]
    secure_ext = [".jpg", ".jpeg", ".webp", ".png"]
    if not any(file.filename.lower().endswith(ext) for ext in secure_ext):
        return jsonify({"status": "Nice try, little boy; Big Brother is watching you..."}), 400
    try:
        raw_bytes = file.read()

        file_ext = os.path.splitext(file.filename)[1].lower()
        if file_ext == ".jpeg":
            file_ext = ".jpg"

        file_name = f"post_{uuid.uuid4().hex}{file_ext}"
        bucket = "linka-media"

        mime_types = {
            ".jpg": "image/jpeg",
            ".png": "image/png",
            ".webp": "image/webp",
        }
        content_type = mime_types.get(file_ext, "image/jpeg")

        supabase.storage.from_(bucket).upload(
            path=file_name,
            file=raw_bytes,
            file_options={"content-type": content_type},
        )
        public_url = supabase.storage.from_(bucket).get_public_url(file_name)

        return jsonify({"status": "success", "image_url": public_url}), 200
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500