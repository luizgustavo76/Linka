from flask import Blueprint, jsonify
from .get_posts import fetch_mastodon_posts

post_bp = Blueprint('mastodon_posts', __name__)

@post_bp.route("/feed/mastodon/<path:tag>")
def get_posts(tag):
    dados_limpos = fetch_mastodon_posts(tag=tag, limit=100)
    return jsonify(dados_limpos)