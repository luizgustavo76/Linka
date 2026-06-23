from flask import Flask, Blueprint, request, jsonify
import sqlite3
import os
from werkzeug.security import generate_password_hash, check_password_hash
import secrets
linkaGate_bp = Blueprint("linkaGate_bp", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
def get_db():
    conn = sqlite3.connect(db_dir + "/linkaGate.db")
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE linkaGate IF NOT EXISTS(
                username TEXT UNIQUE,
                global_id TEXT,
                password TEXT)""")
    conn.commit()
    conn.close()
create_db()
@linkaGate_bp.route("/api/register-gate")
def register_gate():
    data = request.get_json()
    username = data.get("username")
    password = data.get("password")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT username FROM linkaGate WHERE username = ?",(username,))
    result = cur.fetchone()
    if result != None:
        return jsonify({"status":"username exists"}),401
    else:
        coded_password = generate_password_hash(password)
        global_id = secrets.token_hex(16)
        cur.execute("INSERT INTO linkaGate (username, password, global_id) VALUES(?, ?, ?)",(username, coded_password, global_id))
        conn.commit()
        conn.close()
        return jsonify({"status":"account created with sucess on the LinkaGate!"}),200