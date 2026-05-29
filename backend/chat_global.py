from flask import Flask, Blueprint, request, jsonify
import sqlite3
import os
chat_global_bp = Blueprint("chat_global", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
db_file = db_dir + "/chat_global.db"
def get_db():
    conn = sqlite3.connect(db_file)
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""CREATE TABLE IF NOT EXISTS chat_global(
                sender TEXT,
                message TEXT,
                id INTEGER PRIMARY KEY AUTOINCREMENT)""")
    conn.commit()
    conn.close()
create_db()
@chat_global_bp.route("/send-global-message", methods=["POST"])
def send_global_message():
    data = request.get_json()
    sender = data.get("sender")
    message = data.get("message")
    if sender == None or message == None:
        return jsonify({"status":"data is missing"}),401
    conn = get_db()
    cur = conn.cursor()
    cur.execute("INSERT INTO chat_global (sender, message) VALUES (?, ?)",(sender, message))
    conn.commit()
    conn.close()
    return jsonify({"status":"the message has been sent"}),200
@chat_global_bp.route("/view-global-message", methods=["POST"])
def view_global_message():

    data = request.get_json()

    last_id = data.get("id", 0)

    conn = get_db()
    cur = conn.cursor()

    if int(last_id) == 0:

        cur.execute("""
            SELECT sender, message, id
            FROM chat_global
            ORDER BY id DESC
            LIMIT 20
        """)

        rows = cur.fetchall()

        messages = []

        for row in rows:

            messages.append({
                "sender": row[0],
                "message": row[1],
                "id": row[2]
            })

        conn.close()

        return jsonify(messages)

    else:

        cur.execute("""
            SELECT sender, message, id
            FROM chat_global
            WHERE id > ?
            ORDER BY id ASC
        """, (last_id,))

        rows = cur.fetchall()

        messages = []

        for row in rows:

            messages.append({
                "sender": row[0],
                "message": row[1],
                "id": row[2]
            })

        conn.close()

        return jsonify(messages)
