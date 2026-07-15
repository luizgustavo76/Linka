from flask import Flask, request, Blueprint, jsonify
import get_posts
post_bp = Blueprint("post_bp", __name__)
@post_bp.route("/feed")
def feed():
    result = get_posts.fetch_mastodon_posts()
    formatted_posts = get_posts.formate(result)
    return jsonify(formatted_posts),200