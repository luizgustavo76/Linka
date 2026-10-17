from flask import Blueprint, jsonify, request
from .get_posts import fetch_mastodon_posts, fetch_mastodon_comments, fetch_mastodon_user_avatar

post_bp = Blueprint('mastodon_posts', __name__)

@post_bp.route("/feed/mastodon/<path:tag>")
def get_posts(tag):
    dados_limpos = fetch_mastodon_posts(tag=tag, limit=100)

    return jsonify(dados_limpos)
@post_bp.route("/feed/mastodon/comments", methods=["post"])
def get_comments():
    data = request.get_json() or {}
    post_id = data.get("post_id") or data.get("unique_id")
    comments = fetch_mastodon_comments(post_id)
    return jsonify({"comments": comments}) 

@post_bp.route("/feed/mastodon/profile_picture/<path:username>", methods=["get"])
def get_profile_picture(username):
    avatar_url = fetch_mastodon_user_avatar(username)
    return jsonify({"avatar_url": avatar_url}) 