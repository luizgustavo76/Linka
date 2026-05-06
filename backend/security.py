from flask import Flask, request, jsonify, Blueprint
import sqlite3
import os
from werkzeug.security import generate_password_hash, check_password_hash
import secrets
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = (base_dir + "/DB")
tokens_file = (db_dir + "/tokens.db")
security_app = Blueprint("security", __name__)
def get_db():
    conn = sqlite3.connect(tokens_file)
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