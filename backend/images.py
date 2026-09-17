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

@image_bp.route('/lite-render', methods=['GET'])
def lite_render():
    image_url = request.args.get('url')
    if not image_url:
        return "URL ausente", 400

    # Headers completos simulando um navegador real no Windows/Chrome
    # Isso engana o Varnish Cache do Reddit
    headers = {
        'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36',
        'Accept': 'image/avif,image/webp,image/apng,image/svg+xml,image/*,*/*;q=0.8',
        'Accept-Language': 'en-US,en;q=0.9',
        'Referer': 'https://www.reddit.com/',
        'Sec-Fetch-Dest': 'image',
        'Sec-Fetch-Mode': 'no-cors',
        'Sec-Fetch-Site': 'cross-site'
    }

    try:
        # Requisita a imagem passando os headers de navegador
        response = requests.get(image_url, headers=headers, timeout=10)

        if response.status_code != 200:
            return f"Erro ao buscar do Reddit: {response.status_code}", response.status_code

        # Converte a imagem (WebP/PNG/GIF) para JPEG padrao
        # Isso garante que versões antigas do Android consigam renderizar
        image = Image.open(io.BytesIO(response.content))
        if image.mode != 'RGB':
            image = image.convert('RGB')

        output = io.BytesIO()
        image.save(output, format='JPEG', quality=85)
        output.seek(0)

        return Response(output.getvalue(), mimetype='image/jpeg')

    except Exception as e:
        return f"Erro interno do servidor: {str(e)}", 500
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