from flask import Flask, Blueprint, request, jsonify
import sqlite3
import os
from werkzeug.security import generate_password_hash, check_password_hash
import secrets
import jwt
import datetime
from datetime import timedelta
jwt_key = os.environ.get("LINKA_GATE_SECRET")
linkaGate_bp = Blueprint("linkaGate_bp", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
def get_db():
    conn = sqlite3.connect(db_dir + "/linkaGate.db")
    return conn
def get_db_session():
    conn = sqlite3.connect(db_dir + "/sessionToken.db")
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS linkaGate(
                username TEXT UNIQUE,
                global_id TEXT,
                password TEXT)""")
    conn.close()
    conn.commit()
    conn = get_db_session()
    cur = conn.cursor()
    cur.execute("""""
                CREATE TABLE IF NOT EXISTS sessionTokens(
                username TEXT,
                token TEXT,
                expire_at TEXT)""")
    conn.commit()
    conn.close()
create_db()
@linkaGate_bp.route("/register-gate",methods=["POST"])
def register_gate():
    data = request.get_json()
    username = data.get("username")
    password = data.get("password")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT username FROM linkaGate WHERE username = ?",(username,))
    result = cur.fetchone()
    conn.close()
    if result != None:
        return jsonify({"status":"username exists"}),401
    else:
        coded_password = generate_password_hash(password)
        global_id = secrets.token_hex(16)
        cur.execute("INSERT INTO linkaGate (username, password, global_id) VALUES(?, ?, ?)",(username, coded_password, global_id))
        conn.commit()
        conn.close()
        return jsonify({"status":"account created with sucess on the LinkaGate!"}),200
@linkaGate_bp.route("/login-gate",methods=["POST"])
def login():
    data = request.get_json()
    username = data.get("username")
    password = data.get("password")
    session_token = data.get("session_token")
    conn = get_db_session()
    cur = conn.cursor()
    cur.execute("SELECT token FROM sessionToken WHERE token = ? AND expired_at > ?",(session_token, datetime.datetime.utcnow()))
    result = cur.fetchone()
    conn.close()
    if session_token == None or result == None:
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT * FROM linkaGate WHERE username = ?",(username,))
        result = cur.fetchone()
        conn.close()
        if result == None:
            return jsonify({"status":"the account dont exists"}),401
        if check_password_hash(result[2], password):
            new_token = secrets.token_hex(32)
            conn = get_db_session()
            cur = conn.cursor()
            expired_at = datetime.datetime.utcnow() + timedelta(hours=2)
            cur.execute("INSERT INTO sessionToken (username, token, expired_at) VALUES (?, ?, ?)",(username, new_token, expired_at))
            return jsonify({"status":"you logged with sucess!", "token":new_token}),200
        else:
            return jsonify({"status":"username or password incorrect!"}),401
    else:
        return jsonify({"status":"logged with sucess!"}),200