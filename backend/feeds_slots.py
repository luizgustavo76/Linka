import os
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, request, jsonify, g

feeds_slots_bp = Blueprint("feeds_slots", __name__)

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
    CREATE TABLE IF NOT EXISTS feeds_slots(
        id SERIAL PRIMARY KEY,
        username TEXT NOT NULL,
        feed_url TEXT NOT NULL,
        feed_name TEXT NOT NULL,
        slot INTEGER NOT NULL,
        UNIQUE(username, slot)
    )""")
    conn.commit()
    cur.close()
    conn.close()

create_db()

@feeds_slots_bp.route("/new-slot", methods=["POST"])
def new_slot():
    data = request.get_json()
    username = data.get("username")
    
    if username != g.username:
        return jsonify({"status": "unauthorized"}), 403

    feed_url = data.get("feed_url")
    feed_name = data.get("feed_name")
    target_slot = data.get("slot") 

    if None in (feed_url, feed_name):
        return jsonify({"status": "feed_url or feed_name are missing"}), 400

    conn = None
    cur = None
    try:
        conn = get_db()
        cur = conn.cursor()

        cur.execute(
            "SELECT slot, feed_url, feed_name FROM feeds_slots WHERE username = %s ORDER BY slot ASC",
            (username,)
        )
        current_slots = cur.fetchall()

        for row in current_slots:
            if row["feed_url"] == feed_url:
                return jsonify({"status": "already_in_slot", "slot": row["slot"]}), 200

        # 3. Define em qual número de slot (1 a 4) vai salvar
        if target_slot is not None:
            # Valida se o slot escolhido está entre 1 e 4
            if target_slot not in (1, 2, 3, 4):
                return jsonify({"status": "invalid_slot"}), 400
            slot_to_save = target_slot
        else:
            # Procura o primeiro número livre entre 1, 2, 3 e 4
            occupied = {row["slot"] for row in current_slots}
            free_slots = [s for s in (1, 2, 3, 4) if s not in occupied]

            if not free_slots:
                # Todos os 4 estão cheios: devolve a lista para o Android abrir o AlertDialog
                return jsonify({
                    "status": "slots_full",
                    "slots": current_slots
                }), 409

            slot_to_save = free_slots[0]
        cur.execute("""
            INSERT INTO feeds_slots (username, feed_url, feed_name, slot)
            VALUES (%s, %s, %s, %s)
            ON CONFLICT (username, slot) DO UPDATE SET
                feed_url = EXCLUDED.feed_url,
                feed_name = EXCLUDED.feed_name
        """, (username, feed_url, feed_name, slot_to_save))

        conn.commit()
        return jsonify({"status": "ok", "slot": slot_to_save}), 200

    except Exception as e:
        if conn:
            conn.rollback()
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        if cur:
            cur.close()
        if conn:
            conn.close()