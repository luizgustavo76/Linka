from flask import Blueprint, jsonify
import requests
import time
import xml.etree.ElementTree as ET
import re
from html import unescape

post_bp = Blueprint("post_bp", __name__)

@post_bp.route("/")
@post_bp.route("/feed")
@post_bp.route("/feed/")
@post_bp.route("/feed/<subreddit>")
def subreddit_posts(subreddit="comentariosMelhores"):
    clear_sub = subreddit.strip("/") if subreddit else "comentariosMelhores"
    
    if clear_sub.endswith("/feed"):
        clear_sub = clear_sub[:-5]
    
    if not clear_sub or clear_sub.lower() in ["feed", "valide-session"]:
        clear_sub = "comentariosMelhores"

    posts = []
    
    rss_url = f"https://www.reddit.com/r/{clear_sub}/new.rss?limit=30"
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:120.0) Gecko/20100101 Firefox/120.0"
    }

    try:
        start = time.time()
        res = requests.get(rss_url, headers=headers, timeout=5)
        print(f"Status RSS ({clear_sub}): {res.status_code} em {time.time() - start:.2f}s")

        if res.status_code == 200 and res.text.strip():
            root = ET.fromstring(res.text)
            ns = {'atom': 'http://www.w3.org/2005/Atom'}

            for entry in root.findall('atom:entry', ns):
                title_elem = entry.find('atom:title', ns)
                author_elem = entry.find('atom:author/atom:name', ns)
                content_elem = entry.find('atom:content', ns)

                title = title_elem.text.strip() if title_elem is not None and title_elem.text else ""
                author = author_elem.text.replace("/u/", "").strip() if author_elem is not None and author_elem.text else "Anônimo"
                
                if not author or author in ["[deleted]", "AutoModerator"]:
                    continue

                body = ""
                url = ""

                if content_elem is not None and content_elem.text:
                    content_html = unescape(content_elem.text)
                    
                    img_match = re.search(r'href="(https://i\.redd\.it/[^"]+|https://preview\.redd\.it/[^"]+|[^"]+\.(?:jpg|jpeg|png|gif))"', content_html, re.IGNORECASE)
                    if img_match:
                        url = img_match.group(1)
                    else:
                        thumb_match = re.search(r'src="(https://[^"]+\.(?:jpg|jpeg|png|gif)[^"]*)"', content_html, re.IGNORECASE)
                        if thumb_match:
                            url = thumb_match.group(1)

                    text_match = re.search(r'<div class="md">(.*?)</div>', content_html, re.DOTALL)
                    if text_match:
                        raw_text = text_match.group(1)
                        body = re.sub(r'<[^>]+>', '', raw_text).strip()

                if url and body:
                    text_content = f"{title}\n{body}\n[IMAGE]{url}"
                elif url:
                    text_content = f"{title}\n[IMAGE]{url}"
                elif body:
                    text_content = f"{title}\n{body}"
                else:
                    text_content = title

                posts.append({
                    "id": len(posts) + 1,
                    "text_post": text_content,
                    "username": author,
                })

                if len(posts) >= 15:
                    break

            if posts:
                print(f"✅ Sucesso via RSS! {len(posts)} posts encontrados.")
                return jsonify(posts), 200

    except Exception as e:
        print(f"⚠️ Erro ao processar RSS: {e}")

    return jsonify(posts), 200