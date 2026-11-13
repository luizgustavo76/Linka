import os
import psycopg2
from psycopg2.extras import RealDictCursor, execute_values
from flask import Blueprint, request, jsonify, g
from datetime import datetime

poll_bp = Blueprint("poll_bp", __name__)
DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def create_db():
    conn = get_db()
    cur = conn.cursor()

    cur.execute("""
    CREATE TABLE IF NOT EXISTS poll (
        id SERIAL PRIMARY KEY,
        title TEXT NOT NULL,
        username TEXT NOT NULL,
        post_id TEXT UNIQUE,
        expires_at TIMESTAMP NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
    )
    """)

    cur.execute("""
    CREATE TABLE IF NOT EXISTS poll_options (
        id SERIAL PRIMARY KEY,
        poll_id INTEGER NOT NULL REFERENCES poll(id) ON DELETE CASCADE,
        option_text TEXT NOT NULL,
        votes_count INTEGER NOT NULL DEFAULT 0
    )
    """)

    cur.execute("""
    CREATE TABLE IF NOT EXISTS poll_votes (
        id SERIAL PRIMARY KEY,
        poll_id INTEGER NOT NULL REFERENCES poll(id) ON DELETE CASCADE,
        option_id INTEGER NOT NULL REFERENCES poll_options(id) ON DELETE CASCADE,
        username TEXT NOT NULL,
        created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
        UNIQUE (poll_id, username)
    )
    """)

    cur.execute("CREATE INDEX IF NOT EXISTS idx_poll_post_id ON poll(post_id)")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_poll_options_poll_id ON poll_options(poll_id)")

    conn.commit()
    cur.close()
    conn.close()
create_db()
@poll_bp.route("/create-poll", methods=["POST"])
def create_poll():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        title = data.get("title")
        options = data.get("options") or []
        post_id = data.get("post_id")
        end_at = data.get("end_at")

        clean_options = [opt.strip() for opt in options if isinstance(opt, str) and opt.strip()]
        if len(clean_options) < 2:
            return jsonify({"status": "at least 2 options required"}), 400
        if len(clean_options) > 8:
            return jsonify({"status": "too options, limit are 8"}), 400

        conn = None
        cur = None
        try:
            conn = get_db()
            cur = conn.cursor()
            cur.execute("""
                INSERT INTO poll(title, username, post_id, expires_at)
                VALUES(%s, %s, %s, %s)
                RETURNING id
            """, (title, username, post_id, end_at))
            poll_id = cur.fetchone()["id"]

            values = [(poll_id, opt) for opt in clean_options]
            execute_values(
                cur,
                "INSERT INTO poll_options(poll_id, option_text) VALUES %s",
                values
            )

            conn.commit()
            return jsonify({"status": "ok", "poll_id": poll_id}), 201
        except Exception as e:
            if conn:
                conn.rollback()
            return jsonify({"status": "error", "message": str(e)}), 500
        finally:
            if cur:
                cur.close()
            if conn:
                conn.close()
    return jsonify({"status": "unauthorized"}), 403
@poll_bp.route("/vote-poll", methods=["POST"])
def vote_poll():
    data = request.get_json() or {}
    username = data.get("username")
    if username != g.username:
        return jsonify({"status": "forbidden"}), 403

    poll_id = data.get("poll_id")
    option_id = data.get("option_id")
    if not poll_id or not option_id:
        return jsonify({"status": "poll_id or option_id is missing"}), 400

    conn = None
    cur = None
    try:
        conn = get_db()
        cur = conn.cursor()
        cur.execute("""
            DELETE FROM poll_votes
            WHERE username = %s AND poll_id = %s
            RETURNING option_id
        """, (username, poll_id))
        prev = cur.fetchone()

        if prev:
            cur.execute("""
                UPDATE poll_options
                SET votes_count = GREATEST(votes_count - 1, 0)
                WHERE id = %s
            """, (prev["option_id"],))

            if prev["option_id"] == int(option_id):
                conn.commit()
                return jsonify({"status": "the vote has removed", "voted_option_id": None}), 200

        cur.execute("""
            INSERT INTO poll_votes (poll_id, option_id, username)
            VALUES (%s, %s, %s)
        """, (poll_id, option_id, username))

        cur.execute("""
            UPDATE poll_options
            SET votes_count = votes_count + 1
            WHERE id = %s AND poll_id = %s
        """, (option_id, poll_id))

        conn.commit()
        return jsonify({"status": "voted in poll", "voted_option_id": int(option_id)}), 200

    except Exception as e:
        if conn:
            conn.rollback()
        return jsonify({"status": "error", "error": str(e)}), 500
    finally:
        if cur:
            cur.close()
        if conn:
            conn.close()
