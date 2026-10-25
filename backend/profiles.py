import os
import sqlite3
import uuid
import boto3
import dotenv
from flask import Blueprint, g, jsonify, request

dotenv.load_dotenv("backend.env")

# ==========================================
# CONFIGURAÇÃO DO BACKBLAZE B2 VIA BOTO3
# ==========================================
B2_KEY_ID = os.getenv("B2_KEY_ID")
B2_APPLICATION_KEY = os.getenv("B2_APPLICATION_KEY")
B2_ENDPOINT = os.getenv("B2_ENDPOINT", "https://s3.us-east-005.backblazeb2.com")
BUCKET_NAME = os.getenv("B2_BUCKET", "linka-media")
MY_DOMINIO_PROXY = os.getenv("MY_DOMINIO_PROXY", "https://linkaProject.pythonanywhere.com")

s3_client = boto3.client(
    "s3",
    endpoint_url=B2_ENDPOINT,
    aws_access_key_id=B2_KEY_ID,
    aws_secret_access_key=B2_APPLICATION_KEY,
)


def get_optimized_url(filename):
    """Gera a URL final do wsrv.nl apontando para o proxy público do Flask"""
    proxy_url = f"{MY_DOMINIO_PROXY}/midia/{filename}"
    return f"https://wsrv.nl/?url={proxy_url}"


# ==========================================
# BANCO DE DADOS LOCAL (SQLITE)
# ==========================================
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
profile_dir = os.path.join(db_dir, "profile.db")

profile_bp = Blueprint("profile", __name__)


def get_db():
    conn = sqlite3.connect(profile_dir)
    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA synchronous=NORMAL;")
    conn.execute("PRAGMA cache_size=-10000;")
    return conn


def get_db_friends():
    conn = sqlite3.connect(os.path.join(db_dir, "friends.db"))
    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA synchronous=NORMAL;")
    conn.execute("PRAGMA cache_size=-10000;")
    return conn


def create_table():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS linkos_table(
            username TEXT,
            linkos INTEGER
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS badges(
            username TEXT,
            badge TEXT
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS profile (
            username TEXT PRIMARY KEY,
            bio TEXT,
            actual_badge TEXT,
            ProfilePicture TEXT,
            followers INTEGER,
            following INTEGER,
            hex_banner TEXT,
            profile_banner_color TEXT DEFAULT '#FF5722'
        )
    """)

    try:
        cur.execute("ALTER TABLE profile ADD COLUMN profile_banner_color TEXT DEFAULT '#FF5722';")
    except sqlite3.OperationalError:
        pass
    conn.commit()
    conn.close()


create_table()


# ==========================================
# ROTAS DO PERFIL
# ==========================================

@profile_bp.route("/edit", methods=["POST"])
def edit():
    data = request.get_json() or {}
    edit_mode = data.get("edit-mode")
    content = data.get("content")
    username = data.get("username")

    if username == getattr(g, "username", None):
        conn = get_db()
        cur = conn.cursor()

        if edit_mode == "bio":
            cur.execute("UPDATE profile SET bio = ? WHERE username = ?", (content, username))
        elif edit_mode == "dm_header_color":
            cur.execute("UPDATE profile SET dm_header_color = ? WHERE username = ?", (content, username))
        elif edit_mode == "profile_banner_color":
            cur.execute("UPDATE profile SET profile_banner_color = ? WHERE username = ?", (content, username))
        else:
            conn.close()
            return jsonify({"status": "method not allowed"}), 401

        conn.commit()
        conn.close()
        return jsonify({"status": "edit is sucessful"}), 200

    return jsonify({"status": "forbidden"}), 403


@profile_bp.route("/create_profile", methods=["POST"])
def create():
    data = request.get_json() or {}
    username = data.get("username")
    bio_default = f"Hi! my name is {username} and im new in the Linka, let be friends?"
    
    conn = get_db()
    cur = conn.cursor()
    cur.execute("INSERT INTO profile (username, bio) VALUES (?,?)", (username, bio_default))
    conn.commit()
    conn.close()
    return jsonify({"status": "Ok"})


@profile_bp.route("/view_profile/<username>")
def view(username):
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT username, bio, dm_header_color, profile_banner_color FROM profile WHERE username = ?", (username,))
    response = cur.fetchone()

    conn_friends = get_db_friends()
    cur_friends = conn_friends.cursor()
    is_friend = False
    
    user_g = getattr(g, "username", None)
    cur_friends.execute("""
        SELECT 1 FROM friends 
        WHERE (remittee = ? AND receiver = ?) 
        OR (remittee = ? AND receiver = ?)
    """, (username, user_g, user_g, username))
    
    if cur_friends.fetchone():
        is_friend = True

    conn.close()
    conn_friends.close()

    if response:
        row = {
            "username": response[0],
            "bio": response[1],
            "profile_banner_color": response[3] if len(response) > 3 and response[3] else "#99D6FF",
            "is_friend": is_friend
        }
        return jsonify(row), 200

    return jsonify({"status": "profile not found"}), 404


@profile_bp.route("/update-hex-banner", methods=["POST"])
def update_hex_banner():
    data = request.get_json() or {}
    username = data.get("username")

    if username == getattr(g, "username", None):
        hex_color = data.get("hex_color")
        if not hex_color:
            return jsonify({"status": "hex color not be null"}), 400

        conn = get_db()
        cur = conn.cursor()
        cur.execute("UPDATE profile SET profile_banner_color = ? WHERE username = ?", (hex_color, username))
        conn.commit()
        conn.close()
        return jsonify({"status": "banner color updated"}), 200

    return jsonify({"status": "forbidden"}), 403


@profile_bp.route("/upload-profile", methods=["POST"])
def upload_profile_pic():
    username = getattr(g, "username", None)
    if not username:
        return jsonify({"status": "forbidden"}), 403

    if "image" not in request.files:
        return jsonify({"error": "No image uploaded"}), 400

    file = request.files["image"]
    if file.filename == "":
        return jsonify({"error": "File with no name"}), 400

    try:
        extension = os.path.splitext(file.filename)[1] or ".jpg"
        unique_file_name = f"profile_{uuid.uuid4().hex}{extension}"
        file_data = file.read()

        # 1. Upload direto para o Backblaze B2 via boto3
        s3_client.put_object(
            Bucket=BUCKET_NAME,
            Key=unique_file_name,
            Body=file_data,
            ContentType=file.content_type or "image/jpeg"
        )

        # 2. Gerar URL com duplo proxy (wsrv.nl -> Flask -> Backblaze B2)
        public_url = get_optimized_url(unique_file_name)

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
        return jsonify({"status": "error", "message": str(e)}), 500


@profile_bp.route("/view-profile-picture", methods=["POST"])
def get_profile_pic():
    data = request.get_json() or {}
    username = data.get("username")

    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT ProfilePicture FROM profile WHERE username = ?", (username,))
    result = cur.fetchone()
    conn.close()

    if result and result[0]:
        return jsonify({"profile-picture": result[0]}), 200

    return jsonify({"status": "profile picture has not founded"}), 400