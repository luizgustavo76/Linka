import os
import secrets
import datetime
from datetime import timedelta
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, request, jsonify
from werkzeug.security import generate_password_hash, check_password_hash

linkaGate_bp = Blueprint("linkaGate_bp", __name__)

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def create_db():
    conn = get_db()
    cur = conn.cursor()
    
    cur.execute("""
        CREATE TABLE IF NOT EXISTS linka_gate (
            id SERIAL PRIMARY KEY,
            username TEXT UNIQUE NOT NULL,
            global_id TEXT NOT NULL,
            password TEXT NOT NULL
        )
    """)
    
    cur.execute("""
        CREATE TABLE IF NOT EXISTS session_tokens (
            id SERIAL PRIMARY KEY,
            username TEXT NOT NULL,
            token TEXT UNIQUE NOT NULL,
            expired_at TIMESTAMP NOT NULL
        )
    """)
    
    conn.commit()
    cur.close()
    conn.close()

@linkaGate_bp.route("/register-gate", methods=["POST"])
def register_gate():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    password = data.get("password")

    if not username or not password:
        return jsonify({"status": "username and password are required"}), 400

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("SELECT username FROM linka_gate WHERE username = %s", (username,))
        result = cur.fetchone()

        if result is not None:
            return jsonify({"status": "username exists"}), 401

        coded_password = generate_password_hash(password)
        global_id = secrets.token_hex(16)
        
        cur.execute(
            "INSERT INTO linka_gate (username, password, global_id) VALUES (%s, %s, %s)",
            (username, coded_password, global_id)
        )
        conn.commit()
        return jsonify({"status": "account created with sucess on the LinkaGate!"}), 200
    except Exception as e:
        conn.rollback()
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        cur.close()
        conn.close()

@linkaGate_bp.route("/login-gate", methods=["POST"])
def login():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    password = data.get("password")
    session_token = data.get("session_token")

    conn = get_db()
    cur = conn.cursor()
    try:
        # 1. Verifica se já existe um token de sessão válido no Postgres
        if session_token:
            cur.execute(
                "SELECT token FROM session_tokens WHERE token = %s AND expired_at > %s",
                (session_token, datetime.datetime.utcnow())
            )
            token_valid = cur.fetchone()
            if token_valid:
                return jsonify({"status": "logged with sucess!"}), 200

        # 2. Se não houver token válido, tenta autenticar via usuário e senha
        if not username or not password:
            return jsonify({"status": "username or password missing"}), 400

        cur.execute("SELECT username, password FROM linka_gate WHERE username = %s", (username,))
        user_data = cur.fetchone()

        if user_data is None:
            return jsonify({"status": "the account dont exists"}), 401

        if check_password_hash(user_data["password"], password):
            new_token = secrets.token_hex(32)
            expired_at = datetime.datetime.utcnow() + timedelta(hours=2)

            cur.execute(
                "INSERT INTO session_tokens (username, token, expired_at) VALUES (%s, %s, %s)",
                (username, new_token, expired_at)
            )
            conn.commit()
            return jsonify({"status": "you logged with sucess!", "token": new_token}), 200
        else:
            return jsonify({"status": "username or password incorrect!"}), 401

    except Exception as e:
        conn.rollback()
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        cur.close()
        conn.close()