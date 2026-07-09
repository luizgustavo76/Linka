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