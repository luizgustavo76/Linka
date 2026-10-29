from flask import Flask, jsonify, request, Blueprint, g
import os
import sqlite3
import json

community_bp = Blueprint("community_bp", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")

if not os.path.exists(db_dir):
    os.makedirs(db_dir, exist_ok=True)

def get_db():
    conn = sqlite3.connect(os.path.join(db_dir, "community.db"))
    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA synchronous=NORMAL;")
    conn.execute("PRAGMA cache_size=-10000;")
    return conn

def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""CREATE TABLE IF NOT EXISTS community_index (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE,
            description TEXT,
            banner_image TEXT,
            icon_image TEXT,
            owner TEXT NOT NULL,
            members INTEGER DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )""")
    cur.execute("""CREATE TABLE IF NOT EXISTS posts(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            community_id INTEGER NOT NULL,
            username TEXT NOT NULL,
            media_attachments TEXT,
            text_post TEXT,
            datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )""")
    cur.execute("""CREATE TABLE IF NOT EXISTS comments(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            post_id INTEGER NOT NULL,
            username TEXT NOT NULL,
            media_attachments TEXT,
            text_comment TEXT,
            datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            comments_level INTEGER DEFAULT 0,
            parent_comment INTEGER)""")
    conn.commit()
    conn.close()

create_db()

@community_bp.route("/new-community", methods=["POST"])
def new_community():
    data = request.get_json() or {}
    username = data.get("username")
    if username == getattr(g, "username", None):
        community_name = data.get("community_name")
        description = data.get("description")
        banner_image = data.get("banner_image")
        icon_image = data.get("icon_image")
        if None in (community_name, description):
            return jsonify({"status": "missing data"}), 400
        conn = get_db()
        cur = conn.cursor()
        cur.execute("INSERT INTO community_index(name, description, banner_image, icon_image, owner) VALUES(?,?,?,?,?)", (community_name, description, banner_image, icon_image, username))
        conn.commit()
        conn.close()
        return jsonify({"status": "community created"}), 200
    else:
        return jsonify({"status": "forbidden"}), 403

@community_bp.route("/view-communities", methods=["POST"])
def view_communities():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT * FROM community_index")
    result = cur.fetchall()
    conn.close()
    json_return = []
    for comm in result:
        json_return.append({
            "id": comm[0],
            "name": comm[1],
            "description": comm[2],
            "banner_image": comm[3],
            "icon_image": comm[4],
            "owner": comm[5],
            "members": comm[6],
            "created_at": comm[7]
        })
    return jsonify(json_return), 200

@community_bp.route("/new-post", methods=["POST"])
def new_post():
    data = request.get_json() or {}
    username = data.get("username")
    if username == getattr(g, "username", None):
        text_post = data.get("text_post")
        community_id = data.get("community_id")
        media_attachments = data.get("media_attachments", [])
        if not community_id or (not text_post and not media_attachments):
            return jsonify({"status": "missing data"}), 400
        media_attachments_json = json.dumps(media_attachments)
        conn = get_db()
        cur = conn.cursor()
        cur.execute("INSERT INTO posts(community_id, username, media_attachments, text_post) VALUES(?,?,?,?)", (community_id, username, media_attachments_json, text_post))
        conn.commit()
        conn.close()
        return jsonify({"status": "post created"}), 200
    else:
        return jsonify({"status": "forbidden"}), 403

@community_bp.route("/view-posts", methods=["POST"])
def view_posts():
    data = request.get_json() or {}
    community_id = data.get("community_id")
    if not community_id:
        return jsonify({"status": "missing data"}), 400
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT * FROM posts WHERE community_id = ? ORDER BY datetime DESC", (community_id,))
    result = cur.fetchall()
    conn.close()
    json_return = []
    for post in result:
        json_return.append({
            "id": post[0],
            "community_id": post[1],
            "username": post[2],
            "media_attachments": json.loads(post[3]) if post[3] else [],
            "text_post": post[4],
            "datetime": post[5]
        })
    return jsonify(json_return), 200

@community_bp.route("/new-comment", methods=["POST"])
def new_comment():
    data = request.get_json() or {}
    username = data.get("username")
    if username == getattr(g, "username", None):
        post_id = data.get("post_id")
        text_comment = data.get("text_comment")
        parent_comment = data.get("parent_comment")
        comments_level = data.get("comments_level", 0)
        media_attachments = data.get("media_attachments", [])
        if not post_id or (not text_comment and not media_attachments):
            return jsonify({"status": "missing data"}), 400
        media_attachments_json = json.dumps(media_attachments)
        conn = get_db()
        cur = conn.cursor()
        cur.execute("INSERT INTO comments(post_id, username, media_attachments, text_comment, comments_level, parent_comment) VALUES(?,?,?,?,?,?)", (post_id, username, media_attachments_json, text_comment, comments_level, parent_comment))
        conn.commit()
        conn.close()
        return jsonify({"status": "comment created"}), 200
    else:
        return jsonify({"status": "forbidden"}), 403