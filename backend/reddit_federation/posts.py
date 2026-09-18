from flask import Blueprint, jsonify, request
import requests
import time
import xml.etree.ElementTree as ET
import re
from html import unescape

post_bp = Blueprint("reddit_post_bp", __name__)

CLOUDFLARE_WORKER_URL = "https://fancy-fire-49d2.luizsgustavo76.workers.dev"

@post_bp.route("/feed/reddit/<path:subreddit>", methods=["GET"], strict_slashes=False)
def subreddit_posts(subreddit=None):
    clear_sub = subreddit.strip("/") if subreddit else "LinkaProject"
    
    if clear_sub.endswith("/feed"):
        clear_sub = clear_sub[:-5]
    
    if not clear_sub or clear_sub.lower() in ["feed", "valide-session"]:
        clear_sub = "LinkaProject"

    limit = request.args.get("limit", default=100, type=int)
    posts = []
    
    target_rss = f"https://www.reddit.com/r/{clear_sub}/new.rss?limit={limit}"
    proxy_url = f"{CLOUDFLARE_WORKER_URL}/?url={target_rss}"

    try:
        start = time.time()
        res = requests.get(proxy_url, timeout=10)
        print(f"Status Proxy Cloudflare ({clear_sub}): {res.status_code} em {time.time() - start:.2f}s")
        
        # Log do conteúdo retornado para diagnóstico
        raw_xml = res.text.strip()
        print(f"🔍 Resposta inicial do XML ({len(raw_xml)} bytes): {raw_xml[:250]}...")

        if res.status_code == 200 and raw_xml:
            root = ET.fromstring(raw_xml)
            
            # Namespace padronizado do Atom Feed do Reddit
            ns = {
                'atom': 'http://www.w3.org/2005/Atom',
                'media': 'http://search.yahoo.com/mrss/'
            }

            # Procura por entradas com ou sem namespace
            entries = root.findall('atom:entry', ns) or root.findall('entry')

            for entry in entries:
                title_elem = entry.find('atom:title', ns) if 'atom:entry' in entry.tag else entry.find('title')
                author_elem = entry.find('atom:author/atom:name', ns) if 'atom:entry' in entry.tag else entry.find('author/name')
                content_elem = entry.find('atom:content', ns) if 'atom:entry' in entry.tag else entry.find('content')
                media_elem = entry.find('media:thumbnail', ns)

                title = title_elem.text.strip() if title_elem is not None and title_elem.text else ""
                
                author = "Anônimo"
                if author_elem is not None and author_elem.text:
                    author = author_elem.text.replace("/u/", "").replace("u/", "").strip()

                if not author or author in ["[deleted]", "AutoModerator"]:
                    continue

                body = ""
                image_url = ""

                if media_elem is not None and 'url' in media_elem.attrib:
                    image_url = media_elem.attrib['url']

                if content_elem is not None and content_elem.text:
                    content_html = unescape(content_elem.text)
                    
                    if not image_url:
                        img_match = re.search(r'(https://i\.redd\.it/[^\s"<]+|https://preview\.redd\.it/[^\s"<]+|https://i\.imgur\.com/[^\s"<]+)', content_html, re.IGNORECASE)
                        if img_match:
                            image_url = img_match.group(1)

                    text_match = re.search(r'<div class="md">(.*?)</div>', content_html, re.DOTALL)
                    if text_match:
                        raw_text = text_match.group(1)
                        body = re.sub(r'<[^>]+>', '', raw_text).strip()
                    
                    body = re.sub(r'\[link\]|\[comments\]', '', body, flags=re.IGNORECASE).strip()

                components = [title]
                if body:
                    components.append(body)
                if image_url:
                    components.append(f"[IMAGE]{image_url}")

                posts.append({
                    "id": len(posts) + 1,
                    "text_post": "\n".join(components),
                    "username": author,
                })

                if len(posts) >= limit:
                    break

            print(f"✅ Processamento concluído! Total de posts: {len(posts)}")
            return jsonify(posts), 200

    except Exception as e:
        print(f"⚠️ Erro ao processar via Worker: {e}")

    return jsonify(posts), 200