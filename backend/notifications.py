import os
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, request, jsonify

notifications_blueprint = Blueprint("notifications", __name__)

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
            CREATE TABLE IF NOT EXISTS notifications (
                id SERIAL PRIMARY KEY,
                receiver TEXT NOT NULL, 
                from_user TEXT, 
                datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP, 
                type TEXT, 
                content TEXT, 
                read BOOLEAN DEFAULT FALSE
            )
        """)
        conn.commit()
    except Exception as e:
        conn.rollback()
        print(f"[ERRO create_db notifications]: {e}")
    finally:
        cur.close()
        conn.close()

@notifications_blueprint.route("/notifications", methods=["POST"])
def notifications():
    data = request.get_json(silent=True) or {}
    username = data.get("username")

    if not username:
        return jsonify([]), 200

    username = str(username).strip()

    conn = get_db()
    cur = conn.cursor()
    try:
        # Busca notificações pendentes de leitura para o usuário
        cur.execute(
            """
            SELECT id, receiver, from_user, datetime, type, content, read 
            FROM notifications 
            WHERE receiver = %s AND (read = FALSE OR read IS NULL)
            ORDER BY id DESC
            """,
            (username,)
        )
        result = cur.fetchall()

        notifications_list = []
        for row in result:
            notifications_list.append({
                "id": row["id"],
                "receiver": row["receiver"],
                "from_user": row["from_user"],
                "datetime": str(row["datetime"]) if row["datetime"] else None,
                "type": row["type"],
                "content": row["content"],
                "read": bool(row["read"])
            })

        return jsonify(notifications_list), 200
    except Exception as e:
        return jsonify({"error": "Server error", "details": str(e)}), 500
    finally:
        cur.close()
        conn.close()

@notifications_blueprint.route("/set-read-notification", methods=["POST"])
def set_read():
    data = request.get_json(silent=True) or {}
    id_notif = data.get("id")

    if id_notif is None:
        return jsonify({"error": "Missing notification id"}), 400

    try:
        id_notif = int(id_notif)
    except (ValueError, TypeError):
        return jsonify({"error": "Invalid ID format"}), 400

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("UPDATE notifications SET read = TRUE WHERE id = %s", (id_notif,))
        conn.commit()
        return jsonify({"status": "success"}), 200
    except Exception as e:
        conn.rollback()
        return jsonify({"error": "Server error", "details": str(e)}), 500
    finally:
        cur.close()
        conn.close()