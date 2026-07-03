from flask import Flask, request, jsonify, Blueprint, g
import sqlite3
import os
import datetime
friends_bp = Blueprint("friends", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = (base_dir + "/DB")
friends_file = (db_dir + "/friends.db")
def get_db():
    conn = sqlite3.connect(friends_file)
    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA synchronous=NORMAL;")
    conn.execute("PRAGMA cache_size=-10000;")
    return conn
def get_db_notifications():
    conn = sqlite3.connect(db_dir + "/notifications.db")
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
create_db()
def create_notification(text, type, receiver, from_user):
    conn = get_db_notifications()
    cur = conn.cursor()
    date = datetime.datetime.now()
    cur.execute("INSERT INTO notifications (receiver, type, content, from_user, datetime) VALUES(?, ?, ?, ?, ?)",(receiver, type, text, from_user, date))
    conn.commit()
    conn.close()
@friends_bp.route("/friends", methods=["POST"])
def friends():
    data = request.get_json()
    print(data)
    username = data["username"]
    if username == g.username:
        try:
            conn = get_db()
            cur = conn.cursor()
            cur.execute("SELECT * FROM friends WHERE receiver = ? OR remittee = ?",(username, username))
            friends_list = cur.fetchall()
            conn.close()
            return jsonify({"friends":friends_list}),200
        except:
            return jsonify({"status":"error in the server side"}),500
    else:
        return jsonify({"status":"forbidden"}),403
    
@friends_bp.route("/inbox", methods=["POST"])
def inbox():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT * FROM inbox WHERE receiver = ?", (username,))
        select_inbox = cur.fetchall()
        conn.close()
        if select_inbox == None:
            return jsonify({"status":"your inbox is empty"})
        else:
            return jsonify({"inbox":select_inbox}),200
    else:
        return jsonify({"status":"forbidden"}),403
@friends_bp.route("/accept", methods=["POST"])
def accept():
    data = request.get_json()
    receiver = data.get("receiver")
    remittee = data.get("remittee")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("INSERT INTO friends (receiver, remittee) VALUES (?, ?)",(receiver, remittee))
    cur.execute("DELETE FROM inbox WHERE receiver = ? AND remittee = ?", (receiver, remittee))
    conn.commit()
    conn.close()
    return jsonify({"status":"friend add"}),200
@friends_bp.route("/unfriend", methods=["POST"])
def unfriend():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        friend = data.get("friend")
        conn = get_db()
        cur = conn.cursor()
        cur.execute("DELETE (receiver, remittee) FROM friends WHERE receiver = ? AND remittee = ?",(username, friend))
        conn.commit()
        conn.close()
        return jsonify({"status":"you unfriend with sucess"}),200
    else:
        return jsonify({"status":"forbidden"}),403
@friends_bp.route("/denied", methods=["POST"])
def denied():
    data = request.get_json()
    receiver = data.get("receiver")
    remittee = data.get("remittee")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("DELETE FROM inbox WHERE receiver = ? AND remittee = ?", (receiver, remittee))
    conn.commit()
    conn.close()
    return jsonify({"status": "the request was deleted"})
@friends_bp.route("/send-friend", methods=["POST"])
def send():
    data = request.get_json()
    receiver = data.get("receiver")
    remittee = data.get("remittee")
    if remittee == g.username:
        message = data.get("message")
        conn = get_db()
        cur = conn.cursor()
        cur.execute("INSERT INTO inbox (receiver, remittee, message) VALUES (?, ?, ?)",(receiver, remittee, message))
        create_notification(f"{remittee} send a friend request for you!","inbox", receiver, remittee)
        conn.commit()
        conn.close()
        return({"status":"request is send"}),200
    else:
        return jsonify({"status":"forbidden"}),403

