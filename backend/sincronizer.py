from flask import Flask, Blueprint, request, jsonify
import requests
import hmac
import hashlib
import time
import dotenv
import os
import secrets
from linkaFederations import LinkaFederations
sincronizer_bp = Blueprint("sincronizer_bp", __name__)
dotenv.load_dotenv()
SECRET_KEY = os.getenv("SECRET_KEY").encode()

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
        return jsonify({
            "status": "synchronized", 
            "remote_response": response.json() if response.status_code == 200 else response.text
        }), 200
        
    except requests.exceptions.RequestException as e:
        return jsonify({"status": "error", "message": f"Could not reach destiny: {str(e)}"}), 502
@sincronizer_bp.route("/receiveToken", methods=["POST"])