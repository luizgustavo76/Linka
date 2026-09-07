from flask import Blueprint, request, jsonify, g
import sqlite3
import os
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
mentions_bp = Blueprint("mentions_bp", __name__)
def get_db():
    conn = sqlite3.connect(db_dir + "/mentions.db")
    conn.row_factory = sqlite3.Row
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""CREATE TABLE IF NOT EXISTS mentions(
    username TEXT,
    author TEXT,
    content TEXT,
    type TEXT,
    post_id INTEGER,
    comment_id INTEGER)""")
    conn.commit()
    conn.close()
create_db()
@mentions_bp.route("/view-mentions", methods=["POST"])
def view_mentios():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        type = data.get("type")
        conn = get_db()
        cur = conn.cursor()
        if type:
            cur.execute("SELECT * FROM mentions WHERE username = ? AND type = ?", (username, type))
            rows = cur.fetchall()
            result = [dict(row) for row in rows]
            return jsonify(result),200
        else:
            cur.execute("SELECT * FROM mentions WHERE username = ?", (username,))
            rows = cur.fetchall()
            result = [dict(row) for row in rows]
            return jsonify(result),200
    else:
        return jsonify({"status":"forbidden"}),403