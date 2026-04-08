import requests
import os
import json
from flask import Flask, jsonify, request

data_json = {}
app = Flask(__name__)

def connect(json_file):
    global data_json
    if not os.path.exists(json_file):
        print(f"LINKA ERROR: the json doesn`t exist {json_file}")
    else:
        with open(json_file, "r") as f:
            data_json = json.load(f)

@app.route("/", methods=["POST"])
def federations_recept():
    try:
        data_request = request.get_json()
        url = data_request.get("url")
        json_payload = data_request.get("json")
        method = data_request.get("method").lower()  # padroniza pra minúsculo

        if method == "post":
            response = requests.post(url, json=json_payload, timeout=5)
        elif method == "get":
            response = requests.get(url, timeout=5)
        else:
            return jsonify({"ERROR": "Invalid method"}), 400

        if response.status_code in (200, 201):
            return jsonify(response.json())
        else:
            return jsonify({"ERROR": "Linka found an error", "status_code": response.status_code}), 400

    except Exception as e:
        return jsonify({"ERROR": "Linka API side error", "details": str(e)}), 500

def start(debug=False, port=5000):
    app.run(debug=debug, port=port)