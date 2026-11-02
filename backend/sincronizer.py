import os
import hmac
import hashlib
import time
import requests
import dotenv
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, request, jsonify

base_dir = os.path.dirname(os.path.abspath(__file__))
env_path = os.path.join(base_dir, "backend.env")

sincronizer_bp = Blueprint("sincronizer_bp", __name__)

if os.path.exists(env_path):
    dotenv.load_dotenv(env_path)

SECRET_KEY = os.getenv("SECRET_KEY", "CHAVE_RESERVA_DE_EMERGENCIA").encode()
DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def create_db():
    if not DATABASE_URL:
        return
    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("""
            CREATE TABLE IF NOT EXISTS sincronizer (
                id SERIAL PRIMARY KEY,
                token_hash TEXT UNIQUE NOT NULL,
                timestamp TEXT NOT NULL,
                status TEXT DEFAULT 'PENDING'
            )
        """)
        conn.commit()
    except Exception as e:
        conn.rollback()
        print(f"[ERRO create_db sincronizer]: {e}")
    finally:
        cur.close()
        conn.close()

@sincronizer_bp.route("/sendToken", methods=["POST"])
def send_token():
    data = request.get_json(silent=True) or {}
    if not data:
        return jsonify({"status": "error", "message": "Missing JSON payload"}), 400

    destiny = data.get("destiny")
    token = data.get("token")

    if not destiny or not token:
        return jsonify({"status": "error", "message": "Missing destiny or token"}), 400

    token_hash = hmac.new(SECRET_KEY, token.encode(), hashlib.sha256).hexdigest()
    timestamp = str(int(time.time()))

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute(
            "INSERT INTO sincronizer (token_hash, timestamp, status) VALUES (%s, %s, 'PENDING')",
            (token_hash, timestamp)
        )
        conn.commit()
    except Exception:
        conn.rollback()
        cur.close()
        conn.close()
        return jsonify({"status": "error", "message": "Token already exists or processing"}), 409
    finally:
        if not conn.closed:
            cur.close()
            conn.close()

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
            try:
                cur.execute("DELETE FROM sincronizer WHERE token_hash = %s", (token_hash,))
                conn.commit()
            finally:
                cur.close()
                conn.close()
            return jsonify({"status": "error", "message": "Remote federation rejected the token"}), response.status_code

    except requests.exceptions.RequestException as e:
        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute("DELETE FROM sincronizer WHERE token_hash = %s", (token_hash,))
            conn.commit()
        finally:
            cur.close()
            conn.close()
        return jsonify({"status": "error", "message": f"Could not reach destiny federation: {str(e)}"}), 502

@sincronizer_bp.route("/receiveToken", methods=["POST"])
def receive_token():
    data = request.get_json(silent=True) or {}
    if not data:
        return jsonify({"status": "error", "message": "Missing JSON payload"}), 400

    token = data.get("token")
    if not token:
        return jsonify({"status": "error", "message": "Missing token parameter"}), 400

    token_hash_recebido = hmac.new(SECRET_KEY, token.encode(), hashlib.sha256).hexdigest()

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("SELECT id, status FROM sincronizer WHERE token_hash = %s", (token_hash_recebido,))
        row = cur.fetchone()

        if not row:
            timestamp = str(int(time.time()))
            cur.execute(
                "INSERT INTO sincronizer (token_hash, timestamp, status) VALUES (%s, %s, 'CONFIRMED')",
                (token_hash_recebido, timestamp)
            )
            conn.commit()
            return jsonify({"status": "success", "message": "Token received and saved as CONFIRMED"}), 200

        record_id = row["id"]
        status_atual = row["status"]

        if status_atual == 'PENDING':
            cur.execute("UPDATE sincronizer SET status = 'CONFIRMED' WHERE id = %s", (record_id,))
            conn.commit()
            return jsonify({"status": "success", "message": "Local PENDING token matched and updated to CONFIRMED"}), 200

        return jsonify({"status": "already_confirmed", "message": "Token was already confirmed previously"}), 200

    except Exception as e:
        conn.rollback()
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        cur.close()
        conn.close()