# posts.py
from flask import Blueprint, jsonify
# Importe as suas funções de onde elas estiverem salvas
from get_posts import fetch_mastodon_posts, formate 

post_bp = Blueprint('posts', __name__)

@post_bp.route("/feed")  # Ou a rota que você definiu
def get_posts():
    # 1. Busca os posts brutos da API
    raw_posts = fetch_mastodon_posts(total_limit=100)
    dados_limpos = formate(raw_posts)
    return jsonify(dados_limpos)