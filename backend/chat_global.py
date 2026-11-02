import os
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, request, jsonify
import notificationsModule

chat_global_bp = Blueprint("chat_global", __name__)

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS chat_global (
            id SERIAL PRIMARY KEY,
            sender TEXT,
            message TEXT
        )
    """)
    conn.commit()
    cur.close()
    conn.close()

@chat_global_bp.route("/send-global-message", methods=["POST"])
def send_global_message():
    data = request.get_json(silent=True) or {}
    sender = data.get("sender")
    message = data.get("message")

    if sender is None or message is None:
        return jsonify({"status": "data is missing"}), 401

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute(
            "INSERT INTO chat_global (sender, message) VALUES (%s, %s)",
            (sender, message)
        )
        conn.commit()
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        cur.close()
        conn.close()

    notificationsModule.CreateNotificationForEveryone(sender, "0000", "chat", message)
    return jsonify({"status": "the message has been sent"}), 200

@chat_global_bp.route("/view-global-message", methods=["POST"])
def view_global_message():
    data = request.get_json(silent=True) or {}
    raw_id = data.get("id", 0)

    try:
        last_id = int(raw_id)
    except (ValueError, TypeError):
        last_id = 0

    conn = get_db()
    cur = conn.cursor()

    if last_id == 0:
        cur.execute("""
            SELECT sender, message, id FROM (
                SELECT sender, message, id
                FROM chat_global
                ORDER BY id DESC
                LIMIT 20
            ) AS subquery
            ORDER BY id ASC
        """)
    else:
        cur.execute("""
            SELECT sender, message, id
            FROM chat_global
            WHERE id > %s
            ORDER BY id ASC
        """, (last_id,))

    rows = cur.fetchall()
    
    messages = [dict(row) for row in rows]

    cur.close()
    conn.close()
    return jsonify(messages), 200