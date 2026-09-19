import io
import os
import urllib.parse
import uuid
import dotenv
import requests
from flask import Blueprint, Response, jsonify, request
from PIL import Image, ImageOps
from supabase import Client, create_client

dotenv.load_dotenv("backend.env")
supabase: Client = create_client(
    os.getenv("SUPABASE_URL"), os.getenv("SUPABASE_KEY")
)

image_bp = Blueprint("image_bp", __name__)
WORKER_BASE_URL = "https://fancy-fire-49d2.luizsgustavo76.workers.dev/?url="


def clean_reddit_url(url_str):
    url_str = urllib.parse.unquote(url_str).replace("&amp;", "&").strip()
    if "i.redd.it" in url_str:
        return url_str.split("?")[0]
    if "redd.it" in url_str and url_str.count("?") > 1:
        base, rest = url_str.split("?", 1)
        url_str = f"{base}?{rest.replace('?', '&')}"
    return url_str


@image_bp.route("/lite-render", methods=["GET"])
def lite_render():
    raw_url = request.args.get("url")
    if not raw_url:
        return jsonify({"error": "URL missing"}), 400

    target_url = clean_reddit_url(raw_url)
    worker_url = WORKER_BASE_URL + requests.utils.quote(target_url, safe="")

    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/123.0.0.0 Safari/537.36",
        "Accept": "image/avif,image/webp,image/apng,image/*,*/*;q=0.8",
    }

    try:
        response = requests.get(
            worker_url, headers=headers, timeout=15, allow_redirects=True
        )
        if response.status_code != 200 or not response.content:
            return jsonify({"error": "Failed to fetch image from worker"}), 502
    except Exception as e:
        return jsonify({"error": "Worker request failed", "details": str(e)}), 502

    try:
        image = Image.open(io.BytesIO(response.content))

        try:
            image = ImageOps.exif_transpose(image)
        except Exception:
            pass

        if image.mode in ("P", "RGBA", "LA"):
            image = image.convert("RGBA")
            background = Image.new("RGB", image.size, (255, 255, 255))
            background.paste(image, (0, 0), image.getchannel("A"))
            image = background
        elif image.mode != "RGB":
            image = image.convert("RGB")

        jpeg_buffer = io.BytesIO()
        image.save(jpeg_buffer, format="JPEG", quality=88, optimize=True)
        jpeg_buffer.seek(0)

        # Resposta configurada para HTTP puro (compatível com Android 2.x)
        resp = Response(
            jpeg_buffer.getvalue(),
            status=200,
            mimetype="image/jpeg"
        )
        resp.headers["Cache-Control"] = "no-store, no-cache, must-revalidate"
        resp.headers["Content-Type"] = "image/jpeg"
        # Desativa redirecionamentos forçados para HTTPS
        resp.headers["Strict-Transport-Security"] = "" 
        
        return resp

    except Exception as e:
        return jsonify({"error": "Image processing failed", "details": str(e)}), 500
@image_bp.route("/upload-image", methods=["POST"])
def upload_image():
    if "image" not in request.files or not request.files["image"].filename:
        return jsonify({"error": "Invalid or missing image file"}), 400

    file = request.files["image"]
    try:
        ext = os.path.splitext(file.filename)[1]
        file_name = f"post_{uuid.uuid4().hex}{ext}"
        bucket = "linka-media"

        supabase.storage.from_(bucket).upload(
            path=file_name,
            file=file.read(),
            file_options={"content-type": file.content_type},
        )
        public_url = supabase.storage.from_(bucket).get_public_url(file_name)

        return jsonify({"status": "success", "image_url": public_url}), 200
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500