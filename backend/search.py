from flask import Blueprint, request, jsonify
import os
import sqlite3

base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")

profile_db = os.path.join(db_dir, "profile.db")
post_db = os.path.join(db_dir, "post.db")

search_bp = Blueprint("search", __name__)

def get_db_profile():
    conn = sqlite3.connect(profile_db)
    cursor = conn.cursor()
<<<<<<< HEAD
    cursor.execute("PRAGMA journal_mode=WAL;")
    cursor.execute("PRAGMA synchronous=NORMAL;")
    cursor.execute("PRAGMA cache_size=-10000;")
=======
    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA synchronous=NORMAL;")
    conn.execute("PRAGMA cache_size=-10000;")
>>>>>>> 525a9a047e0c512bfcb84d412c38b80df3387426
    return conn
def get_db_post():
    conn = sqlite3.connect(post_db)
    cursor = conn.cursor()
<<<<<<< HEAD
    cursor.execute("PRAGMA journal_mode=WAL;")
    cursor.execute("PRAGMA synchronous=NORMAL;")
    cursor.execute("PRAGMA cache_size=-10000;")
=======
    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA synchronous=NORMAL;")
    conn.execute("PRAGMA cache_size=-10000;")
>>>>>>> 525a9a047e0c512bfcb84d412c38b80df3387426
    return conn

@search_bp.route("/search", methods=["POST"])
def search():
    data = request.get_json(silent=True)
    
    if not data or "content" not in data:
        return jsonify({"error": "content vazio"}), 400

    content = data["content"]

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

<<<<<<< HEAD
    cursor.execute("""
=======
    conn.execute("""
>>>>>>> 525a9a047e0c512bfcb84d412c38b80df3387426
        SELECT username
        FROM profile
        WHERE username LIKE ?
        LIMIT 20
    """, (f"%{content}%",))

<<<<<<< HEAD
    usernames = cursor.fetchall()
=======
    usernames = conn.fetchall()
>>>>>>> 525a9a047e0c512bfcb84d412c38b80df3387426

    for u in usernames:
        result["usernames"].append(u[0])

    conn.close()

    # ===== BUSCA POSTS =====
    conn = get_db_post()
    cursor = conn.cursor()

<<<<<<< HEAD
    cursor.execute("""
=======
    conn.execute("""
>>>>>>> 525a9a047e0c512bfcb84d412c38b80df3387426
        SELECT text_post
        FROM posts
        WHERE text_post LIKE ?
        LIMIT 20
    """, (f"%{content}%",))

<<<<<<< HEAD
    posts = cursor.fetchall()
=======
    posts = conn.fetchall()
>>>>>>> 525a9a047e0c512bfcb84d412c38b80df3387426

    for p in posts:
        result["posts"].append(p[0])

    conn.close()

    return jsonify(result), 200