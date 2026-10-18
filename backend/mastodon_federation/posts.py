from flask import Blueprint, jsonify, request
from .get_posts import fetch_mastodon_posts, fetch_mastodon_comments, fetch_mastodon_user_avatar

post_bp = Blueprint('mastodon_posts', __name__)

@post_bp.route("/feed/mastodon/<path:tag>")
def get_posts(tag):
    dados_limpos = fetch_mastodon_posts(tag=tag, limit=100)
    return jsonify(dados_limpos)

@post_bp.route("/feed/mastodon/comments", methods=["POST", "post"])
def get_comments():
    data = request.get_json(force=True, silent=True) or {}
    
    post_id = data.get("post_id") or data.get("unique_id")
    original_url = data.get("original_url")  # <--- EXTRAI A URL ORIGINAL DO POST

    print(f"\n[DEBUG BRUNO] JSON recebido: {data}")
    print(f"[DEBUG BRUNO] ID extraído: {post_id} | Original URL: {original_url}\n")
    
    if not post_id:
        return jsonify({"erro": "O cliente não enviou o post_id ou unique_id!"}), 400
    comments = fetch_mastodon_comments(original_post_id=post_id, original_url=original_url)
    return jsonify({"comments": comments})

@post_bp.route("/feed/mastodon/profile_picture/<path:username>", methods=["get"])
def get_profile_picture(username):
    avatar_url = fetch_mastodon_user_avatar(username)
    return jsonify({"avatar_url": avatar_url})