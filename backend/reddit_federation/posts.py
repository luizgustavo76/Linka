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
                author = author_elem.text.replace("/u/", "").replace("u/", "").strip() if author_elem is not None and author_elem.text else "Anônimo"
                
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

                text_content = "\n".join(components)

                posts.append({
                    "id": len(posts) + 1,
                    "text_post": text_content,
                    "username": author,
                })

                if len(posts) >= limit:
                    break

            if posts:
                print(f"✅ Sucesso via Cloudflare Proxy! {len(posts)} posts encontrados.")
                return jsonify(posts), 200

    except Exception as e:
        print(f"⚠️ Erro ao processar via Worker: {e}")

    return jsonify(posts), 200