import os
import json
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, request, jsonify, g

community_bp = Blueprint("community_bp", __name__)

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def create_db():
    conn = get_db()
    cur = conn.cursor()
    
    cur.execute("""
        CREATE TABLE IF NOT EXISTS community_index (
            id SERIAL PRIMARY KEY,
            name TEXT NOT NULL UNIQUE,
            description TEXT,
            banner_image TEXT,
            icon_image TEXT,
            owner TEXT NOT NULL,
            members INTEGER DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS members (
            username TEXT,
            community_id INTEGER,
            datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS posts (
            id SERIAL PRIMARY KEY,
            community_id INTEGER NOT NULL,
            username TEXT NOT NULL,
            media_attachments TEXT,
            media_type TEXT,
            text_post TEXT,
            datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS comments (
            id SERIAL PRIMARY KEY,
            post_id INTEGER NOT NULL,
            username TEXT NOT NULL,
            media_attachments TEXT,
            media_type TEXT,
            text_comment TEXT,
            datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            comments_level INTEGER DEFAULT 0,
            parent_comment INTEGER
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS chat (
            id SERIAL PRIMARY KEY,
            username TEXT,
            text_message TEXT,
            media_attachments TEXT,
            media_type TEXT,
            datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
            community_id INTEGER
        )
    """)
    
    conn.commit()
    cur.close()
    conn.close()

@community_bp.route("/new-message-community", methods=["POST"])
def new_message():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    
    if username == getattr(g, "username", None):
        text_message = data.get("text_message")
        community_id = data.get("community_id")
        media_attachment = data.get("media_attachment")
        media_type = data.get("media_type")
        
        if not community_id:
            return jsonify({"status": "missing group_id"}), 400

        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute(
                "SELECT 1 FROM members WHERE username = %s AND community_id = %s",
                (username, community_id)
            )
            if not cur.fetchone():
                return jsonify({"status": "not a community member"}), 403

            cur.execute("""
                INSERT INTO chat 
                (username, text_message, media_attachments, media_type, community_id) 
                VALUES (%s, %s, %s, %s, %s)
            """, (username, text_message, media_attachment, media_type, community_id))
            conn.commit()
            return jsonify({"status": "message was sent"}), 200
        except Exception as e:
            return jsonify({"status": "error", "message": str(e)}), 500
        finally:
            cur.close()
            conn.close()

    return jsonify({"status": "forbidden"}), 403

@community_bp.route("/view-chat-community", methods=["POST"])
def view_chat():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    
    if username == getattr(g, "username", None):
        community_id = data.get("community_id")
        if not community_id:
            return jsonify({"status": "missing community_id"}), 400

        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute("""
                SELECT 
                    c.id,
                    c.username,
                    c.text_message,
                    c.media_attachments,
                    c.media_type
                FROM chat c
                INNER JOIN members m 
                    ON c.username = m.username 
                   AND c.community_id = m.community_id
                WHERE c.community_id = %s
            """, (community_id,))
            result = cur.fetchall()
            
            json_return = []
            for row in result:
                json_return.append({
                    "id": row["id"],
                    "username": row["username"],
                    "text_message": row["text_message"],
                    "media_attachment": row["media_attachments"],
                    "media_type": row["media_type"]
                })
            return jsonify(json_return), 200
        finally:
            cur.close()
            conn.close()

    return jsonify({"status": "forbidden"}), 403

@community_bp.route("/new-community", methods=["POST"])
def new_community():
    data = request.get_json(silent=True) or {}
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
        try:
            cur.execute(
                "INSERT INTO community_index(name, description, banner_image, icon_image, owner) VALUES(%s, %s, %s, %s, %s)", 
                (community_name, description, banner_image, icon_image, username)
            )
            conn.commit()
            return jsonify({"status": "community created"}), 200
        except Exception as e:
            return jsonify({"status": "error", "message": str(e)}), 500
        finally:
            cur.close()
            conn.close()

    return jsonify({"status": "forbidden"}), 403

@community_bp.route("/view-communities", methods=["GET"])
def view_communities():
    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("SELECT id, name, description, banner_image, icon_image, owner, members, created_at FROM community_index")
        result = cur.fetchall()
        
        json_return = []
        for comm in result:
            json_return.append({
                "id": comm["id"],
                "name": comm["name"],
                "description": comm["description"],
                "banner_image": comm["banner_image"],
                "icon_image": comm["icon_image"],
                "owner": comm["owner"],
                "members": comm["members"],
                "created_at": str(comm["created_at"]) if comm["created_at"] else None
            })
        return jsonify(json_return), 200
    finally:
        cur.close()
        conn.close()

@community_bp.route("/new-post", methods=["POST"])
def new_post():
    data = request.get_json(silent=True) or {}
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
        try:
            cur.execute(
                "INSERT INTO posts(community_id, username, media_attachments, text_post) VALUES(%s, %s, %s, %s)", 
                (community_id, username, media_attachments_json, text_post)
            )
            conn.commit()
            return jsonify({"status": "post created"}), 200
        except Exception as e:
            return jsonify({"status": "error", "message": str(e)}), 500
        finally:
            cur.close()
            conn.close()

    return jsonify({"status": "forbidden"}), 403

@community_bp.route("/view-posts", methods=["POST"])
def view_posts():
    data = request.get_json(silent=True) or {}
    community_id = data.get("community_id")
    if not community_id:
        return jsonify({"status": "missing data"}), 400
        
    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("SELECT id, community_id, username, media_attachments, media_type, text_post, datetime FROM posts WHERE community_id = %s ORDER BY datetime DESC", (community_id,))
        result = cur.fetchall()
        
        json_return = []
        for post in result:
            media_att = []
            if post["media_attachments"]:
                try:
                    media_att = json.loads(post["media_attachments"])
                except Exception:
                    media_att = []
                    
            media_tp = []
            if post["media_type"]:
                try:
                    media_tp = json.loads(post["media_type"])
                except Exception:
                    media_tp = []

            json_return.append({
                "id": post["id"],
                "community_id": post["community_id"],
                "username": post["username"],
                "media_attachments": media_att,
                "media_type": media_tp,
                "text_post": post["text_post"],
                "datetime": str(post["datetime"]) if post["datetime"] else None
            })
        return jsonify(json_return), 200
    finally:
        cur.close()
        conn.close()

@community_bp.route("/new-comment", methods=["POST"])
def new_comment():
    data = request.get_json(silent=True) or {}
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
        try:
            cur.execute(
                "INSERT INTO comments(post_id, username, media_attachments, text_comment, comments_level, parent_comment) VALUES(%s, %s, %s, %s, %s, %s)", 
                (post_id, username, media_attachments_json, text_comment, comments_level, parent_comment)
            )
            conn.commit()
            return jsonify({"status": "comment created"}), 200
        except Exception as e:
            return jsonify({"status": "error", "message": str(e)}), 500
        finally:
            cur.close()
            conn.close()

    return jsonify({"status": "forbidden"}), 403