import os
import smtplib
from email.mime.text import MIMEText
from email.mime.multipart import MIMEMultipart
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, request, jsonify
from werkzeug.security import generate_password_hash, check_password_hash
import dotenv

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
env_path = os.path.join(BASE_DIR, "backend.env")
if os.path.exists(env_path):
    dotenv.load_dotenv(env_path)

login_bp = Blueprint("login", __name__)

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def create_db():
    if not DATABASE_URL:
        return

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("""
            CREATE TABLE IF NOT EXISTS login (
                id SERIAL PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                email TEXT UNIQUE NOT NULL
            )
        """)
        cur.execute("""
            CREATE TABLE IF NOT EXISTS fast_login (
                id SERIAL PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                token TEXT NOT NULL
            )
        """)
        cur.execute("""
            CREATE TABLE IF NOT EXISTS profile (
                id SERIAL PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                bio TEXT,
                actual_badge TEXT,
                "ProfilePicture" TEXT,
                followers INTEGER DEFAULT 0,
                following INTEGER DEFAULT 0
            )
        """)
        cur.execute("""
            CREATE TABLE IF NOT EXISTS linkos_table (
                id SERIAL PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                linkos INTEGER DEFAULT 0
            )
        """)
        conn.commit()
    except Exception as e:
        conn.rollback()
        print(f"[ERRO create_db login]: {e}")
    finally:
        cur.close()
        conn.close()

def gerar_hash(senha):
    return generate_password_hash(senha)

def verificar_hash(senha, hash_salvo):
    return check_password_hash(hash_salvo, senha)

def enviar_email(destino, assunto, mensagem_html):
    remetente = "linka.plataform@gmail.com"
    senha_app = os.getenv("token_gmail")

    if not senha_app:
        print("[ERRO EMAIL] token_gmail não configurado no ambiente.")
        return

    try:
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
    except Exception as e:
        print(f"[ERRO EMAIL]: {e}")

@login_bp.route("/register", methods=["POST"])
def register():
    try:
        dados = request.get_json(silent=True) or request.form or {}
        if not dados:
            return jsonify({"status": "data is missing"}), 400

        username = dados.get("username")
        password = dados.get("senha") or dados.get("password")
        email = dados.get("email")
        invite_code = dados.get("invite_code")

        if not all([username, password, email]):
            return jsonify({"status": "data is missing"}), 400

        conn = get_db()
        cur = conn.cursor()
        try:
            # 1. Trata convite se fornecido
            if invite_code:
                try:
                    cur.execute(
                        "UPDATE invites SET status = 'USED' WHERE invite_code = %s AND status = 'NOT USED'",
                        (invite_code,)
                    )
                except Exception:
                    pass  # Ignora falha de convite para não bloquear o cadastro

            # 2. Verifica se o nome de usuário já existe
            cur.execute("SELECT username FROM login WHERE username = %s OR email = %s", (username, email))
            if cur.fetchone():
                return jsonify({"status": "username or email exists"}), 400

            # 3. Insere a conta no login
            senha_com_hash = gerar_hash(password)
            cur.execute(
                "INSERT INTO login (username, password, email) VALUES (%s, %s, %s)",
                (username, senha_com_hash, email)
            )

            # 4. Cria o perfil base
            biography = f"Hi! my name is {username}, let's be friends?"
            cur.execute(
                """
                INSERT INTO profile (username, bio) 
                VALUES (%s, %s) 
                ON CONFLICT (username) DO NOTHING
                """,
                (username, biography)
            )

            # 5. Inicializa a conta na tabela de linkos
            cur.execute(
                """
                INSERT INTO linkos_table (username, linkos)
                VALUES (%s, 0)
                ON CONFLICT (username) DO NOTHING
                """,
                (username,)
            )

            conn.commit()
            return jsonify({"status": "account created with success!"}), 200

        except Exception as e:
            conn.rollback()
            return jsonify({"status": "an error has occurred", "error": str(e)}), 500
        finally:
            cur.close()
            conn.close()

    except Exception as e:
        return jsonify({"status": "an error has occurred", "error": str(e)}), 500


@login_bp.route("/login", methods=["POST"])
def login():
    dados = request.get_json(silent=True) or {}
    username = dados.get("username")
    password = dados.get("senha") or dados.get("password")

    if not username or not password:
        return jsonify({"status": "username and password required"}), 400

    conn = get_db()
    cur = conn.cursor()
    try:
        # 1. Busca senha do usuário
        cur.execute("SELECT password FROM login WHERE username = %s", (username,))
        resultado = cur.fetchone()

        if not resultado:
            return jsonify({"status": "user not exists, please verify the username"}), 401

        if not verificar_hash(password, resultado["password"]):
            return jsonify({"status": "wrong password, check the password entry"}), 401

        # 2. Garante existência do perfil após o login bem-sucedido
        biography = f"Hi! my name is {username}, let's be friends?"
        cur.execute(
            """
            INSERT INTO profile (username, bio) 
            VALUES (%s, %s) 
            ON CONFLICT (username) DO NOTHING
            """,
            (username, biography)
        )
        conn.commit()

        return jsonify({"status": "login is sucessful"}), 200

    except Exception as e:
        conn.rollback()
        return jsonify({"status": "an error has occurred", "error": str(e)}), 500
    finally:
        cur.close()
        conn.close()