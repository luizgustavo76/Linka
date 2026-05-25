from flask import Flask, jsonify, request, Blueprint, g
import os
import sqlite3
notifications_blueprint = Blueprint("notifications", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
notifications_dir = db_dir + "/notifications.db"
def get_db():
    conn = sqlite3.connect(notifications_dir)
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("CREATE TABLE IF NOT EXISTS notifications(receiver TEXT, from_user TEXT, datetime TEXT, type TEXT, content TEXT, read BOOLEAN DEAFULT = FALSE), id INTEGER PRIMARY KEY AUTOINCREMENT ")
    conn.commit()
    conn.close()
create_db()
@notifications_blueprint.route("/notifications", methods=["POST"])
def notifications():
    data = request.get_json()
    username = data.get("username")
    if g.username == username:
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT * FROM notifications WHERE username = ? AND  read = FALSE", (username,))
        result = cur.fetchall()
        notifications_list = []
        for items in result:
            notifications_list.append({
                "receiver":items[0],
                "from_user":items[1],
                "datetime":items[2],
                "type":items[3],
                "content":items[4],
                "read":items[5]
            })
        return jsonify(notifications_list)
    else:
        return jsonify({"status":"forbiden"}),403
@notifications_blueprint.route("/set-read-notification", methods=["POST"])
def set_read():
    data = request.get_json()
    id = data("id")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("UPDATE notifications SET read = 1 WHERE id = ?", (id,))
    conn.commit()
    conn.close()