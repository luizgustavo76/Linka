from flask import Flask, request, jsonify, Blueprint
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
    cur.execute("""CREATE TABLE IF NOT EXISTS tokens
                username TEXT UNIQUE,
                token TEXT,
                emission_date TEXT,
                expire_date TEXT""")
    conn.commit()
    conn.close()
create_db()
def verificar_hash(self, senha, hash):
    return check_password_hash(hash, senha)
security_app.route("/new-session", methods=["post"])
def new_session():
    data = request.get()
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
            date = datetime.datetime.now()
            expire_date = date + timedelta(hours=2)
            conn = get_db()
            cur = conn.cursor()
            cur.execute("INSERT INTO tokens (username, token, date)")
            return jsonify({"status":"login is sucessful"}), 200
        else:
            return jsonify({"status": "wrong password, check the password entry"}), 401