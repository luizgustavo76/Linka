import os
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, request, jsonify

search_bp = Blueprint("search", __name__)

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

@search_bp.route("/search", methods=["POST"])
def search():
    data = request.get_json(silent=True) or {}
    content = data.get("content")

    if not content:
        return jsonify({"error": "content vazio"}), 400

    content = str(content).strip()
    if not content:
        return jsonify({"error": "content vazio"}), 400

    result = {
        "usernames": [],
        "posts": []
    }

    conn = get_db()
    cur = conn.cursor()
    try:
        search_pattern = f"%{content}%"

        # 1. Busca por nomes de usuários no perfil
        cur.execute(
            """
            SELECT username
            FROM profile
            WHERE username ILIKE %s
            LIMIT 20
            """,
            (search_pattern,)
        )
        usernames = cur.fetchall()
        for u in usernames:
            result["usernames"].append(u["username"])

        # 2. Busca por conteúdo das postagens
        cur.execute(
            """
            SELECT text_post
            FROM posts
            WHERE text_post ILIKE %s
            LIMIT 20
            """,
            (search_pattern,)
        )
        posts = cur.fetchall()
        for p in posts:
            if p["text_post"]:
                result["posts"].append(p["text_post"])

        return jsonify(result), 200

    except Exception as e:
        return jsonify({"error": "server error", "details": str(e)}), 500
    finally:
        cur.close()
        conn.close()