import os
import uuid
from flask import Flask, request, jsonify, Blueprint
from supabase import create_client, Client
import dotenv

dotenv.load_dotenv("backend.env")
url = os.getenv("SUPABASE_URL")
key = os.getenv("SUPABASE_KEY")

supabase: Client = create_client(url, key)
image_bp = Blueprint("image_bp", __name__)
import io
import requests
from flask import Flask, request, Response
from PIL import Image
import re

@image_bp.route('/lite-render', methods=['GET'])
def lite_render():
    url = request.args.get('url', '')
    if not url:
        return "URL vazia", 400

    # 1. Limpa entidades HTML (&amp; -> &) se a URL vier codificada da API do Reddit
    clean_url = url.replace('&amp;', '&')

    # 2. Converte preview.redd.it para o CDN direto i.redd.it
    if 'preview.redd.it' in clean_url:
        # Extrai a hash/nome da imagem (ex: 5lw7ajy5awph1.jpeg)
        match = re.search(r'preview\.redd\.it/([a-zA-Z0-9]+\.(?:jpg|jpeg|png|webp))', clean_url)
        if match:
            clean_url = f"https://i.redd.it/{match.group(1)}"
        else:
            # Fallback simples: remove os parametros de query
            clean_url = clean_url.split('?')[0].replace('preview.redd.it', 'i.redd.it')

    # 3. Faz a requisicao com o User-Agent que aprovamos no cURL
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0'
    }

    try:
        res = requests.get(clean_url, headers=headers, stream=True, timeout=8)
        if res.status_code == 200:
            return Response(
                res.raw.read(),
                content_type=res.headers.get('Content-Type', 'image/jpeg')
            )
        return f"Erro CDN: {res.status_code}", res.status_code
    except Exception as e:
        return f"Erro interno: {str(e)}", 500
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
            file_options={"content-type": file.content_type}
        )
        
        public_url = supabase.storage.from_(bucket_name).get_public_url(unique_file_name)
        
        return jsonify({
            "status": "success",
            "image_url": public_url
        }), 200

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500