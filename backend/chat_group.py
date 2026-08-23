from flask import Flask, Blueprint, request, jsonify, g
import sqlite3
import os
import datetime
import secrets
import notificationsModule
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
                    create_data TEXT,
                    rules TEXT)""")

    cur.execute("""CREATE TABLE IF NOT EXISTS channels(
                    group_id INTEGER,
                    name TEXT,
                    type TEXT,
                    FOREIGN KEY(group_id) REFERENCES meta(id))""")

    cur.execute("""CREATE TABLE IF NOT EXISTS chat_group(
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    group_id INTEGER,
                    sender TEXT,
                    message TEXT,
                    channel TEXT,
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
    cur.execute("""CREATE TABLE IF NOT EXISTS invites(
                group_id INTEGER,
                creator TEXT,
                created_at TEXT,
                expire_at TEXT,
                code TEXT,
                acess_limit INTEGER,
                uses INTEGER DEFAULT 0,
                status TEXT DEFAULT 'ACTIVE')""")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_chat_group_id ON chat_group(group_id);")
    cur.execute("CREATE INDEX IF NOT EXISTS idx_users_group_id ON users_in_group(group_id);")
    
    conn.commit()
    conn.close()
create_db()
EXPIRE_MAP = {
    "5 minutes": datetime.timedelta(minutes=5),
    "10 minutes": datetime.timedelta(minutes=10),
    "30 minutes": datetime.timedelta(minutes=30),
    "1 hour": datetime.timedelta(hours=1),
    "3 hour": datetime.timedelta(hours=3),
    "10 hour": datetime.timedelta(hours=10),
    "1 day": datetime.timedelta(days=1),
    "1 week": datetime.timedelta(weeks=1),
    "1 month": datetime.timedelta(days=30),
    "3 month": datetime.timedelta(days=90),
    "never expire": datetime.timedelta(days=99999)
}

@chat_group_bp.route("/generate-invite",methods=["POST"])
def generate_invite():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        group_id = data.get("group_id")
        expire_option = data.get("expire_at")
        acess_limit = data.get("acess_limit")
        if None in [group_id, expire_option, acess_limit]:
            return jsonify({"status":"data is missing"}),401
        if not expire_option in EXPIRE_MAP:
            return jsonify({"status":"invalid expire data"}),401
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT username FROM users_in_group WHERE username = ? AND group_id = ?",(username, group_id))
        result = cur.fetchone()
        if result:
            code = secrets.token_hex(4)
            created_at = datetime.datetime.now()
            delta = EXPIRE_MAP[expire_option]
            expire_at =  (created_at + delta).strftime("%Y-%m-%d %H:%M:%S")
            cur.execute("INSERT INTO invites (group_id, creator, created_at, expire_at, code, acess_limit) VALUES(?, ?, ?, ?, ?, ?)",(group_id, username, created_at, expire_at, code, acess_limit))
            conn.commit()
            conn.close()
            return jsonify({"status":"invite created", "code":code}),200
        else:
            conn.close()
            return jsonify({"status":"you arent a member"}),403
    else:
        return jsonify({"status":"forbidden"}),403
@chat_group_bp.route("/exit-group", methods=["POST"])
def exit_server():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        group_id = data.get("group_id")
        if group_id == None:
            return jsonify({"status":"data is missing"}),400
        conn = get_db()
        cur = conn.cursor()
        cur.execute("DELETE FROM users_in_group WHERE username = ? AND group_id = ?",(username, group_id))
        conn.commit()
        conn.close()
    else:
        return jsonify({"status":"forbidden"}),403
@chat_group_bp.route("/rename-group", methods=["POST"])
def rename_group():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        group_id = data.get("group_id")
        new_name = data.get("new_name")
        if None in [group_id, new_name]:
            return jsonify({"status":"data is missing"}),401
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT username FROM users_in_group WHERE username = ? AND group_id = ? AND permissions = 'admin'",(username, group_id))
        result = cur.fetchone()
        if result:
            cur.execute("UPDATE meta SET name = ? WHERE group_id = ?",(new_name, group_id))
            conn.commit()
            conn.close()
        else:
            conn.close()
            return jsonify({"status":"you arent a admin!"}),403
    else:
        return jsonify({"status":"forbidden"}),403
@chat_group_bp.route("/join-group", methods=["POST"])
def join_group():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        code = data.get("code")
        conn = get_db()
        cur = conn.cursor()
        cur.execute(
            """SELECT group_id FROM invites 
            WHERE code = ? AND status = 'ACTIVE' 
            AND uses < acess_limit 
            AND datetime(expire_at) > datetime('now')""", 
            (code,)
        )
        result = cur.fetchone()
        if result:
            group_id = result[1]
            cur.execute("SELECT username FROM users_in_group WHERE username = ? AND group_id = ?",(username, group_id))
            result_user = cur.fetchone()
            if result_user:
                return jsonify({"status":"you already in group"}),401
            entrande_data = datetime.datetime.now()
            cur.execute("INSERT INTO users_in_group (username, group_id, entrance_date, permissions) VALUES(?, ?, ?, ?)",(username, group_id, entrande_data, "member"))
            cur.execute("UPDATE invites SET uses = uses + 1 WHERE code = ?", (code,))
            conn.commit()
            conn.close()
            return jsonify({"status":"you are now a member"}),200
        else:
            conn.close()
            return jsonify({"status":"code is invalid, check the capacity of acess, contact the invite sender"}),401
    else:
        return jsonify({"status":"forbidden"}),403
@chat_group_bp.route("/remove-channel", methods=["POST"])
def remove_channel():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        channel_name = data.get("channel_name")
        group_id = data.get("group_id")
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT username FROM users_in_group WHERE username = ? AND group_id = ? AND permissions = 'admin'",(username, group_id))
        result = cur.fetchone()
        if result:
            cur.execute("DELETE FROM channels WHERE name = ? AND group_id = ?",(channel_name, group_id))
            conn.commit()
            conn.close()
            return jsonify({"status":"the channel has removed"}),200
        else:
            conn.close()
            return jsonify({"status":"you arent admin"})
    else:
        return jsonify({"status":"forbidden"}),403
@chat_group_bp.route("/new-channel", methods=["POST"])
def new_channel():
    data = request.get_json()
    username = data.get("username")
    channel_name = data.get("channel_name")
    group_id = data.get("group_id")
    type = data.get("type")
    if None in [username, channel_name, group_id, type]:
        return jsonify({"status":"data is missing"})
    else:
        if username == g.username:
            conn = get_db()
            cur = conn.cursor()
            cur.execute("SELECT username FROM users_in_group WHERE username = ? AND permissions = 'admin' AND group_id = ?",(username, group_id))
            result = cur.fetchone()
            if result:
                cur.execute("INSERT INTO channels (name, type, group_id) VALUES(?,?,?)",(channel_name, type, group_id))
                conn.commit()
                conn.close()
                return jsonify({"status":"channel created!"}),201
            else:
                conn.close()
                return jsonify({"status":"you aren`t admin"}),403
        else:
            conn.close()
            return jsonify({"status":"forbidden"}),403
@chat_group_bp.route("/view-permissions", methods=["POST"])
def view_permissions():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        group_id = data.get("group_id")
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT username FROM users_in_group WHERE username = ? AND group_id = ?",(username, group_id))
        result = cur.fetchone()
        if result:
            cur.execute("SELECT permissions FROM users_in_group WHERE username = ? AND group_id = ?",(username, group_id))
            permission = cur.fetchone()
            conn.close()
            return jsonify({"permission":permission}),200
        else:
            conn.close()
            return jsonify({"status":"you arent in group"}),403
    else:
        return jsonify({"status":"forbidden"}),403
@chat_group_bp.route("/view-channels", methods=["POST"])
def view_channels():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    group_id = data.get("group_id")

    # Debug: verifique no console do Flask o que está chegando
    print(f"[DEBUG] payload username: {username} | g.username: {getattr(g, 'username', None)}")
    print(f"[DEBUG] payload group_id: {group_id} (tipo: {type(group_id)})")

    # 1. Validação do usuário
    if not username or username.strip() != str(getattr(g, "username", "")).strip():
        return jsonify({"status": "forbidden", "reason": "username mismatch"}), 403

    # 2. Validação do group_id
    if group_id is None:
        return jsonify({"status": "group_id is missing"}), 400

    try:
        group_id = int(group_id)
    except ValueError:
        return jsonify({"status": "invalid group_id"}), 400

    conn = get_db()
    cur = conn.cursor()    

    # 3. Verificação de membresia no grupo
    cur.execute("SELECT 1 FROM users_in_group WHERE username = ? AND group_id = ?", (username, group_id))
    user_in_group = cur.fetchone()

    if not user_in_group:
        conn.close()
        return jsonify({"status": "you aren't in group", "reason": "user not in users_in_group"}), 403

    cur.execute("SELECT group_id, name, type FROM channels WHERE group_id = ?", (group_id,))
    rows = cur.fetchall()
    conn.close()

    channels = []
    for row in rows:
        channels.append({
            "group_id": row[0],
            "channel_name": row[1],
            "type": row[2]
        })

    return jsonify(channels), 200
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
    create_data = datetime.datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    conn = get_db()
    cur = conn.cursor()
    cur.execute("INSERT INTO meta (username_owner, name, description, create_data) VALUES(?,?,?,?)",
                (username, name_group, description, create_data))
    group_id = cur.lastrowid
    
    cur.execute("INSERT INTO users_in_group (group_id, username, entrance_date, permissions) VALUES (?,?,?,?)", 
                (group_id, username, create_data, "admin"))
    conn.commit()
    conn.close()
    
    return jsonify({"status": "success", "message": "Group created!", "group_id": group_id}), 200

@chat_group_bp.route("/send-group-message", methods=["POST"])
def send_group_message():
    data = request.get_json() or {}
    sender = data.get("sender")
    group_id = data.get("group_id")
    message = data.get("message")
    channel = data.get("channel")
    if sender == g.username:
        if not sender or not group_id or not message or not channel:
            return jsonify({"status": "error", "message": "Data is missing"}), 400
        if message.startswith("@"):
            mention = message[1:]
            date = datetime.datetime.now()
            notificationsModule.CreateNotification(sender, mention, date, "chat", message)
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT username FROM users_in_group WHERE group_id = ? AND username = ?", (group_id, sender))
        result = cur.fetchone()
        
        if result:
            cur.execute("INSERT INTO chat_group (group_id, sender, message, channel) VALUES (?, ?, ?, ?)", (group_id, sender, message, channel))
            conn.commit()
            conn.close()
            return jsonify({"status": "success", "message": "Message sent"}), 200
        
        conn.close()
        return jsonify({"status": "error", "message": "User is not a member of this group"}), 403
    else:
        return jsonify({"status":"forbidden"}),403
@chat_group_bp.route("/members", methods=["POST"])
def members():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        group_id = data.get("group_id")
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT username FROM users_in_group WHERE username = ? AND group_id = ?",(username, group_id))
        result = cur.fetchone()
        if result:
            cur.execute("SELECT username FROM users_in_group WHERE group_id = ?",(group_id,))
            result_member = cur.fetchall()
            members = [row[0] for row in result_member]
            return jsonify({"group_id": group_id, "members": members})
    else:
        return jsonify({"status":"forbidden"}),403
@chat_group_bp.route("/remove-user", methods=["POST"])
def remove_user():
    data = request.get_json()
    username = data.get("username")
    group_id = data.get("group_id")
    if username == g.username:
        target_user = data.get("target_user")
        if None in [username, group_id, target_user]:
            return jsonify({"status":"data is missing"}),401
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT username FROM users_in_group WHERE username = ? AND group_id = ? AND permissions = 'admin'",(username, group_id))
        result = cur.fetchone()
        if result:
            cur.execute("DELETE FROM users_in_group WHERE username = ? AND group_id = ?",(target_user, group_id))
            conn.commit()
            conn.close()
            return jsonify({"status":"the user has banned with sucess"}),200
        else:
            conn.close()
            return jsonify({"status":"you arent a admin"}),403
    else:
        return jsonify({"status":"forbidden"}),403
@chat_group_bp.route("/view-group-message", methods=["POST"])
def view_group_message():
    data = request.get_json() or {}
    username = data.get("username")
    last_id = data.get("id", 0)
    group_id = data.get("group_id")
    channel = data.get("channel")
    if username == g.username:
        if not group_id or not channel:
            return jsonify({"status": "error", "message": "group_id is required"}), 400
            
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT username FROM users_in_group WHERE username = ? AND group_id = ?",(username, group_id))
        result = cur.fetchone()
        if not result:
            conn.close()
            return jsonify({"status":"you arent a member"}),403
        try:
            try:
                last_id_num = int(last_id) if last_id else 0
            except ValueError:
                last_id_num = 0

            if last_id_num == 0:
                cur.execute("""
                    SELECT sender, message, id FROM (
                        SELECT sender, message, id FROM chat_group
                        WHERE group_id = ? AND channel = ? 
                        ORDER BY id DESC LIMIT 20
                    ) AS subquery ORDER BY id ASC
                """, (group_id, channel))
            else:
                cur.execute("""
                    SELECT sender, message, id FROM chat_group
                    WHERE id > ? AND group_id = ? AND channel = ? 
                    ORDER BY id ASC
                """, (last_id_num, group_id, channel))
            rows = cur.fetchall()
        except ValueError:
            conn.close()
            return jsonify({"status": "error", "message": "Invalid last_id format"}), 400
        finally:
            conn.close()

        messages = [{"sender": row[0], "message": row[1], "id": row[2]} for row in rows]
        return jsonify({"status": "success", "messages": messages}), 200
    else:
        return jsonify({"status":"forbidden"}),403
@chat_group_bp.route("/my-groups", methods=["POST"])
def my_groups():
    data = request.get_json() or {}
    username = data.get("username")
    
    if getattr(g, 'username', None) != username:
        return jsonify({"status": "error", "message": "Forbidden"}), 403
        
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        SELECT u.group_id, m.name, u.permissions 
        FROM users_in_group u
        JOIN meta m ON u.group_id = m.id
        WHERE u.username = ?
    """, (username,))
    result = cur.fetchall()
    conn.close()
    
    groups_list = [{
        "group_id": row[0],
        "group_name": row[1],
        "permissions": row[2]
    } for row in result]
    
    return jsonify({"status": "success", "groups": groups_list}), 200