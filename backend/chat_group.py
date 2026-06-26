from flask import Flask, Blueprint, request, jsonify, g
import sqlite3
import os
chat_group_bp = Blueprint("chat_group", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
db_file = db_dir + "/chat_group.db"
def get_db():
    conn = sqlite3.connect(db_file)
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""CREATE TABLE IF NOT EXISTS meta(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT,
                    description TEXT,
                    create_date TEXT,
                    rules TEXT)""")
    cur.execute("""CREATE TABLE IF NOT EXISTS chat_group(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id INTEGER,
                    sender TEXT,
                    message TEXT,
                    FOREIGN KEY(group_id) REFERENCES meta(id))""")
    cur.execute("""CREATE TABLE IF NOT EXISTS post_group(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id INTEGER,
                    username TEXT,
                    text_post TEXT,
                    stars INTEGER,
                    create_data TEXT,
                    FOREIGN KEY(group_id) REFERENCES meta(id))""")
    cur.execute("""CREATE TABLE IF NOT EXISTS users_in_group(
                    group_id INTEGER,
                    username TEXT,
                    entrance_date TEXT,
                    permissions TEXT,
                    FOREIGN KEY(group_id) REFERENCES meta(id))""")
    conn.commit()
    conn.close()
create_db()
@chat_group_bp.route("/create-group", methods=["POST"])
def create_group():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        name_group = data.get("name_group")
@chat_group_bp.route("/send-group-message", methods=["POST"])
def send_group_message():
    data = request.get_json()
    sender = data.get("sender")
    group_id = data.get("group_id")
    message = data.get("message")
    if sender == None or message == None:
        return jsonify({"status":"data is missing"}),401
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT username FROM users_in_group WHERE group_id = ?",(group_id,))
    result = cur.fetchone()
    if result:
        cur.execute("INSERT INTO chat_group (sender, message) VALUES (?, ?)",(sender, message))
    conn.commit()
    conn.close()
    return jsonify({"status":"the message has been sent"}),200
@chat_group_bp.route("/view-group-message", methods=["POST"])
def view_group_message():
    data = request.get_json()
    last_id = data.get("id", 0)
    group_id = data.get("group_id")
    conn = get_db()
    cur = conn.cursor()

    if int(last_id) == 0:
        cur.execute("""
            SELECT sender, message, id FROM (
                SELECT sender, message, id
                FROM chat_group
                ORDER BY id DESC
                LIMIT 20
                WHERE group_id = ?
            ) ORDER BY id ASC
        """,(group_id,))
        rows = cur.fetchall()
    else:
        
        cur.execute("""
            SELECT sender, message, id
            FROM chat_group
            WHERE id > ? AND group_id = ?
            ORDER BY id ASC
        """, (last_id,group_id))
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
