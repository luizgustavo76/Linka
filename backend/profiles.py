from flask import Flask, request, jsonify, Blueprint, send_from_directory, g
import os
import uuid
import sqlite3
from supabase import create_client, Client
import dotenv

dotenv.load_dotenv("backend.env")
url = os.getenv("SUPABASE_URL")
key = os.getenv("SUPABASE_KEY")

supabase: Client = create_client(url, key)
#pasta atual
base_dir = os.path.dirname(os.path.abspath(__file__))
#pasta dos bancos de dados
db_dir = (base_dir + "/DB")
#arquivo de banco de dado de post
profile_dir = (db_dir + "/profile.db")
profile_bp = Blueprint("profile", __name__)
def get_db():
    conn = sqlite3.connect(profile_dir)
    cursor = conn.cursor()
    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA synchronous=NORMAL;")
    conn.execute("PRAGMA cache_size=-10000;")
    return conn
def create_table():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS profile (
            username TEXT PRIMARY KEY,
            bio TEXT,
            ProfilePicture TEXT,
            followers INTEGER,
            following INTEGER
        )
        """)
    conn.commit()
    conn.close()
create_table()
@profile_bp.route("/edit", methods=["POST"])
def edit():
    data = request.get_json()
    edit_mode = data.get("edit-mode")
    content = data.get("content")
    username = data.get("username")
    if username == g.username:
        if edit_mode == "bio":
            conn = get_db()
            cur = conn.cursor()
            cur.execute("UPDATE profile SET bio = ? WHERE username = ?", (content, username))
            conn.commit()
            conn.close()
            return jsonify({"status":"edit is sucessful"}),200
    else:
        return jsonify({"status":"forbidden"}),403
@profile_bp.route("/create_profile",methods=["post"])
def create():
    data = request.get_json()
    username = data.get("username")
    bio_default = f"Hi! my name is {username} and im new in the Linka, let be friends?"
    conn = get_db()
    cur = conn.cursor()
    cur.execute("INSERT INTO profile (username, bio) VALUES (?,?)", (username, bio_default))
    conn.commit()
    conn.close()
    return jsonify({"status":"Ok"})

        
@profile_bp.route("/view_profile/<username>")
def view(username):
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT * FROM profile WHERE username = ?",(username,))
    response = cur.fetchone()
    conn.close()
    row = {
        "username":response[0],
        "bio":response[1]
    }
    return jsonify(row), 200
UPLOAD_FOLDER = "../profile-pictures"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

@profile_bp.route("/upload-profile", methods=["POST"])
def upload_profile_pic():
    if g.username:
        username = g.username
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
            conn = get_db()
            cur = conn.cursor()
            cur.execute("UPDATE profile SET ProfilePicture = ? WHERE username = ?", (public_url, username))
            conn.commit()
            conn.close()
            return jsonify({
                "status": "success",
                "image_url": public_url
            }), 200

        except Exception as e:
            print(e)
            return jsonify({"status": "error", "message": str(e)}), 500


@profile_bp.route("/view-profile-picture",methods=["POST"])
def get_profile_pic():
    data = request.get_json()
    username = data.get("username")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT ProfilePicture FROM profile WHERE username = ?", (username,))
    result = cur.fetchone()
    print(result)
    return jsonify({"profile-picture":result[0]}),200
