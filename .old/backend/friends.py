import os
import datetime
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, request, jsonify, g

friends_bp = Blueprint("friends", __name__)

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def create_db():
    conn = get_db()
    cur = conn.cursor()
    
    cur.execute("""
        CREATE TABLE IF NOT EXISTS friends (
            id SERIAL PRIMARY KEY,
            receiver TEXT,
            remittee TEXT
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS external_contacts (
            id SERIAL PRIMARY KEY,
            username TEXT,
            contact_name TEXT,
            url TEXT
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS nicknames (
            definer_username TEXT NOT NULL,
            target_username TEXT NOT NULL, 
            nickname TEXT NOT NULL,
            PRIMARY KEY (definer_username, target_username)
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS inbox (
            id SERIAL PRIMARY KEY,
            receiver TEXT,
            remittee TEXT,
            message TEXT
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS notifications (
            id SERIAL PRIMARY KEY,
            receiver TEXT,
            type TEXT,
            content TEXT,
            from_user TEXT,
            datetime TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    
    conn.commit()
    cur.close()
    conn.close()

def create_notification(text, notif_type, receiver, from_user):
    conn = get_db()
    cur = conn.cursor()
    date = datetime.datetime.now()
    try:
        cur.execute(
            "INSERT INTO notifications (receiver, type, content, from_user, datetime) VALUES (%s, %s, %s, %s, %s)",
            (receiver, notif_type, text, from_user, date)
        )
        conn.commit()
    finally:
        cur.close()
        conn.close()

@friends_bp.route("/set-nickname", methods=["POST"])
def set_nickname():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    
    if username != getattr(g, "username", None):
        return jsonify({"status": "forbidden"}), 403

    receiver = data.get("receiver")
    nickname = data.get("nickname", "").strip()
    to_me = data.get("toMe", False) 
    if not receiver:
        return jsonify({"status": "receiver is missing"}), 400

    target_user = username if to_me else receiver

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("""
            INSERT INTO nicknames (definer_username, target_username, nickname)
            VALUES (%s, %s, %s)
            ON CONFLICT (definer_username, target_username) 
            DO UPDATE SET nickname = EXCLUDED.nickname
        """, (username, target_user, nickname))
        conn.commit()
        return jsonify({
            "status": "success",
            "message": "nickname updated",
            "target": target_user,
            "nickname": nickname
        }), 200
    except Exception as e:
        conn.rollback()
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        cur.close()
        conn.close()

@friends_bp.route("/friends", methods=["POST"])
def friends():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    if username == getattr(g, "username", None):
        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute("SELECT receiver, remittee FROM friends WHERE receiver = %s OR remittee = %s", (username, username))
            friends_list = cur.fetchall()
            # Retorna lista de pares [receiver, remittee] para compatibilidade com o app Android
            formatted_friends = [[row["receiver"], row["remittee"]] for row in friends_list]
            return jsonify({"friends": formatted_friends}), 200
        except Exception as e:
            return jsonify({"status": "error in the server side", "message": str(e)}), 500
        finally:
            cur.close()
            conn.close()
    return jsonify({"status": "forbidden"}), 403

@friends_bp.route("/inbox", methods=["POST"])
def inbox():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    if username == getattr(g, "username", None):
        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute("SELECT receiver, remittee, message FROM inbox WHERE receiver = %s", (username,))
            select_inbox = cur.fetchall()
            if not select_inbox:
                return jsonify({"status": "your inbox is empty"}), 200
            
            formatted_inbox = [[row["receiver"], row["remittee"], row["message"]] for row in select_inbox]
            return jsonify({"inbox": formatted_inbox}), 200
        finally:
            cur.close()
            conn.close()
    return jsonify({"status": "forbidden"}), 403

@friends_bp.route("/accept", methods=["POST"])
def accept():
    data = request.get_json(silent=True) or {}
    receiver = data.get("receiver")
    remittee = data.get("remittee")
    
    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("INSERT INTO friends (receiver, remittee) VALUES (%s, %s)", (receiver, remittee))
        cur.execute("DELETE FROM inbox WHERE receiver = %s AND remittee = %s", (receiver, remittee))
        conn.commit()
        return jsonify({"status": "friend add"}), 200
    except Exception as e:
        conn.rollback()
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        cur.close()
        conn.close()

@friends_bp.route("/toggle-friend", methods=["POST"])
def toggle_friend():
    data = request.get_json(silent=True) or {}
    sender = data.get("sender")
    receiver = data.get("receiver")
    
    if sender == getattr(g, "username", None):
        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute("SELECT remittee FROM friends WHERE remittee = %s AND receiver = %s", (sender, receiver))
            result = cur.fetchone()
            if result:
                cur.execute("DELETE FROM friends WHERE remittee = %s AND receiver = %s", (sender, receiver))
            else:
                cur.execute("INSERT INTO inbox (receiver, remittee) VALUES (%s, %s)", (receiver, sender))
            conn.commit()
            return jsonify({"status": "added/remove the friend!"}), 200
        except Exception as e:
            conn.rollback()
            return jsonify({"status": "error", "message": str(e)}), 500
        finally:
            cur.close()
            conn.close()
            
    return jsonify({"status": "forbidden"}), 403

@friends_bp.route("/unfriend", methods=["POST"])
def unfriend():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    if username == getattr(g, "username", None):
        friend = data.get("friend")
        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute(
                "DELETE FROM friends WHERE (receiver = %s AND remittee = %s) OR (receiver = %s AND remittee = %s)",
                (username, friend, friend, username)
            )
            conn.commit()
            return jsonify({"status": "you unfriend with sucess"}), 200
        except Exception as e:
            conn.rollback()
            return jsonify({"status": "error", "message": str(e)}), 500
        finally:
            cur.close()
            conn.close()
            
    return jsonify({"status": "forbidden"}), 403

@friends_bp.route("/denied", methods=["POST"])
def denied():
    data = request.get_json(silent=True) or {}
    receiver = data.get("receiver")
    remittee = data.get("remittee")
    
    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("DELETE FROM inbox WHERE receiver = %s AND remittee = %s", (receiver, remittee))
        conn.commit()
        return jsonify({"status": "the request was deleted"}), 200
    finally:
        cur.close()
        conn.close()

@friends_bp.route("/add-external-contact", methods=["POST"])
def add_external_contact():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    if username == getattr(g, "username", None):
        name_contact = data.get("name_contact")
        url = data.get("url")
        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute("INSERT INTO external_contacts (username, contact_name, url) VALUES (%s, %s, %s)", (username, name_contact, url))
            conn.commit()
            return jsonify({"status": "external account added"}), 200
        finally:
            cur.close()
            conn.close()
            
    return jsonify({"status": "forbidden"}), 403

@friends_bp.route("/external-contacts", methods=["POST"])
def external_contacts():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    if username == getattr(g, "username", None):
        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute("SELECT username, contact_name, url FROM external_contacts WHERE username = %s", (username,))
            rows = cur.fetchall()
            contacts_list = [
                {"username": row["username"], "contact_name": row["contact_name"], "url": row["url"]} 
                for row in rows
            ]
            return jsonify(contacts_list), 200
        finally:
            cur.close()
            conn.close()
            
    return jsonify({"status": "forbidden"}), 403

@friends_bp.route("/send-friend", methods=["POST"])
def send():
    data = request.get_json(silent=True) or {}
    receiver = data.get("receiver")
    remittee = data.get("remittee")
    if remittee == getattr(g, "username", None):
        message = data.get("message")
        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute("INSERT INTO inbox (receiver, remittee, message) VALUES (%s, %s, %s)", (receiver, remittee, message))
            conn.commit()
        finally:
            cur.close()
            conn.close()

        create_notification(f"{remittee} send a friend request for you!", "inbox", receiver, remittee)
        return jsonify({"status": "request is send"}), 200
        
    return jsonify({"status": "forbidden"}), 403