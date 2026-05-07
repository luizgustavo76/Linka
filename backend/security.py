from flask import Flask, request, jsonify, Blueprint,g
import sqlite3
import os
from datetime import datetime, timedelta
from werkzeug.security import generate_password_hash, check_password_hash
import secrets
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = (base_dir + "/DB")
tokens_file = (db_dir + "/tokens.db")
security_app = Blueprint("security", __name__)
def get_db():
    conn = sqlite3.connect(tokens_file)
    return conn
def get_db_login():
    conn = sqlite3.connect("DB/login.db")
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""CREATE TABLE IF NOT EXISTS tokens(
                username TEXT UNIQUE,
                token TEXT,
                emission_date TEXT,
                expire_date TEXT)""")
    conn.commit()
    conn.close()
create_db()
def verificar_hash(senha, hash):
    return check_password_hash(hash, senha)
@security_app.route("/new-session", methods=["POST"])
def new_session():
    data = request.get_json()
    username = data.get("username")
    password = data.get("password")
    conn = get_db_login()
    cur = conn.cursor()
    cur.execute("SELECT username FROM login WHERE username = ?", (username,))
    #resultado de usernames, se existir aquele username ele retorna o nome se não retorna None
    resultado_username = cur.fetchone()
    conn.close()
    if not resultado_username:
        return jsonify({"status":"usuário não existe, verifique o campo de usuário"}), 401
    if resultado_username:
        conn = get_db_login()
        cur = conn.cursor()
        cur.execute("SELECT senha FROM login WHERE username =?", (username,))
        resultado_senha = cur.fetchone()
        hash_salvo = resultado_senha[0]
        senha_descodificada = verificar_hash(password, hash_salvo)
        conn.close()
        if senha_descodificada:
            token = secrets.token_hex(16)
            date = datetime.now()
            expire_date = date + timedelta(hours=2)
            conn = get_db()
            cur = conn.cursor()
            cur.execute("INSERT INTO tokens (username, token, emission_date, expire_date) VALUES(?, ?, ?, ?)", (username, token, date, expire_date))
            conn.commit()
            conn.close()
            return jsonify({"status":"session is sucessful", "token":token}), 200
        else:
            return jsonify({"status": "wrong password, check the password entry"}), 401
@secure_app.route("/valide", methods=["POST"])
def valide():
    data = request.get_json()
    token = data.get("token")
    username = data.get("username")
    conn = get_db()
    cur = conn.cursor()     
    cur.execute("SELECT token, username FROM tokens WHERE token = ?", (token,))
    result = cur.fetchone()
    if token = result[0]:
        g.user = result[1]
        cur.execute("DELETE token,emission_date, expire_date FROM tokens WHERE token = ?", (token,))
        conn.commit()
        conn.close()
        token_hex = secrets.token_hex(16)
        emission_date = datetime.now()
        expire_date = emission_date + timedelta(hours=2)
        cur.execute("INSERT INTO friends (token, emission_date, expire_date), VALUES (?,?,?) WHERE username = ?",(token_hex, emission_date, expire_date, username))
        conn.commit()
        conn.close()
        return jsonify({"status":"token is valid", "token":token_hex}), 200
    else:
        return jsonify({"status":"the token is invalid or expired, please make the login again"}), 403
