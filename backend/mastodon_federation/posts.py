from flask import Blueprint, jsonify, request
from .get_posts import fetch_mastodon_posts, fetch_mastodon_comments, fetch_mastodon_user_avatar

post_bp = Blueprint('mastodon_posts', __name__)

@post_bp.route("/feed/mastodon/<path:tag>")
def get_posts(tag):
    dados_limpos = fetch_mastodon_posts(tag=tag, limit=100)

    return jsonify(dados_limpos)
@post_bp.route("feed/mastodon/comments", methods=["post"])
def get_comments():
    data = request.get_json()
    post_id = data.get("post_id")
    comments = fetch_mastodon_comments(post_id)
@post_bp.route("feed/mastodon/profile_picture", methods=["post"])
def get_profile_picture():
    data = request.get_json()
    username = data.get("username")
    fetch_mastodon_user_avatar(username)