from flask import Blueprint, jsonify
from get_posts import fetch_mastodon_posts

post_bp = Blueprint('posts', __name__)

@post_bp.route("/feed")
def get_posts():
    dados_limpos = fetch_mastodon_posts(limit=100)
    return jsonify(dados_limpos)