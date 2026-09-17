from flask import Blueprint, jsonify
from bluesky_bridge import fetch_bluesky_posts

post_bp = Blueprint('posts', __name__)

@post_bp.route("/feed/bluesky")
def get_bluesky_feed():
    dados_limpos = fetch_bluesky_posts(query="retrocomputing", limit=40)
    return jsonify(dados_limpos)