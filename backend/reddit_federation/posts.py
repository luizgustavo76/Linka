from flask import Blueprint, jsonify, request
import requests
import time
import xml.etree.ElementTree as ET
import re
from html import unescape

post_bp = Blueprint("post_bp", __name__)

@post_bp.route("/", methods=["GET"], strict_slashes=False)
@post_bp.route("/feed", methods=["GET"], strict_slashes=False)
@post_bp.route("/feed/", methods=["GET"], strict_slashes=False)
@post_bp.route("/feed/<path:subreddit>", methods=["GET"], strict_slashes=False)
def subreddit_posts(subreddit=None):
    clear_sub = subreddit.strip("/") if subreddit else "LinkaProject"
    
    if clear_sub.endswith("/feed"):
        clear_sub = clear_sub[:-5]
    
    if not clear_sub or clear_sub.lower() in ["feed", "valide-session"]:
        clear_sub = "LinkaProject"

    # Permite passar ?limit=X na requisição se quiser (padrão 50)
    limit = request.args.get("limit", default=3, type=int)

    posts = []
    rss_url = f"https://www.reddit.com/r/{clear_sub}/new.rss?limit={limit}"
    headers = {
        "User-Agent": "mozilla/5.0 (Windows NT 10.0; Win64; x64) LinkaLite/2.0"
    }

    try:
        start = time.time()
        res = requests.get(rss_url, headers=headers, timeout=5)
        print(f"Status RSS ({clear_sub}): {res.status_code} em {time.time() - start:.2f}s")

        if res.status_code == 200 and res.text.strip():
            root = ET.fromstring(res.text)
            ns = {
                'atom': 'http://www.w3.org/2005/Atom',
                'media': 'http://search.yahoo.com/mrss/'
            }

            for entry in root.findall('atom:entry', ns):
                title_elem = entry.find('atom:title', ns)
                author_elem = entry.find('atom:author/atom:name', ns)
                content_elem = entry.find('atom:content', ns)
                media_elem = entry.find('media:thumbnail', ns)

                title = title_elem.text.strip() if title_elem is not None and title_elem.text else ""
                author = author_elem.text.replace("/u/", "").strip() if author_elem is not None and author_elem.text else "Anônimo"
                
                # Descarta posts de moderadores ou deletados
                if not author or author in ["[deleted]", "AutoModerator"]:
                    continue

                body = ""
                image_url = ""

                # 1. Tenta pegar imagem pela tag media:thumbnail
                if media_elem is not None and 'url' in media_elem.attrib:
                    image_url = media_elem.attrib['url']

                if content_elem is not None and content_elem.text:
                    content_html = unescape(content_elem.text)
                    
                    # 2. Se não tinha thumbnail, busca imagem válida no HTML
                    if not image_url:
                        img_match = re.search(r'(https://i\.redd\.it/[^\s"<]+|https://preview\.redd\.it/[^\s"<]+|https://i\.imgur\.com/[^\s"<]+)', content_html, re.IGNORECASE)
                        if img_match:
                            image_url = img_match.group(1)

                    # 3. Extrai apenas o texto útil do post
                    text_match = re.search(r'<div class="md">(.*?)</div>', content_html, re.DOTALL)
                    if text_match:
                        raw_text = text_match.group(1)
                        body = re.sub(r'<[^>]+>', '', raw_text).strip()
                    
                    # Limpa as tags [link] e [comments] residuais
                    body = re.sub(r'\[link\]|\[comments\]', '', body, flags=re.IGNORECASE).strip()

                # Montagem do texto final com quebra de linha garantida antes do [IMAGE]
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

                # Limite ajustado de acordo com a query ou padrão 50
                if len(posts) >= limit:
                    break

            if posts:
                print(f"✅ Sucesso via RSS! {len(posts)} posts encontrados.")
                return jsonify(posts), 200

    except Exception as e:
        print(f"⚠️ Erro ao processar RSS: {e}")

    return jsonify(posts), 200