import os
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, request, jsonify

slug_bp = Blueprint("slug", __name__)

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def create_db():
    if not DATABASE_URL:
        return
    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("""
            CREATE TABLE IF NOT EXISTS slug (
                id SERIAL PRIMARY KEY,
                name_server TEXT UNIQUE NOT NULL,
                url TEXT NOT NULL,
                owner TEXT,
                profile_linka TEXT,
                email TEXT
            )
        """)
        conn.commit()
    except Exception as e:
        conn.rollback()
        print(f"[ERRO create_db slug]: {e}")
    finally:
        cur.close()
        conn.close()

@slug_bp.route("/consult", methods=["POST"])
def consult():
    data = request.get_json(silent=True) or {}
    name_server = data.get("name_server")
    
    if not name_server:
        return jsonify({"status": "name_server is empty"}), 400

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("SELECT name_server, url, owner, profile_linka, email, id FROM slug WHERE name_server = %s", (name_server,))
        result = cur.fetchone()

        if not result:
            return jsonify({"status": "this instance not exists"}), 400

        row = {
            "name_server": result["name_server"],
            "url": result["url"],
            "owner": result["owner"],
            "profile_linka": result["profile_linka"],
            "email": result["email"],
            "id": result["id"]
        }
        return jsonify(row), 200
    finally:
        cur.close()
        conn.close()

@slug_bp.route("/register-instance", methods=["POST"])
def register_instance():
    data = request.get_json(silent=True) or {}
    name_server = data.get("name_server")
    url = data.get("url")
    owner = data.get("owner")
    profile_linka = data.get("profile_linka")
    email = data.get("email")

    if not name_server or not url:
        return jsonify({"status": "name_server or url is missing"}), 400

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute(
            """
            INSERT INTO slug (name_server, url, owner, profile_linka, email) 
            VALUES (%s, %s, %s, %s, %s)
            """,
            (name_server, url, owner, profile_linka, email)
        )
        conn.commit()
        return jsonify({"status": "instance register is sucessful"}), 200
    except Exception as e:
        conn.rollback()
        return jsonify({"status": "an error has occurred", "error": str(e)}), 400
    finally:
        cur.close()
        conn.close()