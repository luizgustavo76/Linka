import json
import os
modules_flags = {}
base_dir = os.path.dirname(os.path.abspath(__file__))
json_path = os.path.join(base_dir, "backend.json")
with open(json_path, "r") as f:
    modules_flags = json.load(f)
root_flags = modules_flags["modules-flags"]
from flask_cors import CORS 
from jobs import jobs_bp
if root_flags["post"]:
    from post import post_bp
if root_flags["chat"]:
    from chat import chat_bp
if root_flags["friends"]:
    from friends import friends_bp
from login import login_bp
if root_flags["profiles"]:
    from profiles import profile_bp
if root_flags["search"]:
    from search import search_bp
if root_flags["status"]:
    from status import status_bp
from meta import meta_bp
if root_flags["chat_global"]:
    from chat_global import chat_global_bp
if root_flags["federations"]:
    from federations import federations_bp
if root_flags["notifications"]:
    from notifications import notifications_blueprint
if root_flags["images"]:
    from images import image_bp
from flask import Flask, Blueprint, request, jsonify, g
import sqlite3
import os
if root_flags["federation_index"]:
    from federation_index import federation_index_bp
if root_flags["sincronizer"]:
    from sincronizer import sincronizer_bp
if root_flags["themes"]:
    from themes import theme_bp
from datetime import datetime, timedelta
from werkzeug.security import check_password_hash
import secrets
from chat_group import chat_group_bp
db_dir = os.path.join(base_dir, "DB")
tokens_file = os.path.join(db_dir, "tokens.db")
login_file = os.path.join(db_dir, "login.db")
banned_file = os.path.join(db_dir, "banned.db")
app = Flask(__name__)
def get_db():
    conn = sqlite3.connect(tokens_file)
    conn.row_factory = sqlite3.Row
    return conn

def get_db_login():
    conn = sqlite3.connect(login_file)
    conn.row_factory = sqlite3.Row
    return conn
def get_db_banned():
    conn = sqlite3.connect(banned_file)
    conn.row_factory = sqlite3.Row
    return conn
def create_db():
    if not os.path.exists(db_dir):
        os.makedirs(db_dir)

    conn = get_db()
    cur = conn.cursor()

    cur.execute("""
    CREATE TABLE IF NOT EXISTS tokens(
        username TEXT,
        token TEXT,
        emission_date TEXT,
        expire_date TEXT
    )
    """)

    conn.commit()
    conn.close()
def create_db_banned():
    conn = get_db_banned()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS banned(
                username TEXT,
                time TEXT,
                reason TEXT)""")
    conn.commit()
    conn.close()
create_db_banned()
create_db()




def verificar_hash(senha, hash_salvo):
    return check_password_hash(hash_salvo, senha)


def gerar_token():
    return secrets.token_hex(16)

@app.route("/new-session", methods=["POST"])
def new_session():
    data = request.get_json()
    username = data.get("username")
    password = data.get("password")
    
    if None in (username, password):
        return jsonify({"status":"the json is empty or is missing data"}),401
        
    conn = get_db_login()
    cur = conn.cursor()
    cur.execute("SELECT password FROM login WHERE username = ?",(username,))
    result = cur.fetchone()
    conn.close()
    
    if not result:
        return jsonify({"status": "user not found"}), 401
        
    if verificar_hash(password, result["password"]):
        emission_date = datetime.now()
        expire_date = emission_date + timedelta(hours=2)
        token = gerar_token()
        conn = get_db()
        cur = conn.cursor()
        cur.execute("INSERT INTO tokens(username, token, emission_date, expire_date) VALUES(?, ?, ?, ?)", (username, token, emission_date, expire_date))
        conn.commit()
        conn.close()
        return jsonify({"status":"the session has created", "token":token}),200
        
    return jsonify({"status": "wrong password"}), 401
public_routes = [
    "post.feed",
    "meta.return_version",
    "new_session",
    "search.search",
    "post.view_comments",
    "login.register",
    "login.login",
    "post.return_stars",
    "None",
    "profile.create",
    "images.upload_image",
    "profile.get_profile_pic",
    "post.view_post"
]
@app.before_request
def valide():
    if request.path in ["/receiveToken", "/sendToken", "/upload-image", "/view-post"]:
        return None

    token = request.headers.get("Authorization")

 
    if (request.endpoint in public_routes or request.method == "GET") and not token:
        g.username = None
        return None

    if token is None:
        return jsonify({"status": "the token is empty"}), 403
    token = token.replace("Bearer ", "").strip()

    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT username, expire_date, token FROM tokens WHERE token = ?", (token,))
    result = cur.fetchone()
    conn.close()

    if not result:
        return jsonify({"status": "invalid token"}), 403
    token_db = result["token"]
    g.username = result["username"]
    conn = get_db_banned()
    cur = conn.cursor()
    cur.execute("SELECT * FROM banned WHERE username = ?", (g.username,))
    result_user_banned = cur.fetchone()
    conn.close()
    if result_user_banned:
        json_banned = {
            "status": "BANNED",
            "reason": result_user_banned[2],
            "time": result_user_banned[1]
        }
        return jsonify(json_banned), 403
    if result["expire_date"]:
        expire_date = datetime.fromisoformat(result["expire_date"])
        if datetime.now() > expire_date:
            return jsonify({"status": "the token has been expired"}), 403
    return None
@app.route("/valide-session", methods=["POST"])
def valideManual():
    public_paths = ["/login", "/register", "/new-session", "/create-profile"]
    
    if request.path in public_paths:
        return None  

    token = request.headers.get("Authorization")
    
    if token == None:
        return jsonify({"status": "the token is empty"}), 403
    else:
        token = token.replace("Bearer ", "")
        
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT username, expire_date, token FROM tokens WHERE token = ?", (token,))
    result = cur.fetchone()
    
    if not result:
        conn.close()
        return jsonify({"status": "invalid token"}), 403
        
    token_db = result["token"]
    g.username = result["username"]
    expire_date = datetime.fromisoformat(result["expire_date"])
    
    if token_db == token:
        if datetime.now() > expire_date:
            return jsonify({"status": "the token has been expired"}), 403
        else:
            return jsonify({"status":"the token is valid"}),200
    else:
        return jsonify({"status": "the token is invalid"}), 403




CORS(app, resources={r"/*": {"origins": "*"}})
if root_flags["status"]:
    app.register_blueprint(status_bp)
if root_flags["search"]:
    app.register_blueprint(search_bp)
if root_flags["profiles"]:
    app.register_blueprint(profile_bp)
app.register_blueprint(login_bp)
if root_flags["post"]:
    app.register_blueprint(post_bp)
if root_flags["chat"]:
    app.register_blueprint(chat_bp)
if root_flags["sincronizer"]:
    app.register_blueprint(sincronizer_bp)
if root_flags["friends"]:
    app.register_blueprint(friends_bp)
app.register_blueprint(meta_bp)
app.register_blueprint(jobs_bp)
if root_flags["chat_global"]:
    app.register_blueprint(chat_global_bp)
if root_flags["notifications"]:
    app.register_blueprint(notifications_blueprint)
if root_flags["chat_group"]:
    app.register_blueprint(chat_group_bp)
if root_flags["images"]:
    app.register_blueprint(image_bp)
if root_flags["themes"]:
    app.register_blueprint(theme_bp)
if root_flags["federations"]:
    app.register_blueprint(federations_bp)
if root_flags["federation_index"]:
    app.register_blueprint(federation_index_bp)
if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5000)