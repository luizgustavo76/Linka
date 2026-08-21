import json
from flask import Flask, request, jsonify, Blueprint
from linkaFederations import LinkaFederations
from federation_crypto import get_my_public_key_pem, verify_incoming_request

federations_bp = Blueprint("federations_bp", __name__)

# Endpoint público para que outros nós confirmem a sua existência e peguem sua chave
@federations_bp.route("/.well-known/federation-key", methods=["GET"])
def get_federation_key():
    return jsonify({"public_key": get_my_public_key_pem()}), 200

# Exemplo de Middleware interno para proteger rotas da sua federação
def protection_layer():
    is_valid, msg, status_code = verify_incoming_request(request)
    if not is_valid:
        return jsonify({"status": "forbidden", "error": msg}), status_code
    return None

@federations_bp.route("/send-request", methods=["POST"])
def send_request():
    try:
        raw_data = request.get_data(as_text=True)
        data = json.loads(raw_data)

        if isinstance(data, str):
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

        fed = LinkaFederations()
        fed.actual_server = "http://127.0.0.1:5000"  # Coloque a URL pública desta instância aqui

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
