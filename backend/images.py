import io
import os
import re
import urllib.parse
import uuid
import dotenv
import requests
from flask import Blueprint, Flask, Response, jsonify, redirect, request
from PIL import Image
from supabase import Client, create_client

dotenv.load_dotenv("backend.env")
url = os.getenv("SUPABASE_URL")
key = os.getenv("SUPABASE_KEY")

supabase: Client = create_client(url, key)
image_bp = Blueprint("image_bp", __name__)


def clean_reddit_url(url_str):
    """Limpa e normaliza totalmente URLs de mídia do Reddit."""
    # 1. Decodifica HTML e caracteres codificados
    url_str = urllib.parse.unquote(url_str).replace("&amp;", "&").strip()

    # 2. Se for link do i.redd.it, corta qualquer parâmetro
    if "i.redd.it" in url_str:
        return url_str.split("?")[0]

    # 3. Se contiver múltiplos '?', separa a URL base dos parâmetros
    if "redd.it" in url_str and url_str.count("?") > 1:
        base, rest = url_str.split("?", 1)
        rest = rest.replace("?", "&")
        url_str = f"{base}?{rest}"

    return url_str


@image_bp.route("/lite-render", methods=["GET"])
def lite_render():
    raw_url = request.args.get("url")

    if not raw_url:
        return "URL missing", 400

    target_url = clean_reddit_url(raw_url)

    # Headers avançados simulando navegação direta no Reddit
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
        "Accept": "image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8",
        "Accept-Language": "en-US,en;q=0.9",
        "Cache-Control": "no-cache",
        "Pragma": "no-cache",
        "Referer": "https://www.reddit.com/",
        "Sec-Ch-Ua": '"Chromium";v="123", "Not:A-Brand";v="8", "Google Chrome";v="123"',
        "Sec-Ch-Ua-Mobile": "?0",
        "Sec-Ch-Ua-Platform": '"Windows"',
        "Sec-Fetch-Dest": "image",
        "Sec-Fetch-Mode": "no-cors",
        "Sec-Fetch-Site": "cross-site",
    }

    try:
        session = requests.Session()
        resp = session.get(
            target_url, headers=headers, timeout=8, allow_redirects=True
        )

        # Se o Reddit aceitou a requisição, devolve a imagem
        if resp.status_code == 200:
            content_type = resp.headers.get("Content-Type", "image/jpeg")
            return Response(resp.content, mimetype=content_type)

        print(
            f"[LITE-RENDER ERROR] Status {resp.status_code} para {target_url}. Redirecionando..."
        )

        # FALLBACK: Se o IP do servidor tomou 403 da CDN do Reddit, redireciona diretamente
        return redirect(target_url, code=302)

    except Exception as e:
        print(f"[LITE-RENDER EXCEPTION] {e}")
        # Em caso de erro de conexão, faz o fallback via redirect
        return redirect(target_url, code=302)


@image_bp.route("/upload-image", methods=["POST"])
def upload_image():
    if "image" not in request.files:
        return jsonify({"error": "No image uploaded"}), 400

    file = request.files["image"]

    if file.filename == "":
        return jsonify({"error": "File with no name"}), 400

    try:
        extension = os.path.splitext(file.filename)[1]
        unique_file_name = f"post_{uuid.uuid4().hex}{extension}"

        file_data = file.read()

        bucket_name = "linka-media"
        supabase.storage.from_(bucket_name).upload(
            path=unique_file_name,
            file=file_data,
            file_options={"content-type": file.content_type},
        )

        public_url = supabase.storage.from_(bucket_name).get_public_url(
            unique_file_name
        )

        return jsonify({"status": "success", "image_url": public_url}), 200

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500