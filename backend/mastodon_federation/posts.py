from flask import Blueprint, jsonify, request
from .get_posts import fetch_mastodon_posts, fetch_mastodon_comments, fetch_mastodon_user_avatar
from .create_post import post_to_mastodon
post_bp = Blueprint('mastodon_posts', __name__)

@post_bp.route("/feed/mastodon/<path:tag>")
def get_posts(tag):
    dados_limpos = fetch_mastodon_posts(tag=tag, limit=100)
    return jsonify(dados_limpos)

@post_bp.route("/feed/mastodon/comments", methods=["POST"])
def get_comments():
    data = request.get_json(force=True, silent=True) or {}
    
    post_id = data.get("post_id") or data.get("unique_id")
    original_url = data.get("original_url")

    
    if not post_id and not original_url:
        return jsonify({"comments": [], "error": "missing data"}), 400

    comments = fetch_mastodon_comments(original_post_id=post_id, original_url=original_url)
    
    return jsonify({"comments": comments})

@post_bp.route("/feed/mastodon/profile_picture/<path:username>", methods=["GET"])
def get_profile_picture(username):
    avatar_url = fetch_mastodon_user_avatar(username)
    return jsonify({"avatar_url": avatar_url})
@post_bp.route("/feed/mastodon/new", methods=["POST"])
def new_post():
    data = request.get_json()
    access_token = data.get("access_token")
    text_post = data.get("text_post")
    if access_token not in("", None):
        post_to_mastodon("mastodon.social", access_token, text_post)
        return jsonify({"status":"post was created"}),200
    else:
        return jsonify({"status":"your token is empty"}),403