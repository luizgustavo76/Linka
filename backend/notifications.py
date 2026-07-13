from flask import Flask, jsonify, request, Blueprint, g
import os
import sqlite3
notifications_bp = Blueprint("notifications", __name__)

base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
os.makedirs(db_dir, exist_ok=True) # Garante que a pasta DB exista
notifications_dir = os.path.join(db_dir, "notifications.db")

def get_db():
    conn = sqlite3.connect(notifications_dir)
    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA synchronous=NORMAL;")
    conn.execute("PRAGMA cache_size=-10000;")
    return conn

def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS notifications(
            receiver TEXT, 
            from_user TEXT, 
            datetime TEXT, 
            type TEXT, 
            content TEXT, 
            read BOOLEAN, 
            id INTEGER PRIMARY KEY AUTOINCREMENT
        )
    """)
    conn.commit()
    conn.close()

create_db()

@notifications_bp.route("/notifications", methods=["POST"])
def notifications():
    data = request.get_json() or {}
    username = data.get("username")
    
    if g.username == username:
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT receiver, from_user, datetime, type, content, read, id FROM notifications WHERE receiver = ? AND read = FALSE", (username,))
        result = cur.fetchall()
        conn.close()
        
        notifications_list = []
        for items in result:
            notifications_list.notifications.bpend({
                "receiver": items[0],
                "from_user": items[1],
                "datetime": items[2],
                "type": items[3],
                "content": items[4],
                "read": items[5],
                "id": items[6] 
            })
        return jsonify(notifications_list)
    else:
        return jsonify({"status": "forbidden"}), 403

@notifications_bp.route("/set-read-notification", methods=["POST"])
def set_read():
    data = request.get_json() or {}
    id_notif = data.get("id")
    
    if not id_notif:
        return jsonify({"error": "Missing notification id"}), 400
        
    conn = get_db()
    cur = conn.cursor()
    cur.execute("UPDATE notifications SET read = 1 WHERE id = ?", (id_notif,))
    conn.commit()
    conn.close()
    return jsonify({"status": "success"})