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
    def get_db_profiles(self):
        conn = sqlite3.connect(self.db_dir + "/profile.db")
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
        if not dados:
            return jsonify({"status": "data is missing"}), 400

        username = dados.get("username")
        password = dados.get("senha") or dados.get("password")
        email = dados.get("email")
        invite_code = dados.get("invite_code")

        if not all([username, password, email, invite_code]):
            return jsonify({"status": "data is missing"}), 400

        with login_system.get_db_invites() as conn_invite:
            cur_invite = conn_invite.cursor()
            cur_invite.execute(
                "SELECT invite_code FROM invites WHERE invite_code = ? AND status = 'NOT USED'",
                (invite_code,)
            )
            if not cur_invite.fetchone():
                return jsonify({"status": "the invite code is invalid"}), 401

        with login_system.get_db_login() as conn_login:
            cur_login = conn_login.cursor()
            cur_login.execute("SELECT username FROM login WHERE username = ?", (username,))
            if cur_login.fetchone():
                return jsonify({"status": "username exists"}), 400

            senha_com_hash = login_system.gerar_hash(password)
            cur_login.execute(
                "INSERT INTO login (username, password, email) VALUES (?, ?, ?)",
                (username, senha_com_hash, email)
            )
            conn_login.commit()

        with login_system.get_db_profiles() as conn_profile:
            cur_profile = conn_profile.cursor()
            cur_profile.execute("SELECT username FROM profile WHERE username = ?", (username,))
            if not cur_profile.fetchone():
                biography = f"Hi! my name is {username}, let's be friends?"
                cur_profile.execute(
                    "INSERT INTO profile (username, bio) VALUES (?, ?)",
                    (username, biography)
                )
                conn_profile.commit()


        return jsonify({"status": "account created with success!"}), 201
    except Exception as e:
        return jsonify({"status": "an error has occurred", "error": str(e)}), 500
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
    conn_profile = login_system.get_db_profiles()
    cur_profile = conn_profile.cursor()
    cur_profile.execute("SELECT username FROM profile WHERE username = ?",(username,))
    result_profile = cur_profile.fetchone()
    if result_profile:
        pass
    else:
        biography = f"Hi! my name is {username}, let's be friends?"
        cur_profile.execute(
            "INSERT INTO profile (username, bio) VALUES (?, ?)",
            (username, biography)
        )
        conn_profile.commit()
    if not resultado_username:
        return jsonify({"status":"user not exists, please verify the username"}), 401
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
        
