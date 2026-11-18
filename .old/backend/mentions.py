import os
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, request, jsonify, g

mentions_bp = Blueprint("mentions_bp", __name__)

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
            CREATE TABLE IF NOT EXISTS mentions (
                id SERIAL PRIMARY KEY,
                username TEXT NOT NULL,
                author TEXT NOT NULL,
                content TEXT,
                type TEXT,
                post_id INTEGER,
                comment_id INTEGER,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        conn.commit()
    except Exception as e:
        conn.rollback()
        print(f"[ERRO create_db mentions_bp]: {e}")
    finally:
        cur.close()
        conn.close()

@mentions_bp.route("/view-mentions", methods=["POST"])
def view_mentions():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    
    if username == getattr(g, "username", None):
        mention_type = data.get("type")
        conn = get_db()
        cur = conn.cursor()
        try:
            if mention_type:
                cur.execute(
                    "SELECT id, username, author, content, type, post_id, comment_id, created_at FROM mentions WHERE username = %s AND type = %s ORDER BY created_at DESC", 
                    (username, mention_type)
                )
            else:
                cur.execute(
                    "SELECT id, username, author, content, type, post_id, comment_id, created_at FROM mentions WHERE username = %s ORDER BY created_at DESC", 
                    (username,)
                )
            
            rows = cur.fetchall()
            result = []
            for row in rows:
                result.append({
                    "id": row["id"],
                    "username": row["username"],
                    "author": row["author"],
                    "content": row["content"],
                    "type": row["type"],
                    "post_id": row["post_id"],
                    "comment_id": row["comment_id"],
                    "created_at": str(row["created_at"]) if row["created_at"] else None
                })
                
            return jsonify(result), 200
        except Exception as e:
            return jsonify({"status": "error", "message": str(e)}), 500
        finally:
            cur.close()
            conn.close()
            
    return jsonify({"status": "forbidden"}), 403