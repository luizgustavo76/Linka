from flask import Flask, request, jsonify, Blueprint
import sqlite3
import os
friends_bp = Blueprint("friends", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = (base_dir + "/DB")
friends_file = (db_dir + "/friends.db")
def get_db():
    conn = sqlite3.connect(friends_file)
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS friends(
                receiver TEXT,
                remittee TEXT
                )
    """)
    cur.execute("CREATE TABLE IF NOT EXISTS inbox (receiver TEXT, remittee TEXT, message TEXT)")
    conn.commit()
    conn.close()
conn = get_db()
cur = conn.cursor()
cur.execute("CREATE TABLE IF NOT EXISTS inbox (receiver TEXT, remittee TEXT, message TEXT)")
conn.commit()
conn.close()
create_db()
@friends_bp.route("/friends", methods=["POST"])
def friends():
    data = request.get_json()
    username = data.get("username")
    try:
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT * FROM friends WHERE receiver OR remittee = ?",(username,))
        friends_list = cur.fetchall()
        conn.close()
        return jsonify({"friends":friends_list})
    except:
        pass
@friends_bp.route("/inbox", methods=["POST"])
def inbox():
    data = request.get_json()
    username = data.get("username")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT * FROM inbox WHERE receiver = ?", (username,))
    select_inbox = cur.fetchall()
    conn.close()
    if select_inbox == None:
        return jsonify({"status":"your inbox is empty"})
    else:
        return jsonify({"inbox":select_inbox}),200
@friends_bp.route("/accept", methods=["POST"])
def accept():
    data = request.get_json()
    receiver = data.get("receiver")
    remittee = data.get("remittee")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("INSERT INTO friends (receiver, remittee) VALUES (?, ?)",(receiver, remittee))
    conn.commit()
    conn.close()
    return jsonify({"status":"friend add"}),200
@friends_bp.route("/send-friend", methods=["POST"])
def send():
    data = request.get_json()
    receiver = data.get("receiver")
    remittee = data.get("remittee")
    message = data.get("message")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("INSERT INTO inbox (receiver, remittee, message) VALUES (?, ?, ?)",(receiver, remittee, message))
    conn.commit()
    conn.close()
    return({"status":"request is send"}),200

