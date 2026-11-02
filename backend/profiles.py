import os
import uuid
import boto3
import dotenv
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, g, jsonify, request

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
env_path = os.path.join(BASE_DIR, "backend.env")
if os.path.exists(env_path):
    dotenv.load_dotenv(env_path)

# ==========================================
# CONFIGURAÇÃO DO BACKBLAZE B2 VIA BOTO3
# ==========================================
B2_KEY_ID = os.getenv("B2_KEY_ID")
B2_APPLICATION_KEY = os.getenv("B2_APPLICATION_KEY")
B2_ENDPOINT = os.getenv("B2_ENDPOINT", "https://s3.us-east-005.backblazeb2.com")
BUCKET_NAME = os.getenv("B2_BUCKET", "linka-media")
MY_DOMINIO_PROXY = os.getenv("MY_DOMINIO_PROXY", "https://seu-app-linka.onrender.com")

s3_client = boto3.client(
    "s3",
    endpoint_url=B2_ENDPOINT,
    aws_access_key_id=B2_KEY_ID,
    aws_secret_access_key=B2_APPLICATION_KEY,
)


def get_optimized_url(filename):
    """Gera a URL final do wsrv.nl apontando para o proxy público do Flask"""
    proxy_url = f"{MY_DOMINIO_PROXY.rstrip('/')}/midia/{filename}"
    return f"https://wsrv.nl/?url={proxy_url}"


# ==========================================
# BANCO DE DADOS (POSTGRESQL / NEON.TECH)
# ==========================================
DATABASE_URL = os.getenv("DATABASE_URL")

profile_bp = Blueprint("profile", __name__)


def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)


def create_table():
    if not DATABASE_URL:
        return
    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("""
            CREATE TABLE IF NOT EXISTS linkos_table (
                id SERIAL PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                linkos INTEGER DEFAULT 0
            )
        """)
        cur.execute("""
            CREATE TABLE IF NOT EXISTS badges (
                id SERIAL PRIMARY KEY,
                username TEXT NOT NULL,
                badge TEXT NOT NULL
            )
        """)
        cur.execute("""
            CREATE TABLE IF NOT EXISTS profile (
                username TEXT PRIMARY KEY,
                bio TEXT,
                actual_badge TEXT,
                "ProfilePicture" TEXT,
                followers INTEGER DEFAULT 0,
                following INTEGER DEFAULT 0,
                hex_banner TEXT,
                profile_banner_color TEXT DEFAULT '#FF5722',
                dm_header_color TEXT
            )
        """)
        conn.commit()
    except Exception as e:
        conn.rollback()
        print(f"[ERRO create_table profile]: {e}")
    finally:
        cur.close()
        conn.close()


# ==========================================
# ROTAS DO PERFIL
# ==========================================

@profile_bp.route("/edit", methods=["POST"])
def edit():
    data = request.get_json(silent=True) or {}
    edit_mode = data.get("edit-mode")
    content = data.get("content")
    username = data.get("username")

    if username == getattr(g, "username", None):
        conn = get_db()
        cur = conn.cursor()
        try:
            if edit_mode == "bio":
                cur.execute("UPDATE profile SET bio = %s WHERE username = %s", (content, username))
            elif edit_mode == "dm_header_color":
                cur.execute("UPDATE profile SET dm_header_color = %s WHERE username = %s", (content, username))
            elif edit_mode == "profile_banner_color":
                cur.execute("UPDATE profile SET profile_banner_color = %s WHERE username = %s", (content, username))
            else:
                return jsonify({"status": "method not allowed"}), 401

            conn.commit()
            return jsonify({"status": "edit is sucessful"}), 200
        except Exception as e:
            conn.rollback()
            return jsonify({"status": "error", "message": str(e)}), 500
        finally:
            cur.close()
            conn.close()

    return jsonify({"status": "forbidden"}), 403


@profile_bp.route("/create_profile", methods=["POST"])
def create():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    
    if not username:
        return jsonify({"status": "username is missing"}), 400

    bio_default = f"Hi! my name is {username} and im new in the Linka, let be friends?"
    
    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute(
            """
            INSERT INTO profile (username, bio) 
            VALUES (%s, %s)
            ON CONFLICT (username) DO NOTHING
            """,
            (username, bio_default)
        )
        conn.commit()
        return jsonify({"status": "Ok"}), 200
    except Exception as e:
        conn.rollback()
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        cur.close()
        conn.close()


@profile_bp.route("/view_profile/<username>")
def view(username):
    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute(
            "SELECT username, bio, dm_header_color, profile_banner_color FROM profile WHERE username = %s",
            (username,)
        )
        response = cur.fetchone()

        is_friend = False
        user_g = getattr(g, "username", None)

        if user_g:
            cur.execute("""
                SELECT 1 FROM friends 
                WHERE (remittee = %s AND receiver = %s) 
                   OR (remittee = %s AND receiver = %s)
            """, (username, user_g, user_g, username))
            
            if cur.fetchone():
                is_friend = True

        if response:
            row = {
                "username": response["username"],
                "bio": response["bio"],
                "profile_banner_color": response["profile_banner_color"] if response["profile_banner_color"] else "#99D6FF",
                "is_friend": is_friend
            }
            return jsonify(row), 200

        return jsonify({"status": "profile not found"}), 404
    finally:
        cur.close()
        conn.close()


@profile_bp.route("/update-hex-banner", methods=["POST"])
def update_hex_banner():
    data = request.get_json(silent=True) or {}
    username = data.get("username")

    if username == getattr(g, "username", None):
        hex_color = data.get("hex_color")
        if not hex_color:
            return jsonify({"status": "hex color not be null"}), 400

        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute("UPDATE profile SET profile_banner_color = %s WHERE username = %s", (hex_color, username))
            conn.commit()
            return jsonify({"status": "banner color updated"}), 200
        except Exception as e:
            conn.rollback()
            return jsonify({"status": "error", "message": str(e)}), 500
        finally:
            cur.close()
            conn.close()

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
        try:
            cur.execute("UPDATE profile SET \"ProfilePicture\" = %s WHERE username = %s", (public_url, username))
            conn.commit()
        except Exception as e:
            conn.rollback()
            raise e
        finally:
            cur.close()
            conn.close()

        return jsonify({
            "status": "success",
            "image_url": public_url
        }), 200

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500


@profile_bp.route("/view-profile-picture", methods=["POST"])
def get_profile_pic():
    data = request.get_json(silent=True) or {}
    username = data.get("username")

    if not username:
        return jsonify({"status": "profile picture has not founded"}), 400

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("SELECT \"ProfilePicture\" FROM profile WHERE username = %s", (username,))
        result = cur.fetchone()

        if result and result["ProfilePicture"]:
            return jsonify({"profile-picture": result["ProfilePicture"]}), 200

        return jsonify({"status": "profile picture has not founded"}), 400
    finally:
        cur.close()
        conn.close()