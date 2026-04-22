from flask import Flask, request, Blueprint, jsonify
import sqlite3
import os
chat_bp = Blueprint("chat", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = (base_dir + "/DB")
def get_db():
    conn = sqlite3.connect(db_dir + "/chat.db")
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("CREATE TABLE IF NOT EXISTS chat (receiver TEXT, remittee TEXT, message TEXT, date DATETIME)")
    conn.commit()
    conn.close()
create_db()
@chat_bp.route("/send-message", methods=["POST"])
def send():
    data = request.get_json()
    receiver = data.get("receiver")
    remittee = data.get("remittee")
    message = data.get("message")
    if not receiver:
        return jsonify({"status":"is not possible to send a message for a null receiver"}),400
    if not remittee:
        return jsonify({"status":"please inform the remittee"})
    conn = get_db()
    cur = conn.cursor()
    cur.execute("INSERT INTO chat (receiver,remittee, message) VALUES (?, ?, ?)",(receiver,remittee, message))
    conn.commit()
    conn.close()
    return jsonify({"status": "message sent"}), 200
    
@chat_bp.route("/view", methods=["POST"])
def view():
    data = request.get_json()
    receiver = data.get("receiver")
    remittee = data.get("remittee")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT receiver, remittee, message, date FROM chat WHERE receiver = ?", (receiver, remittee))
    resultado = cur.fetchall()
    conn.commit()
    conn.close()
    return resultado
if __name__ == "__main__":
    chat_bp.run(debug=True)