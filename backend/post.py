from flask import Blueprint, request, jsonify
import sqlite3
import os

base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
post_dir = os.path.join(db_dir, "post.db")

post_bp = Blueprint("post", __name__)

if not os.path.exists(db_dir):
    os.makedirs(db_dir)


def get_db():
    conn = sqlite3.connect(post_dir)
    return conn


def create_db():
    conn = get_db()
    cur = conn.cursor()

    cur.execute("""
        CREATE TABLE IF NOT EXISTS posts(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT,
            text_post TEXT,
            datetime TEXT
        )
    """)

    cur.execute("""
        CREATE TABLE IF NOT EXISTS stars(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            post_id INTEGER,
            username TEXT,
            UNIQUE(post_id, username)
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS comments(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                text_comment TEXT,
                post_id INTEGER,
                username TEXT
            )
    """)
    conn.commit()
    conn.close()


create_db()

@post_bp.route("/comments", methods=["POST"])
def new_comment():
    data = request.get_json()
    username = data.get("username")
    text_comment = data.get("text_comment")
    post_id = data.get("post_id")
    comment_id = data.get("comment_id")
    if (username, text_comment, post_id, comment_id) is None:
        return jsonify({"status":"as informações vieram vazias"}), 401
    else:
        conn = get_db()
        cur = conn.cursor()
        cur.execute("INSERT INTO comments (text_comment, username, post_id) VALUES(?,?,?)", (text_comment, username, post_id))
    return jsonify({"status":"o comentario foi criado com sucesso!"}), 200
@post_bp.route("/view-comments", methods=["POST"])
def view_comments():
    data = request.get_json()
    post_id = data.get("post_id")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT * FROM comments WHERE post_id = ?", (post_id,))      
    result = cur.fetchall()
    conn.close()
    return jsonify({"comments":result})
@post_bp.route("/new", methods=["POST"])
def new_post():
    data = request.get_json()

    username = data.get("username")
    text_post = data.get("text_post")
    datetime = data.get("datetime")

    if not username:
        return jsonify({"status": "username não informado"}), 400

    if not text_post:
        return jsonify({"status": "não é possível criar um post sem conteúdo"}), 400

    conn = get_db()
    cur = conn.cursor()

    cur.execute(
        "INSERT INTO posts(username, text_post, datetime) VALUES (?, ?, ?)",
        (username, text_post, datetime)
    )

    conn.commit()
    conn.close()

    return jsonify({"status": "post criado com sucesso"}), 200


@post_bp.route("/feed", methods=["GET"])
def feed():
    conn = get_db()
    cur = conn.cursor()

    cur.execute("SELECT id, username, text_post, datetime FROM posts ORDER BY id DESC")
    posts = cur.fetchall()

    conn.close()

    lista_posts = []
    for post in posts:
        lista_posts.append({
            "id": post[0],
            "username": post[1],
            "text_post": post[2],
            "datetime": post[3]
        })

    return jsonify(lista_posts), 200




@post_bp.route("/star", methods=["POST"])
def star():
    data = request.get_json()

    post_id = data.get("post_id")
    username = data.get("username")

    if not post_id or not username:
        return jsonify({"status": "post_id ou username faltando"}), 400

    conn = get_db()
    cur = conn.cursor()

    cur.execute(
        "SELECT id FROM stars WHERE post_id = ? AND username = ?",
        (post_id, username)
    )
    existing = cur.fetchone()

    if existing:
        cur.execute(
            "DELETE FROM stars WHERE post_id = ? AND username = ?",
            (post_id, username)
        )
        conn.commit()
        conn.close()
        return jsonify({"status": "removed"}), 200

    else:
        cur.execute(
            "INSERT INTO stars(post_id, username) VALUES (?, ?)",
            (post_id, username)
        )
        conn.commit()
        conn.close()
        return jsonify({"status": "added"}), 200


@post_bp.route("/return-stars/<int:post_id>", methods=["GET"])
def return_stars(post_id):
    conn = get_db()
    cur = conn.cursor()

    cur.execute("SELECT COUNT(*) FROM stars WHERE post_id = ?", (post_id,))
    qtd = cur.fetchone()[0]

    conn.close()

    return str(qtd), 200


@post_bp.route("/has-star", methods=["POST"])
def has_star():
    data = request.get_json()

    post_id = data.get("post_id")
    username = data.get("username")

    if not post_id or not username:
        return jsonify({"starred": False}), 200

    conn = get_db()
    cur = conn.cursor()

    cur.execute(
        "SELECT 1 FROM stars WHERE post_id = ? AND username = ?",
        (post_id, username)
    )
    exists = cur.fetchone()

    conn.close()

    return jsonify({"starred": exists is not None}), 200