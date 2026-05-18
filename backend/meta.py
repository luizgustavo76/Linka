from flask import Flask, request, jsonify, Blueprint
import os
import json
with open("version/meta.json", "r") as meta_file:
    version = json.load(meta_file)
meta_bp = Blueprint("meta", __name__)
@meta_bp.route("/meta")
def return_version():
    return jsonify(version)