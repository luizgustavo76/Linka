from flask import Flask, Blueprint, request, jsonify
import sqlite3
import os
from datetime import datetime
store_bp = Blueprint("store_bp", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = (base_dir + "/DB")
def get_db():
    conn = sqlite3.connect(db_dir + "/index.db")
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""CREATE TABLE IF NOT EXISTS index(
                author TEXT,
                name TEXT,
                description TEXT,
                timestamp TEXT,
                repository TEXT)""")
    conn.commit()
    conn.close()
create_db()