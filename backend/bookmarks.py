from flask import Flask, jsonify, request, g, Blueprint
import sqlite3
import os
base_dir = os.path.dirname(os.path.abspath(__file__))
bookmarks_bp = Blueprint("bookmarks_bp", __name__)
db_dir = os.path.join(base_dir, "DB")
def get_db():
    conn = sqlite3.connect(db_dir + "/bookmarks.db")
    conn.row_factory = sqlite3.Row
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""CREATE TABLE IF NOT EXISTS bookmarks(
        username TEXT,
        post_id TEXT,
        is_public INTEGER,
        is_external INTEGER,
        source_url TEXT,
        original_id TEXT,
        platfrom TEXT
        """)
    conn.commit()
    conn.close()
create_db()
@bookmarks_bp.route("/view-bookmarks", methods=["POST"])
def view_bookmarks():
    data = request.get_json()
    username = data.get("username")
    receiver = data.get("receiver")
    if username == g.username:
        if receiver == username:
            conn = get_db()
            cur = conn.cursor()
            cur.execute("SELECT * FROM bookmarkers WHERE username = ?",(username,)
            rows = cur.fetchall()
            conn.close()
            result = [dict(row) for row in rows]
            return jsonify(result),200
        else:
            conn = get_db()
            cur = conn.cursor()
            cur.execute("SELECT * FROM bookmarkers WHERE username = ? AND is_public = 1",(username,)
            rows = cur.fetchall()
            conn.close()
            result = [dict(row) for row in rows]
            return jsonify(result),200
    else:
        return jsonify("status":"forbidden"),403
@bookmarks_bp.route("/toggle-bookmark",methods=["POST"])
def toggle_bookmark():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        post_id = data.get("post_id")
        is_public = data.get("is_public")
        original_id = data.get("original_id")
        is_external = data.get("is_external")
        source_url = data.get("source_url")
        platform = data.get("platform")
        if None in (is_public, is_external, platform):
            return jsonify({"status":"missing data"}),400
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT post_id FROM bookmarks WHERE post_id or original_id = ?",(post_id,))
        result = cur.fetchone()
        if result:
            cur.execute("DELETE FROM bookmarks WHERE post_id or original_id = ?",(post_id,))
            return jsonify({"status":"bookmark removed"}),200
        else:
            cur.execute("INSERT INTO bookmarks (username, post_id, is_public, is_external, source_url, platform, original_id) VALUES(?,?,?,?,?,?)",(username, post_id, is_public, is_external, source_url, platform, original_id))
            return jsonify("status":"bookmarked inserted"),200
        conn.commit()
        conn.close()
    else:
        return jsonify({"status":"forbidden"}),403
            