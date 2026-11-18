import os
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, request, jsonify, g
from datetime import datetime
import notificationsModule

chat_bp = Blueprint("chat", __name__)

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS chat (
            id SERIAL PRIMARY KEY,
            sender TEXT NOT NULL,
            receiver TEXT NOT NULL,
            message TEXT NOT NULL,
            date TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )
    """)
    conn.commit()
    cur.close()
    conn.close()

@chat_bp.route("/send-message", methods=["POST"])
def send():
    data = request.get_json(silent=True) or {}

    sender = data.get("sender")
    if sender == getattr(g, "username", None):
        receiver = data.get("receiver")
        message = data.get("message")

        if not receiver:
            return jsonify({"status": "receiver cannot be null"}), 400

        if not sender:
            return jsonify({"status": "sender cannot be null"}), 400

        if not message:
            return jsonify({"status": "message cannot be empty"}), 400

        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute("""
                INSERT INTO chat (sender, receiver, message)
                VALUES (%s, %s, %s)
            """, (sender, receiver, message))
            conn.commit()
        except Exception as e:
            return jsonify({"status": "error", "message": str(e)}), 500
        finally:
            cur.close()
            conn.close()

        date = datetime.now()
        notificationsModule.CreateNotification(sender, receiver, date, "chat", message)    
        return jsonify({"status": "message sent"}), 200
    
    return jsonify({"status": "forbidden"}), 403

@chat_bp.route("/view", methods=["POST"])
def view():
    data = request.get_json(silent=True) or {}

    user1 = data.get("user1")
    user2 = data.get("user2")
    current_user = getattr(g, "username", None)

    if user1 == current_user or user2 == current_user:
        if not user1:
            return jsonify({"status": "user1 cannot be null"}), 400

        if not user2:
            return jsonify({"status": "user2 cannot be null"}), 400

        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute("""
                SELECT sender, receiver, message, date
                FROM chat
                WHERE (sender = %s AND receiver = %s)
                   OR (sender = %s AND receiver = %s)
                ORDER BY date ASC
            """, (user1, user2, user2, user1))

            resultado = cur.fetchall()
        finally:
            cur.close()
            conn.close()

        mensagens = []
        for row in resultado:
            mensagens.append({
                "sender": row["sender"],
                "receiver": row["receiver"],
                "message": row["message"],
                "date": str(row["date"]) if row["date"] else None
            })

        return jsonify({"messages": mensagens}), 200

    return jsonify({"status": "forbidden"}), 403