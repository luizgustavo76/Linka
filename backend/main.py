from flask_cors import CORS 
from post import post_bp
from chat import chat_bp
from friends import friends_bp
from login import login_bp
from profiles import profile_bp
from search import search_bp
from community import community_bp

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

create_db()


# =========================
# FUNÇÕES
# =========================

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
    cur.execute("SELECT senha FROM login WHERE username = ?",(username,))
    result = cur.fetchone()
    conn.close()
    if verificar_hash(password, result["senha"]):
        emission_date = datetime.now()
        expire_date = emission_date + timedelta(hours=2)
        token = gerar_token()
        conn = get_db()
        cur = conn.cursor()
        cur.execute("INSERT INTO tokens(username, token, emission_date, expire_date) VALUES(?, ?, ?, ?)", (username, token, emission_date, expire_date))
        conn.commit()
        conn.close()
        return jsonify({"status":"the session has created", "token":token}),200

    else:
        return jsonify({"status":"the password is incorret"}),401
public_routes = [
    "post.feed",
    "post.new_post",
    "new_session",
    "search.search",
    "profile.view_profile",
    "post.view_comments",
    "login.register",
    "login.login",
    "post.return_stars",
    "None"
]
@app.before_request
def valide():
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
    username = result["username"]
    expire_date = result["expire_date"]
    expire_date = datetime.fromisoformat(expire_date)
    if token_db == token:
        if datetime.now() > expire_date:
            return jsonify({"status":"the token has been expired"}),401
        else:
            new_token = gerar_token()
            emission_date = datetime.now()
            new_expire_date = emission_date + timedelta(hours=2)
            cur.execute("""UPDATE tokens
SET token = ?, emission_date = ?, expire_date = ?
WHERE username = ?""",(new_token, emission_date, new_expire_date, username))
            conn.commit()
            conn.close()
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


if __name__ == "__main__":
    app.run(debug=True, port=5000)