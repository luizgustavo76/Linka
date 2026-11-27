import os
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, request, jsonify, g
from datetime import datetime, timezone

status_bp = Blueprint("status", __name__)

DATABASE_URL = os.getenv("DATABASE_URL")


def get_db():
    if not DATABASE_URL:
        raise RuntimeError("DATABASE_URL não configurada no ambiente.")
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)


def create_db():
    if not DATABASE_URL:
        return

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("""
            CREATE TABLE IF NOT EXISTS status (
                id SERIAL PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                status TEXT,
                action TEXT
            )
        """)
        conn.commit()
    except Exception as e:
        conn.rollback()
        print(f"[ERRO create_db status]: {e}")
    finally:
        cur.close()
        conn.close()
create_db()

@status_bp.route("/send-status", methods=["POST"])
def send_status():
    data = request.get_json(silent=True) or {}
    username = data.get("username")

    current_user = getattr(g, "username", None)
    if not username or username != current_user:
        return jsonify({"status": "forbidden"}), 403

    status_val = data.get("status")
    action_val = data.get("action")
    timestamp_val = datetime.now(timezone.utc)

    conn = None
    cur = None
    try:
        conn = get_db()
        cur = conn.cursor()
        cur.execute(
            """
            INSERT INTO status (username, status, timestamp, action)
            VALUES (%s, %s, %s, %s)
            ON CONFLICT (username) DO UPDATE
            SET status = EXCLUDED.status,
                timestamp = EXCLUDED.timestamp,
                action = EXCLUDED.action;
            """,
            (username, status_val, timestamp_val, action_val),
        )
        conn.commit()
        return jsonify({"status": "status updated successfully"}), 200
    except Exception as e:
        if conn:
            conn.rollback()
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        if cur:
            cur.close()
        if conn:
            conn.close()


@status_bp.route("/view-status", methods=["POST"])
def view_status():
    data = request.get_json(silent=True) or {}
    username_target = data.get("username_target")

    if not username_target:
        return jsonify({"status": "username_target is required"}), 400

    conn = None
    cur = None
    try:
        conn = get_db()
        cur = conn.cursor()
        cur.execute(
            "SELECT username, status, action, timestamp FROM status WHERE username = %s",
            (username_target,),
        )
        result = cur.fetchone()

        if result:
            payload = dict(result)
            payload["timestamp"] = payload["timestamp"].isoformat() if payload.get("timestamp") else None
            return jsonify(payload), 200

        return jsonify({"status": "status not found"}), 404
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        if cur:
            cur.close()
        if conn:
            conn.close()


@status_bp.route("/view-online", methods=["POST"])
def view_online():
    data = request.get_json(silent=True) or {}
    target = data.get("target")
    target_user = data.get("target_user")

    conn = None
    cur = None
    try:
        conn = get_db()
        cur = conn.cursor()

        if target == "global":
            cur.execute("SELECT username FROM status WHERE status = %s", ("online",))
            results = cur.fetchall()
            online_users = [row["username"] for row in results]
            return jsonify({"online_users": online_users}), 200

        if target == "dm":
            if not target_user:
                return jsonify({"status": "target_user parameter missing"}), 400

            cur.execute(
                "SELECT username, status, action FROM status WHERE status = %s AND username = %s",
                ("online", target_user),
            )
            result = cur.fetchone()
            return jsonify({
                "is_online": result is not None,
                "data": dict(result) if result else None,
            }), 200

        return jsonify({"status": "invalid target mode"}), 400
    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        if cur:
            cur.close()
        if conn:
            conn.close()