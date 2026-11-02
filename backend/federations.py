import os
from flask import Flask, request, jsonify, Blueprint
from linkaFederations import LinkaFederations
from federation_crypto import get_my_public_key_pem, verify_incoming_request

federations_bp = Blueprint("federations_bp", __name__)

# Puxa a URL pública real do Render ou cai no localhost se estiver em dev
ACTUAL_SERVER_URL = os.getenv("ACTUAL_SERVER_URL", "http://127.0.0.1:5000")

@federations_bp.route("/.well-known/federation-key", methods=["GET"])
def get_federation_key():
    return jsonify({"public_key": get_my_public_key_pem()}), 200

def protection_layer():
    is_valid, msg, status_code = verify_incoming_request(request)
    if not is_valid:
        return jsonify({"status": "forbidden", "error": msg}), status_code
    return None

@federations_bp.route("/send-request", methods=["POST"])
def send_request():


    try:
        data = request.get_json(silent=True)
        
        # Trata o caso de payload enviado como string JSON duplamente encodada
        if isinstance(data, str):
            import json
            data = json.loads(data)

        if not isinstance(data, dict):
            return jsonify({
                "status": "error",
                "error": f"Expected JSON object, got {type(data).__name__}"
            }), 400

        payload = data.get("payload", {})
        headers = data.get("headers", {})
        method = str(data.get("method", "POST")).upper()
        route = data.get("route", "")
        url = data.get("url", "")

        if not url:
            return jsonify({"status": "error", "error": "Target URL is required"}), 400

        fed = LinkaFederations()
        fed.actual_server = ACTUAL_SERVER_URL

        response = None
        if method == "POST":
            response = fed.sendPayload(payload, url, route, headers)
        elif method == "GET":
            response = fed.receiveConnection(url, route, headers)
        else:
            return jsonify({
                "status": "error",
                "error": f"Method {method} not supported"
            }), 400

        return jsonify({"status": "request_sent", "response": response}), 200

    except Exception as e:
        print(f"[ERROR /send-request]: {e}")
        return jsonify({"status": "error", "error": str(e)}), 500