from flask import Flask, request, jsonify,Blueprint, redirect, send_from_directory, abort, render_template, g
import sqlite3
import os
from werkzeug.security import generate_password_hash, check_password_hash
import secrets
from dotenv import load_dotenv
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
login_bp = Blueprint("login", __name__)
load_dotenv("backend.env")
class Login:
    def __init__(self):
        self.base_dir = os.path.dirname(os.path.abspath(__file__))
        self.db_dir = (self.base_dir + "/DB")
        self.login_dir = os.path.join(self.db_dir, "login.db")
        self.fast_login = os.path.join(self.db_dir, "FastLogin.db")
        self.senha_hash = None
        
        if not os.path.exists(self.db_dir):
            os.makedirs(self.db_dir)
        def criar_db_login():
            conn = self.get_db_login()
            cur = conn.cursor()
            cur.execute("""
                CREATE TABLE IF NOT EXISTS login(
                        username TEXT UNIQUE,
                        password TEXT,
                        email TEXT UNIQUE,
                        id TEXT UNIQUE
                    )
            """)
            cur.execute("""
                CREATE TABLE IF NOT EXISTS linkos_table(
                    username TEXT,
                    linkos INTEGER
                )
            """)
            conn.commit()
            conn.close()
            conn = self.get_db_fastlogin()
            cur = conn.cursor()
            cur.execute("""
                CREATE TABLE IF NOT EXISTS FastLogin(
                    username TEXT UNIQUE,
                    token TEXT
                )
                """)
            conn.commit()
            conn.close()
        criar_db_login()
    def get_db_login(self):
        conn = sqlite3.connect(self.login_dir, timeout=15)
        conn.execute("PRAGMA journal_mode=WAL;")
        cursor = conn.cursor()
        conn.execute("PRAGMA journal_mode=WAL;")
        conn.execute("PRAGMA synchronous=NORMAL;")
        conn.execute("PRAGMA cache_size=-10000;")

        return conn
    def get_db_fastlogin(self):
        conn = sqlite3.connect(self.fast_login, timeout=15)
        conn.execute("PRAGMA journal_mode=WAL;")
        return conn
    def gerar_hash(self, senha):
        self.senha_hash = generate_password_hash(senha)
        return self.senha_hash
    def verificar_hash(self, senha, hash):
        return check_password_hash(hash, senha)
    def get_db_invites(self):
        conn = sqlite3.connect(self.db_dir + "/invite.db")
        return conn
login_system = Login()

def enviar_email(destino, assunto, mensagem_html):
    remetente = "linka.plataform@gmail.com"
    senha_app = os.getenv("token_gmail")

    msg = MIMEMultipart()
    msg["From"] = remetente
    msg["To"] = destino
    msg["Subject"] = assunto

    msg.attach(MIMEText(mensagem_html, "html"))

    servidor = smtplib.SMTP("smtp.gmail.com", 587)
    servidor.starttls()
    servidor.login(remetente, senha_app)
    servidor.sendmail(remetente, destino, msg.as_string())
    servidor.quit()

    print("Email enviado com sucesso!")

@login_bp.route("/register", methods=["POST"])
def register():
    try:
        dados = request.get_json()
        if dados == None:
            return jsonify({"data is missing"}),401
        username = dados.get("username")
        password = dados.get("senha") or dados.get("password")
        email = dados.get("email")
        invite_code = dados.get("invite_code")
        conn = login_system.get_db_login()
        cur = conn.cursor()
        cur.execute("SELECT username FROM login WHERE username = ?", (username,))
        resultado = cur.fetchone()
        conn.close()
        if resultado:
            return jsonify({"status":"username exists"}),400
        if not username or not password or not email or not invite_code:
            return jsonify({"status":"data is missing"}),401
        else:
            senha_com_hash = login_system.gerar_hash(password)
            conn = login_system.get_db_login()
            cur = conn.cursor()
            conn_invite = login_system.get_db_invites()
            cur_invite = conn_invite.cursor()
            cur_invite.execute("SELECT invite_code FROM invites WHERE invite_code = ? AND status = 'NOT USED'")
            result = cur_invite.fetchone()
            conn_invite.close()
            if result:
                cur.execute("""
                    INSERT INTO login (username, password, email) VALUES (?, ?, ?)""",(username, senha_com_hash, email))
                conn.commit()
                conn.close()
                return jsonify({"status":"account created with sucess!"}), 201
            else:
                conn.close()
                return jsonify({"status":"the invite code is invalid"}),401
            
    except Exception as e:
        return jsonify({"status": "an error has occurred", "error": str(e)}),500
@login_bp.route("/create-fast-login", methods=["POST"])
def create_fast_login():
    data = request.get_json()
    username = data.get("username")
    password = data.get("password")
    conn = login_system.get_db_login()
    cur = conn.cursor()
    cur.execute("SELECT * FROM login WHERE username = ?", (username,))
    result = cur.fetchone()
    conn.close()
    hash = result[1]
    if not login_system.verificar_hash(password, hash):
        return jsonify({"status":"the username or passowrd s incorret"}),401
    else:
        conn = login_system.get_db_fastlogin()
        cur = conn.cursor()
        token = secrets.token_hex(32)
        cur.execute("INSERT INTO Fastlogin (username, token) VALUES (?, ?)", (username, token))
        conn.commit()
        conn.close()
        return jsonify({"status":"the session has been created", "token":token}),200
@login_bp.route("/fast-login", methods=["POST"])
def fast_login():
    data = request.get_json()
    username = data.get("username")
    token = data.get("token")
    conn = login_system.get_db_fastlogin()
    cur = conn.cursor()
    cur.execute("SELECT * FROM Fastlogin WHERE username = ? AND token = ?", (username, token))
    result = cur.fetchone()
    conn.close()
    try:
        row = {
            "username":result[0],
            "token":result[1]
        }
        if None in (row["username"], row["token"]):
            return jsonify({"status":"user dont have a session"}),401
        else:
            return jsonify({"status":"logged in with sucess"}),200
    except:
        return jsonify({"status":"user dont have a session"}),401
@login_bp.route("/login", methods=["POST"])
def login():
    dados = request.get_json()
    username = dados.get("username")
    password = dados.get("senha") or dados.get("password")
    conn = login_system.get_db_login()
    cur = conn.cursor()
    cur.execute("SELECT username FROM login WHERE username = ?", (username,))
    resultado_username = cur.fetchone()
    conn.close()
    if not resultado_username:
        return jsonify({"status":"user not exists, please veriy the username"}), 401
    if resultado_username:
        conn = login_system.get_db_login()
        cur = conn.cursor()
        cur.execute("SELECT password FROM login WHERE username =?", (username,))
        resultado_senha = cur.fetchone()
        hash_salvo = resultado_senha[0]
        senha_descodificada = login_system.verificar_hash(password, hash_salvo)
        conn.close()
        if senha_descodificada:
            return jsonify({"status":"login is sucessful"}), 200
        else:
            return jsonify({"status": "wrong password, check the password entry"}), 401
        
