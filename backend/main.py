from flask_cors import CORS 
from post import post_bp
from chat import chat_bp
from friends import friends_bp
from login import login_bp
from profiles import profile_bp
from search import search_bp
from community import community_bp
from meta import meta_bp
from chat_global import chat_global_bp
from notifications import notifications_blueprint
from flask import Flask, Blueprint, request, jsonify, g
import sqlite3
import os
from datetime import datetime, timedelta
from werkzeug.security import check_password_hash
import secrets
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
tokens_file = os.path.join(db_dir, "tokens.db")
login_file = os.path.join(db_dir, "login.db")
banned_file = os.path.join(db_dir, "banned.db")
app = Flask(__name__)
# =========================
# BANCO DE DADOS
# =========================
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
        CREATE TABLE IF NOT EXISTS banned
                username TEXT,
                time TEXT,
                reason TEXT""")
    conn.commit()
    conn.close()
create_db_banned()
create_db()


# =========================
# FUNÇÕES
# =========================

def verificar_hash(senha, hash_salvo):
    return check_password_hash(hash_salvo, senha)


def gerar_token():
    return secrets.token_hex(16)


public_username_requets = [
    "login.register",
    "login.login"
]
@app.before_request
def valide_user():
    data = request.get_json()
    username_keys = ["username", "user1", "user2", "receiver", "remitte", "sender"]
    actual_key = ""
    if request.endpoint not in public_username_requets:
        for key in username_keys:
            if key in data:
                actual_key = data[key]
        if actual_key != "":
            conn = get_db_login()
            cur = conn.cursor()
            cur.execute("SELECT * FROM login WHERE username = ?", (actual_key,))
            result = cur.fetchone()
            conn.close()
            if result != None:
                pass
            else:
                return jsonify({"status":"username not exists"}),400
    else:
        pass
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
    "profile.view_profile",
    "post.view_comments",
    "login.register",
    "login.login",
    "post.return_stars",
    "None",
    "profiles.create"
]
@app.before_request
def valide():
    data = request.get_json()
    username = data.get("username")
    if username in data:
        conn = get_db_banned()
        cur = conn.cursor()
        cur.execute("SELECT * FROM banned WHERE username = ?",(username))
        result = cur.fetchone()
        if result:
            json_banned = {
                "status":"banned",
                "reason":result[2],
                "time":result[1]
            }
            return jsonify(json_banned),403
    token = request.headers.get("Authorization")
    print(request.endpoint)
    if request.endpoint in public_routes:
        return None
    if token == None:
        return jsonify({"status":"the token is empty"}),401
    else:
        token = token.replace("Bearer ", "")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT username, expire_date, token FROM tokens WHERE token = ?", (token,))
    result = cur.fetchone()
    if not result:
        conn.close()
        return jsonify({"status": "invalid token"}), 401
    token_db = result["token"]
    g.username = username = result["username"]
    conn = get_db_banned()
    cur = conn.cursor()
    cur.execute("SELECT * FROM banned WHERE username = ?",(result["username"]))
    result = cur.fetchone()
    if result:
        json_banned = {
            "status":"BANNED",
            "reason":result[2],
            "time":result[1]
        }
        return jsonify(json_banned),403
    expire_date = result["expire_date"]
    expire_date = datetime.fromisoformat(expire_date)
    if token_db == token:
        if datetime.now() > expire_date:
            return jsonify({"status":"the token has been expired"}),401
        else:
            return None
    else:
        return jsonify({"status":"the token is invalid"}),401
@app.route("/valide-session", methods=["POST"])
def valideManual():
    token = request.headers.get("Authorization")
    print(request.endpoint)
    if request.endpoint in public_routes:
        return None
    if token == None:
        return jsonify({"status":"the token is empty"}),401
    else:
        token = token.replace("Bearer ", "")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT username, expire_date, token FROM tokens WHERE token = ?", (token,))
    result = cur.fetchone()
    if not result:
        conn.close()
        return jsonify({"status": "invalid token"}), 401
    token_db = result["token"]
    g.username = username = result["username"]
    expire_date = result["expire_date"]
    expire_date = datetime.fromisoformat(expire_date)
    if token_db == token:
        if datetime.now() > expire_date:
            return jsonify({"status":"the token has been expired"}),401
        else:
            return None
    else:
        return jsonify({"status":"the token is invalid"}),401




CORS(app, resources={r"/*": {"origins": "*"}})
app.register_blueprint(community_bp)
app.register_blueprint(search_bp)
app.register_blueprint(profile_bp)
app.register_blueprint(login_bp)
app.register_blueprint(post_bp)
app.register_blueprint(chat_bp)
app.register_blueprint(friends_bp)
app.register_blueprint(meta_bp)
app.register_blueprint(chat_global_bp)
app.register_blueprint(notifications_blueprint)
if __name__ == "__main__":
    app.run(debug=True, port=5000)