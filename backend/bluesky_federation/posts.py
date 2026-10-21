from flask import Blueprint, jsonify, request
from .bluesky_bridge import fetch_bluesky_posts
from .create_post import post_to_bluesky
post_bp = Blueprint('bluesky_posts', __name__)

@post_bp.route("/feed/bluesky/<path:tag>")
def get_bluesky_feed(tag="retrocomputing"):
    dados_limpos = fetch_bluesky_posts(query=tag, limit=100)
    return jsonify(dados_limpos)
@post_bp.route("/feed/bluesky/new", methods=["POST"])
def new():
    data = request.get_json()
    access_token = data.get("acess_token")
    text_post = data.get("text_post")
    handle = data.get("handle")
    if access_token not in ("", None) and handle not in ("", None):
        post_to_bluesky(handle, access_token, text_post)
        return jsonify({"status":"the post was created"}),200
    else:
        return jsonify({"status":"handle or access token cannot be null"}),403