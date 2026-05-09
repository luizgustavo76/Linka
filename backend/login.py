from flask import Flask, request, jsonify,Blueprint, redirect, send_from_directory, abort, render_template, g
import sqlite3
import os
from werkzeug.security import generate_password_hash, check_password_hash
import secrets

login_bp = Blueprint("login", __name__)
class Login:
    def __init__(self):
        #pasta atual
        self.base_dir = os.path.dirname(os.path.abspath(__file__))
        #pasta onde está os banco de dados
        self.db_dir = (self.base_dir + "/DB")
        #banco de dados de login
        self.login_dir = os.path.join(self.db_dir, "login.db")
        self.fast_login = os.path.join(self.db_dir, "FastLogin.db")
        #senha em hash
        self.senha_hash = None
        
        if not os.path.exists(self.db_dir):
            os.makedirs(self.db_dir)
        #criar banco se não existir
        def criar_db_login():
            conn = self.get_db_login()
            cur = conn.cursor()
            cur.execute("""
                CREATE TABLE IF NOT EXISTS login(
                        username TEXT UNIQUE,
                        senha TEXT,
                        email TEXT UNIQUE,
                        id TEXT UNIQUE
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
        conn = sqlite3.connect(self.login_dir)
        return conn
    def get_db_fastlogin(self):
        conn = sqlite3.connect(self.fast_login)
        return conn    
    #transformar senha em texto para hash
    def gerar_hash(self, senha):
        self.senha_hash = generate_password_hash(senha)
        return self.senha_hash
    #verificar se a hash com a senha
    def verificar_hash(self, senha, hash):
        return check_password_hash(hash, senha)
login_system = Login()
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart

def enviar_email(destino, assunto, mensagem_html):
    remetente = "linka.plataform@gmail.com"
    senha_app = "gthv xlyw cbyg raft"

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

#rota pra criar token de sessão
login_bp.route("/session", methods= ["POST"])
def session():
    pass
#rota pra registrar usuarios
@login_bp.route("/register", methods=["POST"])
def register():
    try:
        dados = request.get_json()
        print("DADOS RECEBIDOS:", dados)
        username = dados.get("username")
        password = dados.get("senha") or dados.get("password")
        email = dados.get("email")
        conn = login_system.get_db_login()
        cur = conn.cursor()
        cur.execute("SELECT username FROM login WHERE username = ?", (username,))
        resultado = cur.fetchone()
        conn.close()
        if resultado:
            return jsonify({"status":"username já existe"}),400
        else:
            senha_com_hash = login_system.gerar_hash(password)
            conn = login_system.get_db_login()
            cur = conn.cursor()
            cur.execute("""
                INSERT INTO login (username, senha, email) VALUES (?, ?, ?)""",(username, senha_com_hash, email))
            conn.commit()
            conn.close()
            html_register = f"""
            <!DOCTYPE html>
            <html>
            <head>
            <meta charset="UTF-8">
            <title>Welcome to Linka</title>
            </head>
            <body style="margin:0; padding:0; background-color:#0b0b0f; font-family:Arial, sans-serif;">

            <table width="100%" cellpadding="0" cellspacing="0" style="background-color:#0b0b0f; padding:30px 0;">
                <tr>
                <td align="center">

                    <table width="600" cellpadding="0" cellspacing="0" style="
                    background-color:#14141c;
                    border-radius:18px;
                    overflow:hidden;
                    box-shadow:0px 0px 20px rgba(0,0,0,0.5);
                    ">

                    <!-- HEADER -->
                    <tr>
                        <td style="
                        background: linear-gradient(90deg, #00ffcc, #00aaff);
                        padding:20px;
                        text-align:center;
                        color:#0b0b0f;
                        font-size:26px;
                        font-weight:bold;
                        ">
                        🚀 Welcome to Linka
                        </td>
                    </tr>

                    
                    <!-- CONTENT -->
                    <tr>
                        <td style="padding:30px; color:#ffffff;">

                        <h2 style="margin-top:0; font-size:22px; color:#00ffcc;">
                            Hello, {username} 👋
                        </h2>

                        <p style="font-size:15px; line-height:1.6; color:#dcdcdc;">
                            Welcome to <b>Linka</b> — a social network built for people who value freedom.
                            Here, you are not just a user...
                            <b>you are part of the system.</b>
                        </p>

                        <p style="font-size:15px; line-height:1.6; color:#dcdcdc;">
                            Your account has been successfully created, and now you can:
                        </p>

                        <ul style="color:#dcdcdc; font-size:15px; line-height:1.7; padding-left:18px;">
                            <li>📝 Create posts and share your ideas</li>
                            <li>💬 Chat privately with other users</li>
                            <li>👥 Make friends and build connections</li>
                            <li>🌍 Join servers and federations</li>
                        </ul>

                        <div style="
                            margin:30px 0;
                            padding:18px;
                            border:1px solid #2b2b35;
                            border-radius:14px;
                            background-color:#101018;
                        ">
                            <p style="margin:0; font-size:14px; color:#aaaaaa;">
                            🔥 Quick tip:
                            </p>
                            <p style="margin:8px 0 0 0; font-size:15px; color:#ffffff;">
                            Try exploring federations and see how Linka connects different worlds together.
                            </p>
                        </div>

                        <p style="font-size:15px; line-height:1.6; color:#dcdcdc;">
                            If you did not create this account, please ignore this message.
                            But if you did...
                            then get ready:
                            <b>Linka is now your territory.</b>
                        </p>

                        <!-- BUTTON -->
                        <div style="text-align:center; margin-top:35px;">
                            <a href="{{link}}" style="
                            display:inline-block;
                            padding:14px 26px;
                            background: linear-gradient(90deg, #00ffcc, #00aaff);
                            color:#0b0b0f;
                            text-decoration:none;
                            font-weight:bold;
                            border-radius:12px;
                            font-size:16px;
                            ">
                            Enter Linka
                            </a>
                        </div>

                        </td>
                    </tr>

                    <!-- FOOTER -->
                    <tr>
                        <td style="
                        padding:18px;
                        text-align:center;
                        background-color:#0e0e14;
                        color:#777777;
                        font-size:12px;
                        ">
                        © 2026 Linka Project — Developed by Luiz Gustavo<br>
                        <span style="color:#444;">Automated message • Please do not reply</span>
                        </td>
                    </tr>

                    </table>

                </td>
                </tr>
            </table>

            </body>
            </html>
            """
            enviar_email(destino=email, assunto="Linka Login", mensagem_html=html_register)
            return jsonify({"status":"conta criada com sucesso!"}), 201
    except:
        return jsonify({"status":"a error as ocurred"})
#route of main page
@login_bp.route("/")
def main():
    return render_template("index.html") 
#route of the fast login
@login_bp.route("/fast-login", methods=["POST"])
def FastLogin():
    data = request.get_json()
    username = data.get("username")
    token = data.get("token")

    def generate_token(length=32):
        return secrets.token_hex(length)

    if not token:
        new_token = generate_token()
        conn = login_system.get_db_fastlogin()
        cur = conn.cursor()
        cur.execute("INSERT INTO FastLogin (username, token) VALUES (?, ?)", (username, new_token))
        conn.commit()
        conn.close()
        return jsonify({"status":"fastlogin created", "token": new_token}), 201

    if token:
        conn = login_system.get_db_fastlogin()
        cur = conn.cursor()
        cur.execute("SELECT token FROM FastLogin WHERE username = ?", (username,))
        get_token = cur.fetchone()

        if not get_token:
            conn.close()
            return jsonify({"status":"user not found"}), 404

        saved_token = get_token[0]

        if token == saved_token:
            new_token = generate_token()
            cur.execute("UPDATE FastLogin SET token = ? WHERE username = ?", (new_token, username))
            conn.commit()
            conn.close()
            return jsonify({"status":"fastlogin success", "token": new_token}), 200
        else:
            conn.close()
            return jsonify({"status":"the token is invalid"}), 401
        
#login route
@login_bp.route("/login", methods=["POST"])
def login():
    dados = request.get_json()
    oauth = dados.get("oauth")
    if not oauth:
        username = dados.get("username")
        password = dados.get("senha") or dados.get("password")
        conn = login_system.get_db_login()
        cur = conn.cursor()
        cur.execute("SELECT username FROM login WHERE username = ?", (username,))
        #resultado de usernames, se existir aquele username ele retorna o nome se não retorna None
        resultado_username = cur.fetchone()
        conn.close()
        if not resultado_username:
            return jsonify({"status":"usuário não existe, verifique o campo de usuário"}), 401
        if resultado_username:
            conn = login_system.get_db_login()
            cur = conn.cursor()
            cur.execute("SELECT senha FROM login WHERE username =?", (username,))
            resultado_senha = cur.fetchone()
            hash_salvo = resultado_senha[0]
            senha_descodificada = login_system.verificar_hash(password, hash_salvo)
            conn.close()
            if senha_descodificada:
                return jsonify({"status":"login is sucessful"}), 200
            else:
                return jsonify({"status": "wrong password, check the password entry"}), 401
    if oauth:
        username = dados.get("username")
        avatar_id = dados.get("avatar_id")
        id = dados.get("id")
        conn = login_system.get_db_login()
        cur = conn.cursor()
        cur.execute("SELECT username FROM login WHERE username = ?",(username,))
        result_user = cur.fetchone()
        conn.close()
        if not result_user:
            conn = login_system.get_db_login()
            cur = conn.cursor()
            cur.execute("INSERT INTO login (username, id) VALUES (?,?)",(username, id))
            conn.commit()
            conn.close()
            return jsonify({"status":"account oauth is created"}),200
        if result_user:
            return jsonify({"status":"login is sucessful"}),200
        
