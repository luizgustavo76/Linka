import os
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import jsonify, request, g, Blueprint

bookmarks_bp = Blueprint("bookmarks_bp", __name__)

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS bookmarks(
            id SERIAL PRIMARY KEY,
            username TEXT NOT NULL,
            post_id TEXT NOT NULL,
            is_public INTEGER DEFAULT 1,
            is_external INTEGER DEFAULT 0,
            source_url TEXT,
            original_id TEXT,
            platform TEXT,
            UNIQUE(username, post_id)
        )
    """)
    conn.commit()
    cur.close()
    conn.close()

@bookmarks_bp.route("/view-bookmarks", methods=["POST"])
def view_bookmarks():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    receiver = data.get("receiver") or username
    current_user = getattr(g, "username", None)

    if username and username == current_user:
        conn = get_db()
        cur = conn.cursor()
        
        # O cast 'p.id::text' resolve a conversão entre TEXT e INTEGER no PostgreSQL
        if receiver == username:
            query = """
                SELECT 
                    b.id AS bookmark_id,
                    b.username AS bookmark_user,
                    b.post_id,
                    b.is_public,
                    b.is_external,
                    b.platform,
                    p.id,
                    p.username,
                    p.text_post,
                    p.datetime,
                    p.media,
                    p.media_type,
                    p.width,
                    p.heigth
                FROM bookmarks b
                LEFT JOIN posts p ON b.post_id = p.id::text
                WHERE b.username = %s
                ORDER BY b.id DESC
            """
            cur.execute(query, (username,))
        else:
            query = """
                SELECT 
                    b.id AS bookmark_id,
                    b.username AS bookmark_user,
                    b.post_id,
                    b.is_public,
                    b.is_external,
                    b.platform,
                    p.id,
                    p.username,
                    p.text_post,
                    p.datetime,
                    p.media,
                    p.media_type,
                    p.width,
                    p.heigth
                FROM bookmarks b
                LEFT JOIN posts p ON b.post_id = p.id::text
                WHERE b.username = %s AND b.is_public = 1
                ORDER BY b.id DESC
            """
            cur.execute(query, (receiver,))
            
        rows = cur.fetchall()
        
        result = []
        for row in rows:
            post_dict = dict(row)
            if post_dict.get("media"):
                post_dict["media_list"] = [{
                    "media_url": post_dict.get("media"),
                    "media_type": post_dict.get("media_type", "image"),
                    "width": post_dict.get("width", ""),
                    "heigth": post_dict.get("heigth", "")
                }]
            result.append(post_dict)

        cur.close()
        conn.close()
        return jsonify(result), 200
    
    return jsonify({"status": "forbidden"}), 403

@bookmarks_bp.route("/toggle-bookmark", methods=["POST"])
def toggle_bookmark():
    data = request.get_json(silent=True) or {}
    username = data.get("username")

    if username == getattr(g, "username", None):
        post_id = data.get("post_id")
        is_public = data.get("is_public")
        original_id = data.get("original_id")
        is_external = data.get("is_external")
        source_url = data.get("source_url")
        platform = data.get("platform")

        if None in (is_public, is_external, platform) or not post_id:
            return jsonify({"status": "missing data"}), 400

        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute(
                "SELECT id FROM bookmarks WHERE username = %s AND (post_id = %s OR original_id = %s)", 
                (username, post_id, original_id)
            )
            result = cur.fetchone()

            if result:
                cur.execute(
                    "DELETE FROM bookmarks WHERE username = %s AND (post_id = %s OR original_id = %s)", 
                    (username, post_id, original_id)
                )
                conn.commit()
                return jsonify({"status": "bookmark removed"}), 200
            else:
                cur.execute("""
                    INSERT INTO bookmarks (username, post_id, is_public, is_external, source_url, platform, original_id)
                    VALUES (%s, %s, %s, %s, %s, %s, %s)
                """, (username, post_id, is_public, is_external, source_url, platform, original_id))
                conn.commit()
                return jsonify({"status": "bookmark inserted"}), 200
        except Exception as e:
            return jsonify({"status": "error", "message": str(e)}), 500
        finally:
            cur.close()
            conn.close()
            
    return jsonify({"status": "forbidden"}), 403