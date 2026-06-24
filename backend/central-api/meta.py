from flask import Blueprint, jsonify
import json
with open("meta/meta.json", "r") as f:
    config = json.load(f)
meta_bp = Blueprint("meta_bp",__name__)
@meta_bp.route("/meta")
def meta():
    return jsonify(config)