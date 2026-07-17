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

@chat_group_bp.route("/new-post-group", methods=["POST"])
def create_post_group():
    data = request.get_json() or {}
    username = data.get("username")
    
    if username != getattr(g, 'username', None):
        return jsonify({"status": "Unauthorized"}), 403

    text_post = data.get("text_post")
    group_id = data.get("group_id")
    date_str = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    conn = get_db()
    cur = conn.cursor()
    # Correção: alterado 'sender' indefinido para 'username'
    cur.execute("SELECT username FROM users_in_group WHERE group_id = ? AND username = ?", (group_id, username))
    result = cur.fetchone()
    
    if result:
        cur.execute("INSERT INTO post_group (group_id, username, text_post, stars, create_data) VALUES(?,?,?,?,?)",
                    (group_id, username, text_post, 0, date_str))
        conn.commit()
        conn.close()
        return jsonify({"status": "success", "message": "The post has been created"}), 200
    
    conn.close()
    return jsonify({"status": "error", "message": "You aren't in this group"}), 403

@chat_group_bp.route("/view-posts-group", methods=["POST"])
def view_posts_group():
    data = request.get_json() or {}
    username = data.get("username")
    group_id = data.get("group_id")
    
    if username != getattr(g, 'username', None):
        return jsonify({"status": "Unauthorized"}), 403

    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT username FROM users_in_group WHERE group_id = ? AND username = ?", (group_id, username))
    result = cur.fetchone()
    
    if not result:
        conn.close()
        return jsonify({"status": "error", "message": "Access denied"}), 403

    cur.execute("SELECT id, group_id, username, text_post, stars, create_data FROM post_group WHERE group_id = ? ORDER BY id DESC", (group_id,))
    posts = cur.fetchall()
    conn.close()
    
    # JSON estruturado limpo e explícito para o C++
    list_posts = [{
        "id": post[0],
        "group_id": post[1],
        "username": post[2],
        "text_post": post[3],
        "stars": post[4],
        "datetime": post[5]
    } for post in posts]
    
    return jsonify({"status": "success", "posts": list_posts}), 200

@chat_group_bp.route("/create-group", methods=["POST"])
def create_group():
    data = request.get_json() or {}
    username = data.get("username")
    
    if username != getattr(g, 'username', None):
        return jsonify({"status": "error", "message": "Unauthorized context"}), 403
        
    name_group = data.get("name_group")
    description = data.get("description")   
    create_date = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    conn = get_db()
    cur = conn.cursor()
    cur.execute("INSERT INTO meta (username_owner, name, description, create_date) VALUES(?,?,?,?)",
                (username, name_group, description, create_date))
    group_id = cur.lastrowid
    
    cur.execute("INSERT INTO users_in_group (group_id, username, entrance_date, permissions) VALUES (?,?,?,?)", 
                (group_id, username, create_date, "admin"))
    conn.commit()
    conn.close()
    
    return jsonify({"status": "success", "message": "Group created!", "group_id": group_id}), 200

@chat_group_bp.route("/send-group-message", methods=["POST"])
def send_group_message():
    data = request.get_json() or {}
    sender = data.get("sender")
    group_id = data.get("group_id")
    message = data.get("message")
    
    if not sender or not group_id or not message:
        return jsonify({"status": "error", "message": "Data is missing"}), 400
    
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT username FROM users_in_group WHERE group_id = ? AND username = ?", (group_id, sender))
    result = cur.fetchone()
    
    if result:
        cur.execute("INSERT INTO chat_group (group_id, sender, message) VALUES (?, ?, ?)", (group_id, sender, message))
        conn.commit()
        conn.close()
        return jsonify({"status": "success", "message": "Message sent"}), 200
    
    conn.close()
    return jsonify({"status": "error", "message": "User is not a member of this group"}), 403

@chat_group_bp.route("/view-group-message", methods=["POST"])
def view_group_message():
    data = request.get_json() or {}
    last_id = data.get("id", 0)
    group_id = data.get("group_id")
    
    if not group_id:
        return jsonify({"status": "error", "message": "group_id is required"}), 400
        
    conn = get_db()
    cur = conn.cursor()

    try:
        if int(last_id) == 0:
            cur.execute("""
                SELECT sender, message, id FROM (
                    SELECT sender, message, id FROM chat_group
                    WHERE group_id = ? ORDER BY id DESC LIMIT 20
                ) ORDER BY id ASC
            """, (group_id,))
        else:
            cur.execute("""
                SELECT sender, message, id FROM chat_group
                WHERE id > ? AND group_id = ? ORDER BY id ASC
            """, (last_id, group_id))
        rows = cur.fetchall()
    except ValueError:
        conn.close()
        return jsonify({"status": "error", "message": "Invalid last_id format"}), 400
    finally:
        conn.close()

    messages = [{"sender": row[0], "message": row[1], "id": row[2]} for row in rows]
    return jsonify({"status": "success", "messages": messages}), 200

@chat_group_bp.route("/my-groups", methods=["POST"])
def my_groups():
    data = request.get_json() or {}
    username = data.get("username")
    
    if getattr(g, 'username', None) != username:
        return jsonify({"status": "error", "message": "Forbidden"}), 403
        
    conn = get_db()
    cur = conn.cursor()
    # Fazemos um JOIN com a tabela 'meta' para trazer o nome real do grupo, não apenas IDs!
    cur.execute("""
        SELECT u.group_id, m.name, u.permissions 
        FROM users_in_group u
        JOIN meta m ON u.group_id = m.id
        WHERE u.username = ?
    """, (username,))
    result = cur.fetchall()
    conn.close()
    
    # Transforma o array do SQL em objetos fáceis com chaves nomeadas
    groups_list = [{
        "group_id": row[0],
        "group_name": row[1],
        "permissions": row[2]
    } for row in result]
    
    return jsonify({"status": "success", "groups": groups_list}), 200