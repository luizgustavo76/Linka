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
SECRET_KEY = os.getenv("SECRET_KEY").encode()


def get_db():
    conn = sqlite3.connect(db_dir + "/tokenSincronizer.db")
    return conn

def create_db():
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

@sincronizer_bp.route("/send-request", methods=["POST"])
def send_request():
    data = request.get_json()
    if not data:
        return jsonify({"status": "error", "message": "Missing JSON payload"}), 400
        
    destiny = data.get("destiny")
    payload = data.get("payload") 
    username = data.get("username") 
    
    timestamp = str(int(time.time()))
    message = f"{timestamp}-{username}-{str(payload)}".encode()
    signature = hmac.new(SECRET_KEY, message, hashlib.sha256).hexdigest()
    
    federation_packet = {
        "username": username,
        "payload": payload,
        "timestamp": timestamp,
        "signature": signature
    }

    try:
        response = requests.post(url=f"{destiny}/receive-federation", json=federation_packet, timeout=5)
        
        if response.status_code == 200:
            remote_data = response.json()
            token_recebido = remote_data.get("confirmation_token")
            
            if not token_recebido:
                return jsonify({"status": "error", "message": "Destiny did not return a confirmation token"}), 500
            
            token_hash = hmac.new(SECRET_KEY, token_recebido.encode(), hashlib.sha256).hexdigest()
            
            conn = get_db()
            cur = conn.cursor()
            cur.execute(
                "INSERT INTO sincronizer (token_hash, timestamp, status) VALUES (?, ?, 'PENDING')",
                (token_hash, timestamp)
            )
            conn.commit()
            conn.close()
            
            return jsonify({
                "status": "awaiting_confirmation", 
                "message": "Payload sent. Token saved locally, awaiting callback.",
                "remote_response": remote_data
            }), 200
        else:
            return jsonify({"status": "error", "message": "Remote instance rejected the sync"}), response.status_code
            
    except requests.exceptions.RequestException as e:
        return jsonify({"status": "error", "message": f"Could not reach destiny: {str(e)}"}), 502

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
    
    cur.execute("SELECT id FROM sincronizer WHERE token_hash = ? AND status = 'PENDING'", (token_hash_recebido,))
    row = cur.fetchone()
    
    if not row:
        conn.close()
        return jsonify({"status": "unauthorized", "message": "Invalid, altered or expired token"}), 401
        
    record_id = row[0]
    cur.execute("UPDATE sincronizer SET status = 'CONFIRMED' WHERE id = ?", (record_id,))
    conn.commit()
    conn.close()
    
    return jsonify({
        "status": "success", 
        "message": "Token matched and validated! Synchronization complete."
    }), 200