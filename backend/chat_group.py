from flask import Flask, Blueprint, request, jsonify, g
import sqlite3
import os
import datetime

chat_group_bp = Blueprint("chat_group", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")

os.makedirs(db_dir, exist_ok=True)
db_file = os.path.join(db_dir, "chat_group.db")

def get_db():
    conn = sqlite3.connect(db_file)
    cursor = conn.cursor()
    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA synchronous=NORMAL;")
    conn.execute("PRAGMA cache_size=-10000;")
    return conn

def create_db():
    conn = get_db()
    cur = conn.cursor()
    
    cur.execute("PRAGMA foreign_keys = ON;")
    
    cur.execute("""CREATE TABLE IF NOT EXISTS meta(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT,
                    username_owner TEXT,
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
    
    cur.execute("CREATE INDEX IF NOT EXISTS idx_chat_group_id ON chat_group(group_id);")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_users_group_id ON users_in_group(group_id);")
    
    conn.commit()
    conn.close()

create_db()
@chat_group_bp.route("/new-post-group",methods=["POST"])
def create_post_group():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        text_post = data.get("text_post")
        date = datetime.datetime.now()
        group_id = data.get("group_id")
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT username FROM users_in_group WHERE group_id = ? AND username = ?", (group_id, sender))
        result = cur.fetchone()
        if result:
            cur.execute("INSERT INTO post_group (group_id, username, text_post, stars, create_data) VALUES(?,?,?,?,?)",(group_id, username, text_post, "0", date))
            return jsonify({"status":"the post has created"}),200
        conn.commit()
        conn.close()
        if not result:
            return jsonify({"status":"you arent in this group"}),403
@chat_group_bp.route("/view-posts-group",methods=["POST"])
def view_posts_group():
    data = request.get_json()
    username = data.get("username")
    group_id = data.get("group_id")
    if username == g.username:
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT username FROM users_in_group WHERE group_id = ? AND username = ?", (group_id, sender))
        result = cur.fetchone()
        if result:
            cur.execute("SELECT * FROM post_group WHERE group_id = ? ORDER BY id DESC",(group_id,))
            posts = cur.fetchall()
            list_posts = []
            for post in posts:
                list_posts.append({
                    "id": post[0],
                    "username": post[1],
                    "group_id":post[2],
                    "stars":post[3],
                    "text_post": post[4],
                    "datetime": post[5]
                })
            return jsonify({list_posts}),200

@chat_group_bp.route("/create-group", methods=["POST"])
def create_group():
    data = request.get_json() or {}
    username = data.get("username")
    if username == getattr(g, 'username', None):
        name_group = data.get("name_group")
        description = data.get("description")   
        create_date = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
        conn = get_db()
        cur = conn.cursor()
        cur.execute("INSERT INTO meta (username_owner, name, description, create_date) VALUES(?,?,?,?)",
                    (username, name_group, description, create_date))
        cur.execute("SELECT id FROM meta WHERE username_owner = ?",(username,))
        group_id = cur.lastrowid
        permission = "admin"
        cur.execute("INSERT INTO users_in_group (group_id, username, entrance_date, permissions) VALUES (?,?,?,?)", (group_id,username,create_date, permission))
        conn.commit()
        conn.close()
        return jsonify({"status": "the group has created!"}), 200
        
    return jsonify({"status": "Unauthorized context or user mismatch"}), 403

@chat_group_bp.route("/send-group-message", methods=["POST"])
def send_group_message():
    data = request.get_json() or {}
    sender = data.get("sender")
    group_id = data.get("group_id")
    message = data.get("message")
    
    if not sender or not group_id or not message:
        return jsonify({"status": "data is missing"}), 400
    
    conn = get_db()
    cur = conn.cursor()
    
    cur.execute("SELECT username FROM users_in_group WHERE group_id = ? AND username = ?", (group_id, sender))
    result = cur.fetchone()
    
    if result:
        cur.execute("INSERT INTO chat_group (group_id, sender, message) VALUES (?, ?, ?)", (group_id, sender, message))
        conn.commit()
        conn.close()
        return jsonify({"status": "the message has been sent"}), 200
    else:
        conn.close()
        return jsonify({"status": "User is not a member of this group"}), 403
@chat_group_bp.route("/in-group",methods=["POST"])
def in_group():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        group_id = data.get("group_id")
@chat_group_bp.route("/view-group-message", methods=["POST"])
def view_group_message():
    data = request.get_json() or {}
    last_id = data.get("id", 0)
    group_id = data.get("group_id")
    
    if not group_id:
        return jsonify({"status": "group_id is required"}), 400
        
    conn = get_db()
    cur = conn.cursor()

    try:
        if int(last_id) == 0:
            cur.execute("""
                SELECT sender, message, id FROM (
                    SELECT sender, message, id
                    FROM chat_group
                    WHERE group_id = ?
                    ORDER BY id DESC
                    LIMIT 20
                ) ORDER BY id ASC
            """, (group_id,))
            rows = cur.fetchall()
        else:
            cur.execute("""
                SELECT sender, message, id
                FROM chat_group
                WHERE id > ? AND group_id = ?
                ORDER BY id ASC
            """, (last_id, group_id))
            rows = cur.fetchall()
    except ValueError:
        conn.close()
        return jsonify({"status": "Invalid last_id format"}), 400

    messages = []
    for row in rows:
        messages.append({
            "sender": row[0],
            "message": row[1],
            "id": row[2]
        })

    conn.close()
    return jsonify(messages)
@chat_group_bp.route("/my-groups",methods=["POST"])
def my_groups():
    data = request.get_json()
    username = data.get("username")
    if g.username == username:
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT * FROM users_in_group WHERE username = ?",(username,))
        result = cur.fetchall()
        conn.close()
        return jsonify(result)
    else:
        return jsonify({"status":"forbidden"}),403