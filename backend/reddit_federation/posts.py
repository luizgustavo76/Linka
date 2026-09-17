from flask import Blueprint, jsonify, request
import requests
import time
import re
from html import unescape

post_bp = Blueprint("reddit_post_bp", __name__)

@post_bp.route("/feed/reddit/<path:subreddit>", methods=["GET"], strict_slashes=False)
def subreddit_posts(subreddit=None):
    clear_sub = subreddit.strip("/") if subreddit else "LinkaProject"
    
    if clear_sub.endswith("/feed"):
        clear_sub = clear_sub[:-5]
    
    if not clear_sub or clear_sub.lower() in ["feed", "valide-session"]:
        clear_sub = "LinkaProject"

    limit = request.args.get("limit", default=100, type=int)

    posts = []
    
    # O rss2json converte o RSS em JSON e passa liso pelo proxy do PythonAnywhere
    target_rss = f"https://www.reddit.com/r/{clear_sub}/new.rss"
    api_url = f"https://api.rss2json.com/v1/api.json?rss_url={target_rss}&api_key=&count={limit}"

    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
    }

    try:
        start = time.time()
        res = requests.get(api_url, headers=headers, timeout=8)
        print(f"Status RSS2JSON ({clear_sub}): {res.status_code} em {time.time() - start:.2f}s")

        if res.status_code == 200:
            data = res.json()
            
            if data.get("status") == "ok":
                items = data.get("items", [])
                
                for item in items:
                    title = item.get("title", "").strip()
                    author = item.get("author", "").replace("/u/", "").replace("u/", "").strip()
                    if not author:
                        author = "Anônimo"

                    # Descarta posts de moderadores ou deletados
                    if author in ["[deleted]", "AutoModerator"]:
                        continue

                    body = ""
                    image_url = item.get("thumbnail", "")

                    # Se a thumbnail do rss2json não veio válida
                    if not image_url or "static/selficon" in image_url or "default" in image_url:
                        image_url = ""

                    content_html = unescape(item.get("content", ""))

                    if content_html:
                        # Se não tinha thumbnail, extrai a imagem do HTML
                        if not image_url:
                            img_match = re.search(r'(https://i\.redd\.it/[^\s"<]+|https://preview\.redd\.it/[^\s"<]+|https://i\.imgur\.com/[^\s"<]+)', content_html, re.IGNORECASE)
                            if img_match:
                                image_url = img_match.group(1)

                        # Extrai o texto útil do post do div classe md
                        text_match = re.search(r'<div class="md">(.*?)</div>', content_html, re.DOTALL)
                        if text_match:
                            raw_text = text_match.group(1)
                            body = re.sub(r'<[^>]+>', '', raw_text).strip()
                        
                        body = re.sub(r'\[link\]|\[comments\]', '', body, flags=re.IGNORECASE).strip()

                    # Montagem do texto final no formato do LinkaLite
                    components = [title]
                    if body:
                        components.append(body)
                    if image_url:
                        components.append(f"[IMAGE]{image_url}")

                    text_content = "\n".join(components)

                    posts.append({
                        "id": len(posts) + 1,
                        "text_post": text_content,
                        "username": author,
                    })

                    if len(posts) >= limit:
                        break

                if posts:
                    print(f"✅ Sucesso via RSS2JSON! {len(posts)} posts encontrados.")
                    return jsonify(posts), 200

    except Exception as e:
        print(f"⚠️ Erro ao processar via RSS2JSON: {e}")

    return jsonify(posts), 200