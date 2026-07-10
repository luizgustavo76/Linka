from flask import Flask, Blueprint, request, jsonify, g
import sqlite3
import os
from datetime import datetime
reports_bp = Blueprint("reports_bp", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = (base_dir + "/DB")
reports_dir = (db_dir + "/reports.db")
def get_db():
    conn = sqlite3.connect(reports_dir)
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""CREATE TABLE IF NOT EXISTS reports(
                username TEXT,
                content TEXT,
                id TEXT,
                sender TEXT,
                timestamp TEXT)""")
    conn.commit()
    conn.close()
create_db()
@reports_bp.route("/send-report")
def send_report():
    data = request.get_json()
    username = data.get("username")
    sender = data.get("sender")
    if username == g.username:
        content = data.get("content")
        id = data.get("id")
        timestamp = datetime.now()
        if None in [content, id, sender]:
            return jsonify({"status":"incomplete report request"}),400
        else:
            conn = get_db()
            cur = conn.cursor()
            cur.execute("INSERT INTO reports (username, content, id, sender, timestamp) VALUES(?, ?, ?, ?, ?)",(username, content, id, sender, timestamp))
            conn.commit()
            conn.cursor()
    else:
        return jsonify({"status":"forbidden"}),403