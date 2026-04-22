from flask import Blueprint, request, jsonify
import os
import sqlite3

base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")

profile_db = os.path.join(db_dir, "profile.db")
post_db = os.path.join(db_dir, "post.db")

search_bp = Blueprint("search", __name__)

def get_db_profile():
    return sqlite3.connect(profile_db)

def get_db_post():
    return sqlite3.connect(post_db)

@search_bp.route("/search", methods=["GET"])
def search():
    content = request.args.get("content")

    if not content:
        return jsonify({"error": "content vazio"}), 400

    content = content.strip()

    result = {
        "usernames": [],
        "posts": []
    }

    # ===== BUSCA USERNAME =====
    conn = get_db_profile()
    cursor = conn.cursor()

    cursor.execute("""
        SELECT username
        FROM profiles
        WHERE username LIKE ?
        LIMIT 20
    """, (f"%{content}%",))

    usernames = cursor.fetchall()

    for u in usernames:
        result["usernames"].append(u[0])

    conn.close()


    # ===== BUSCA POSTS =====
    conn = get_db_post()
    cursor = conn.cursor()

    cursor.execute("""
        SELECT text
        FROM posts
        WHERE text LIKE ?
        LIMIT 20
    """, (f"%{content}%",))

    posts = cursor.fetchall()

    for p in posts:
        result["posts"].append(p[0])

    conn.close()

    return jsonify(result), 200