from flask import Flask, request, Blueprint, jsonify
import sqlite3
import os

chat_bp = Blueprint("chat", __name__)

base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")

# garante que a pasta DB existe
if not os.path.exists(db_dir):
    os.makedirs(db_dir)

def get_db():
    conn = sqlite3.connect(os.path.join(db_dir, "chat.db"))
    return conn

def create_db():
    conn = get_db()
    cur = conn.cursor()

    cur.execute("""
        CREATE TABLE IF NOT EXISTS chat (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            sender TEXT NOT NULL,
            receiver TEXT NOT NULL,
            message TEXT NOT NULL,
            date DATETIME DEFAULT CURRENT_TIMESTAMP
        )
    """)

    conn.commit()
    conn.close()

create_db()

# enviar mensagem
@chat_bp.route("/send-message", methods=["POST"])
def send():
    data = request.get_json()

    sender = data.get("sender")
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

    cur.execute("""
        INSERT INTO chat (sender, receiver, message)
        VALUES (?, ?, ?)
    """, (sender, receiver, message))

    conn.commit()
    conn.close()

    return jsonify({"status": "message sent"}), 200


# ver conversa completa entre 2 usuários
@chat_bp.route("/view", methods=["POST"])
def view():
    data = request.get_json()

    user1 = data.get("user1")
    user2 = data.get("user2")

    if not user1:
        return jsonify({"status": "user1 cannot be null"}), 400

    if not user2:
        return jsonify({"status": "user2 cannot be null"}), 400

    conn = get_db()
    cur = conn.cursor()

    cur.execute("""
        SELECT sender, receiver, message, date
        FROM chat
        WHERE (sender = ? AND receiver = ?)
           OR (sender = ? AND receiver = ?)
        ORDER BY date ASC
    """, (user1, user2, user2, user1))

    resultado = cur.fetchall()
    conn.close()

    mensagens = []
    for row in resultado:
        mensagens.append({
            "sender": row[0],
            "receiver": row[1],
            "message": row[2],
            "date": row[3]
        })

    return jsonify({"messages": mensagens}), 200


if __name__ == "__main__":
    chat_bp.run(debug=True)