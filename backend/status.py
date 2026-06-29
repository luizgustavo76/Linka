from flask import Flask, request, jsonify, Blueprint, g
import sqlite3
import os
from datetime import datetime
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
status_db = os.path.join(db_dir, "status.db")
status_bp = Blueprint("status", __name__)
def get_db():
    conn = sqlite3.connect(status_db)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA synchronous=NORMAL;")
    conn.execute("PRAGMA cache_size=-10000;")
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""CREATE TABLE IF NOT EXISTS status(
                username TEXT UNIQUE,
                timestamp TEXT,
                status TEXT,
                action TEXT)""")
    conn.commit()
    conn.cursor()
@status_bp.route("/send-status",methods=["POST"])
def send_status():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        status = data.get("status")
        action = data.get("action")
        timestamp = datetime.now()
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT username FROM status WHERE username = ?",(username,))
        result_status = cur.fetchall()
        if result_status:
            cur.execute("DELETE * FROM status WHERE username = ?",(username,))
            conn.commit()
            cur.execute("INSERT INTO status (username, status, timestamp) VALUES (?, ?, ?, ?)",(username, status, timestamp, action))
        else:
            cur.execute("INSERT INTO status (username, status, timestamp) VALUES (?, ?, ?, ?)",(username, status, timestamp, action))
        conn.commit()
    else:
        return jsonify({"status":"forbidden"}),403
@status_bp.route("/view-status", methods=["POST"])
def view_status():
    data = request.get_json()
    username_target = data.get("username_target")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT * FROM status WHERE username = ?",(username_target))
    result = cur.fetchone()
    return jsonify(result)
@status_bp.route("/view-online",methods=["POST"])
def view_online():
    data = request.get_json()
    target = data.get("target")
    if target == "global":
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT username FROM status WHERE status = ?",("online"))
        result = cur.fetchone()
        conn.close()
        return jsonify(result)