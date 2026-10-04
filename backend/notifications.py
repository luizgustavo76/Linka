from flask import Flask, jsonify, request, Blueprint
import os
import sqlite3

notifications_blueprint = Blueprint("notifications", __name__)

base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
os.makedirs(db_dir, exist_ok=True)
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
            read BOOLEAN DEFAULT FALSE, 
            id INTEGER PRIMARY KEY AUTOINCREMENT
        )
    """)
    conn.commit()
    conn.close()
    print(f"[DEBUG Notifications] DB verificado em: {notifications_dir}")

create_db()

@notifications_blueprint.route("/notifications", methods=["POST"])
def notifications():
    data = request.get_json(force=True) or {}
    username = data.get("username")
    
    print(f"\n[DEBUG /notifications] Requisição recebida com JSON: {data}")

    if not username:
        print(" [DEBUG /notifications] Erro: Requisição sem 'username'!")
        return jsonify([]), 200

    username = str(username).strip()

    conn = get_db()
    cur = conn.cursor()

    # INSPEÇÃO DO BANCO: Lista todos os registros existentes no log do servidor
    cur.execute("SELECT id, receiver, from_user, read, content FROM notifications")
    raw_all = cur.fetchall()
    print(f" [DEBUG /notifications] Total de registros no arquivo DB: {len(raw_all)}")
    for row in raw_all:
        print(f"   -> ID: {row[0]} | Receiver: '{row[1]}' | From: '{row[2]}' | Read: {row[3]} | Content: '{row[4]}'")

    # Busca registros correspondentes ao usuário e pendentes de leitura
    cur.execute(
        "SELECT receiver, from_user, datetime, type, content, read, id FROM notifications WHERE receiver = ? AND (read = 0 OR read IS NULL OR read = FALSE)", 
        (username,)
    )
    result = cur.fetchall()
    conn.close()
    
    notifications_list = []
    for items in result:
        notifications_list.append({
            "receiver": items[0],
            "from_user": items[1],
            "datetime": items[2],
            "type": items[3],
            "content": items[4],
            "read": bool(items[5]),
            "id": items[6] 
        })
        
    print(f" [DEBUG /notifications] Retornando {len(notifications_list)} notificações para '{username}'")
    return jsonify(notifications_list), 200

@notifications_blueprint.route("/set-read-notification", methods=["POST"])
def set_read():
    data = request.get_json(force=True) or {}
    id_notif = data.get("id")
    
    print(f"\n[DEBUG /set-read-notification] Payload recebido: {data}")

    if id_notif is None:
        return jsonify({"error": "Missing notification id"}), 400
        
    try:
        id_notif = int(id_notif)
    except ValueError:
        return jsonify({"error": "Invalid ID format"}), 400

    conn = get_db()
    cur = conn.cursor()
    cur.execute("UPDATE notifications SET read = 1 WHERE id = ?", (id_notif,))
    conn.commit()
    conn.close()
    
    print(f" [DEBUG /set-read-notification] Notificação ID {id_notif} marcada como LIDA!")
    return jsonify({"status": "success"}), 200