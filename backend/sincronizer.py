from flask import Flask, Blueprint, request, jsonify
import os
import requests
from werkzeug import security
sincronizer_bp = Blueprint("sincronizer_bp", __name__)
@sincronizer_bp.route("/send-token",method=["POST"])
def send_token():
    data = request.get_json()
    token = data.get("token")
    destiny = data.get("destiny")
    hash_token = security.generate_password_hash(token)
    requests.post(url=destiny + "/receive-token",json={"token":hash_token})
    return jsonify({"status":"the token has send to destiny"}),200