from flask import Flask, request, jsonify, Blueprint
import os
import json
with open("version/meta.json", "r") as meta_file:
    version = json.load(meta_file)
with open("version/federation.json", "r") as fed_file:
    federation = json.load(fed_file)
meta_bp = Blueprint("meta", __name__)
@meta_bp.route("/meta")
def return_version():
    return jsonify(version)
@meta_bp.route("/check-federation")
def check_federation():
    return jsonify(federation)