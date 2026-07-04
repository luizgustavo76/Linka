from flask import Flask, request, jsonify, Blueprint
from linkaFederations import LinkaFederations
import sqlite3
import os

federations_bp = Blueprint("federations_bp", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))

@federations_bp.route("/send-request", methods=["POST"])
def send_request():
    try:
        data = request.get_json()
        payload = data.get("payload")
        headers = data.get("headers")
        method = data.get("method")
        route = data.get("route")
        url = data.get("url")
        
        fed = LinkaFederations()
        fed.actual_server = "http://127.0.0.1:5000" 
        
        if method.lower() == "post":
            response = fed.sendPayload(payload, url, route, headers)
        if method.lower() == "get":
            response = fed.receiveConnection(url, route, headers)
            
        return jsonify({"status": "the request was send!", "response": response}), 200
    except Exception as e:
        return jsonify({"status": "error send request", "error": str(e)}), 401