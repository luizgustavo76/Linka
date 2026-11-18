import os
import psycopg2
from psycopg2.extras import RealDictCursor
from psycopg2.errors import UniqueViolation
from flask import Blueprint, request, jsonify, g
from dotenv import load_dotenv

load_dotenv("backend.env")
ADMIN_MASTER_KEY = os.getenv("ADMIN_MASTER_KEY")

federation_index_bp = Blueprint("federation_index_bp", __name__)

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def create_table():
    conn = get_db()
    cur = conn.cursor()
    
    cur.execute("""
        CREATE TABLE IF NOT EXISTS federation_index (
            id SERIAL PRIMARY KEY,
            name TEXT,
            url TEXT,
            description TEXT,
            cover_image TEXT,
            theme TEXT
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS themes_timeline (
            id SERIAL PRIMARY KEY,
            name_theme TEXT UNIQUE,
            description TEXT
        )
    """)
    conn.commit()
    cur.close()
    conn.close()

@federation_index_bp.route("/add-theme", methods=["POST"])
def add_theme():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    name = data.get("name")
    description = data.get("description")
    
    if username == getattr(g, "username", None):
        admin_master_key = data.get("admin_master_key")
        if admin_master_key == ADMIN_MASTER_KEY:
            if not name:
                return jsonify({"status": "theme name is missing"}), 400

            conn = get_db()
            cur = conn.cursor()
            try:
                cur.execute(
                    "INSERT INTO themes_timeline (name_theme, description) VALUES (%s, %s)",
                    (name, description)
                )
                conn.commit()
                return jsonify({"status": "theme created"}), 200
            except UniqueViolation:
                conn.rollback()
                return jsonify({"status": "theme already exists"}), 409
            except Exception as e:
                conn.rollback()
                return jsonify({"status": "error", "message": str(e)}), 500
            finally:
                cur.close()
                conn.close()
        else:
            return jsonify({"status": "forget admin key.... or you are a invasor.. nice to meet you"}), 400
    
    return jsonify({"status": "forbidden"}), 403

@federation_index_bp.route("/view-index/<path:theme>", methods=["GET"])
def view_index_by_theme(theme):
    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute(
            "SELECT name, url, description, cover_image, theme FROM federation_index WHERE theme = %s", 
            (theme,)
        )
        result = cur.fetchall()
        
        formatted_index = [{
            "name": row["name"],
            "url": row["url"],
            "description": row["description"],
            "cover_image": row["cover_image"],
            "theme": row["theme"]
        } for row in result]
        
        return jsonify(formatted_index), 200
    finally:
        cur.close()
        conn.close()

@federation_index_bp.route("/view-index", methods=["GET"])
def view_index():
    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("SELECT name, url, description, cover_image, theme FROM federation_index")
        result = cur.fetchall()
        
        formatted_index = [{
            "name": row["name"],
            "url": row["url"],
            "description": row["description"],
            "cover_image": row["cover_image"],
            "theme": row["theme"]
        } for row in result]
        
        return jsonify(formatted_index), 200
    finally:
        cur.close()
        conn.close()

@federation_index_bp.route("/view-index-themes", methods=["GET"])
def view_index_themes():
    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("SELECT name_theme, description FROM themes_timeline")
        result = cur.fetchall()
        
        formatted_index = [{
            "name_theme": row["name_theme"],
            "description": row["description"]
        } for row in result]
        
        return jsonify(formatted_index), 200
    finally:
        cur.close()
        conn.close()

@federation_index_bp.route("/register-federation", methods=["POST"])
def register_federation():
    data = request.get_json(silent=True) or {}
    name = data.get("name")
    url = data.get("url")
    description = data.get("description")
    cover_image = data.get("cover_image")
    theme = data.get("theme")
    
    if not name or not url:
        return jsonify({"status": "missing essential data"}), 400

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute(
            "INSERT INTO federation_index (name, url, description, cover_image, theme) VALUES (%s, %s, %s, %s, %s)",
            (name, url, description, cover_image, theme)
        )
        conn.commit()
        return jsonify({"status": "the federations has registred with sucess"}), 200
    except Exception as e:
        conn.rollback()
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        cur.close()
        conn.close()