from flask import Flask, Blueprint, request, jsonify
import sqlite3
import os
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
        CREATE TABLE moa IF NOT EXISTS(
                username TEXT,
                global_id TEXT,
                password TEXT)""")
    conn.commit()
    conn.close()
create_db()