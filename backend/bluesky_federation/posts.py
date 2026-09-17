from flask import Blueprint, jsonify
from .bluesky_bridge import fetch_bluesky_posts

post_bp = Blueprint('bluesky_posts', __name__)

@post_bp.route("/feed/bluesky/<path:tag>")
def get_bluesky_feed(tag="retrocomputing"):
    dados_limpos = fetch_bluesky_posts(query=tag, limit=100)
    return jsonify(dados_limpos)