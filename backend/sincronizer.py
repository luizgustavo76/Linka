from flask import Flask, Blueprint, request, jsonify
import requests
import hmac
import hashlib
import time
import dotenv
import os
import sqlite3

base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
env_path = os.path.join(base_dir, "backend.env")

sincronizer_bp = Blueprint("sincronizer_bp", __name__)
dotenv.load_dotenv(env_path)
SECRET_KEY = os.getenv("SECRET_KEY", "CHAVE_RESERVA_DE_EMERGENCIA").encode()

def get_db():
    conn = sqlite3.connect(os.path.join(db_dir, "tokenSincronizer.db"), timeout=10)
    return conn

def create_db():
    if not os.path.exists(db_dir):
        os.makedirs(db_dir)
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""CREATE TABLE IF NOT EXISTS sincronizer(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                token_hash TEXT UNIQUE,
                timestamp TEXT,
                status TEXT DEFAULT 'PENDING')""")
    conn.commit()
    conn.close()

create_db()

@sincronizer_bp.route("/sendToken", methods=["POST"])
def send_token():
    data = request.get_json()
    if not data:
        return jsonify({"status": "error", "message": "Missing JSON payload"}), 400
        
    destiny = data.get("destiny")
    token = data.get("token")
    
    if not destiny or not token:
        return jsonify({"status": "error", "message": "Missing destiny or token"}), 400

    token_hash = hmac.new(SECRET_KEY, token.encode(), hashlib.sha256).hexdigest()
    timestamp = str(int(time.time()))

    try:
        conn = get_db()
        cur = conn.cursor()
        cur.execute(
            "INSERT INTO sincronizer (token_hash, timestamp, status) VALUES (?, ?, 'PENDING')",
            (token_hash, timestamp)
        )
        conn.commit()
        conn.close()
    except sqlite3.IntegrityError:
        return jsonify({"status": "error", "message": "Token already exists or processing"}), 409

    federation_packet = {
        "token": token,
        "timestamp": timestamp
    }

    try:
        response = requests.post(url=f"{destiny}/receiveToken", json=federation_packet, timeout=5)
        
        if response.status_code == 200:
            return jsonify({
                "status": "sent", 
                "message": "Token sent and registered locally as PENDING.",
                "remote_response": response.json() if response.headers.get('Content-Type') == 'application/json' else response.text
            }), 200
        else:
            conn = get_db()
            cur = conn.cursor()
            cur.execute("DELETE FROM sincronizer WHERE token_hash = ?", (token_hash,))
            conn.commit()
            conn.close()
            return jsonify({"status": "error", "message": "Remote federation rejected the token"}), response.status_code
            
    except requests.exceptions.RequestException as e:
        conn = get_db()
        cur = conn.cursor()
        cur.execute("DELETE FROM sincronizer WHERE token_hash = ?", (token_hash,))
        conn.commit()
        conn.close()
        return jsonify({"status": "error", "message": f"Could not reach destiny federation: {str(e)}"}), 502

@sincronizer_bp.route("/receiveToken", methods=["POST"])
def receive_token():
    data = request.get_json()
    if not data:
        return jsonify({"status": "error", "message": "Missing JSON payload"}), 400
        
    token = data.get("token")
    if not token:
        return jsonify({"status": "error", "message": "Missing token parameter"}), 400
        
    token_hash_recebido = hmac.new(SECRET_KEY, token.encode(), hashlib.sha256).hexdigest()
    
    conn = get_db()
    cur = conn.cursor()
    
    cur.execute("SELECT id, status FROM sincronizer WHERE token_hash = ?", (token_hash_recebido,))
    row = cur.fetchone()
    
    if not row:
        timestamp = str(int(time.time()))
        cur.execute(
            "INSERT INTO sincronizer (token_hash, timestamp, status) VALUES (?, ?, 'CONFIRMED')",
            (token_hash_recebido, timestamp)
        )
        conn.commit()
        conn.close()
        return jsonify({"status": "success", "message": "Token received and saved as CONFIRMED"}), 200
        
    record_id = row[0]
    status_atual = row[1]
    
    if status_atual == 'PENDING':
        cur.execute("UPDATE sincronizer SET status = 'CONFIRMED' WHERE id = ?", (record_id,))
        conn.commit()
        conn.close()
        return jsonify({"status": "success", "message": "Local PENDING token matched and updated to CONFIRMED"}), 200
        
    conn.close()
    return jsonify({"status": "already_confirmed", "message": "Token was already confirmed previously"}), 200