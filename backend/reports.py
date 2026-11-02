import os
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, request, jsonify, g

reports_bp = Blueprint("reports_bp", __name__)

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
            CREATE TABLE IF NOT EXISTS reports (
                id SERIAL PRIMARY KEY,
                username TEXT NOT NULL,
                sender TEXT NOT NULL,
                content TEXT NOT NULL,
                target_id TEXT,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        conn.commit()
    except Exception as e:
        conn.rollback()
        print(f"[ERRO create_db reports]: {e}")
    finally:
        cur.close()
        conn.close()

@reports_bp.route("/send-report", methods=["POST"])
def send_report():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    sender = data.get("sender")
    content = data.get("content")
    target_id = data.get("id")

    current_user = getattr(g, "username", None)

    if username == current_user or sender == current_user:
        if None in [content, target_id, sender]:
            return jsonify({"status": "incomplete report request"}), 400

        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute(
                """
                INSERT INTO reports (username, content, target_id, sender) 
                VALUES (%s, %s, %s, %s)
                """,
                (username, content, str(target_id), sender)
            )
            conn.commit()
            return jsonify({"status": "report sent with success!"}), 200
        except Exception as e:
            conn.rollback()
            return jsonify({"status": "error", "message": str(e)}), 500
        finally:
            cur.close()
            conn.close()
    else:
        return jsonify({"status": "forbidden"}), 403