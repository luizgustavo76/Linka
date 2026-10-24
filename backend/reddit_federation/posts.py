import re
import time
import urllib.parse
import xml.etree.ElementTree as ET
from html import unescape
from datetime import datetime
from flask import Blueprint, jsonify, request
import requests

post_bp = Blueprint("reddit_post_bp", __name__)

CLOUDFLARE_WORKER_URL = "https://fancy-fire-49d2.luizsgustavo76.workers.dev"


def get_relative_time(post_date):
    if not post_date:
        return ""
    if isinstance(post_date, str):
        try:
            post_date = datetime.strptime(post_date, "%Y-%m-%d %H:%M:%S")
        except ValueError:
            try:
                post_date = datetime.fromisoformat(post_date.replace("Z", "+00:00"))
            except ValueError:
                return post_date

    now = datetime.now()
    diff = now - post_date
    seconds = int(diff.total_seconds())

    if seconds < 60:
        return "just now"
    
    minutes = seconds // 60
    if minutes < 60:
        return f"{minutes}m ago"
    
    hours = minutes // 60
    if hours < 24:
        return f"{hours}h ago"
    
    days = hours // 24
    if days < 30:
        return f"{days}d ago"
    
    months = days // 30
    if months < 12:
        return f"{months} mo ago" if months == 1 else f"{months} mos ago"
    
    years = days // 365
    return f"{years} yr ago" if years == 1 else f"{years} yrs ago"


def sanitizar_url_reddit(url):
    if not url:
        return ""
    clean = unescape(url).strip()
    if "redd.it" in clean and clean.count("?") > 1:
        parts = clean.split("?")
        clean = parts[0] + "?" + "&".join(parts[1:])
    clean = re.sub(r"width=\d+", "width=1080", clean)
    clean = re.sub(r"height=\d+", "", clean)
    clean = re.sub(r"crop=[^&]+", "", clean)
    clean = re.sub(r"&&+", "&", clean)
    return clean.strip("&?")


def fetch_reddit_posts(subreddit="LinkaProject", limit=100):
    clear_sub = subreddit.strip("/") if subreddit else "LinkaProject"
    if clear_sub.endswith("/feed"):
        clear_sub = clear_sub[:-5]
    if not clear_sub or clear_sub.lower() in ["feed", "valide-session"]:
        clear_sub = "LinkaProject"

    target_rss = f"https://www.reddit.com/r/{clear_sub}/new.rss?limit={limit}"
    proxy_url = f"{CLOUDFLARE_WORKER_URL}/?url={urllib.parse.quote(target_rss, safe='')}"

    headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}

    posts = []
    try:
        res = requests.get(proxy_url, headers=headers, timeout=6)
        
        if res.status_code != 200:
            res = requests.get(target_rss, headers=headers, timeout=6)

        if res.status_code == 200 and res.text.strip():
            root = ET.fromstring(res.text)
            for elem in root.iter():
                if "}" in elem.tag:
                    elem.tag = elem.tag.split("}", 1)[1]

            entries = root.findall("entry")
            for entry in entries:
                title_elem = entry.find("title")
                content_elem = entry.find("content")
                published_elem = entry.find("published") or entry.find("updated")
                
                title = title_elem.text.strip() if title_elem is not None and title_elem.text else ""

                author = "entity404"
                author_elem = entry.find("author")
                if author_elem is not None:
                    name_elem = author_elem.find("name")
                    if name_elem is not None and name_elem.text:
                        author = name_elem.text
                    elif author_elem.find("uri") is not None and author_elem.find("uri").text:
                        author = author_elem.find("uri").text.split("/user/")[-1]
                    elif author_elem.text:
                        author = author_elem.text
                    author = re.sub(r"^/?u/", "", author).strip()

                if not author or author in ["[deleted]", "AutoModerator"]:
                    continue

                datetime_formatado = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
                if published_elem is not None and published_elem.text:
                    try:
                        dt = datetime.fromisoformat(published_elem.text.replace("Z", "+00:00"))
                        datetime_formatado = dt.strftime("%Y-%m-%d %H:%M:%S")
                    except Exception:
                        pass

                body = ""
                found_images = []
                width = None
                height = None

                if content_elem is not None and content_elem.text:
                    content_html = unescape(content_elem.text)
                    
                    img_matches = re.findall(
                        r'src=["\'](https://(?:i|preview|external-preview)\.redd\.it/[^"\']+|https://i\.imgur\.com/[^"\']+)["\']',
                        content_html, re.IGNORECASE
                    )
                    if img_matches:
                        for img in img_matches:
                            clean_img = sanitizar_url_reddit(img)
                            if clean_img and clean_img not in found_images:
                                found_images.append(clean_img)
                    else:
                        link_matches = re.findall(
                            r'href=["\'](https://i\.redd\.it/[^"\']+\.(?:jpg|jpeg|png|gif))["\']',
                            content_html, re.IGNORECASE
                        )
                        if link_matches:
                            for lk in link_matches:
                                clean_lk = sanitizar_url_reddit(lk)
                                if clean_lk and clean_lk not in found_images:
                                    found_images.append(clean_lk)

                    text_match = re.search(r'<div class="md">(.*?)</div>', content_html, re.DOTALL)
                    if text_match:
                        body = re.sub(r"<[^>]+>", "", text_match.group(1)).strip()
                    body = re.sub(r"\[link\]|\[comments\]", "", body, flags=re.IGNORECASE).strip()

                if not found_images:
                    media_elem = entry.find("thumbnail")
                    if media_elem is not None and "url" in media_elem.attrib:
                        thumb = media_elem.attrib["url"]
                        if thumb.startswith("http"):
                            clean_thumb = sanitizar_url_reddit(thumb)
                            if clean_thumb:
                                found_images.append(clean_thumb)
                            if "width" in media_elem.attrib:
                                width = media_elem.attrib["width"]
                            if "height" in media_elem.attrib:
                                height = media_elem.attrib["height"]

                if found_images and not width:
                    w_match = re.search(r'width=(\d+)', found_images[0])
                    h_match = re.search(r'height=(\d+)', found_images[0])
                    if w_match:
                        width = w_match.group(1)
                    if h_match:
                        height = h_match.group(1)

                if not title and not body and not found_images:
                    continue

                components = []
                if title:
                    components.append(title)
                if body:
                    components.append(body)

                media_url = None
                media_type = None
                media_list = []

                if found_images:
                    media_url = found_images[0]
                    media_type = "image"

                    for img_item in found_images:
                        components.append(f"[IMAGE]{img_item}")
                        media_list.append({
                            "media_url": img_item,
                            "media_type": "image",
                            "width": width or "",
                            "heigth": height or ""
                        })

                text_final = "\n".join(components).strip()

                posts.append({
                    "id": len(posts) + 1,
                    "username": f"@{author}" if not author.startswith("@") else author,
                    "text_post": text_final,
                    "datetime": datetime_formatado,
                    "relative_datetime": get_relative_time(datetime_formatado),
                    "comment_count": 0,
                    "media": {
                        "media_url": media_url,
                        "media_type": media_type,
                        "width": width,
                        "heigth": height
                    },
                    "media_list": media_list,
                    "media_attachments": media_list,
                    "platform": "reddit",
                    "origin_server": "https://linkaProject.pythonanywhere.com/feed/reddit"
                })
                if len(posts) >= limit:
                    break
    except Exception as e:
        print(f"[REDDIT EXCEPTION] {e}")

    return posts


@post_bp.route("/feed/reddit/<path:subreddit>", methods=["GET"], strict_slashes=False)
def subreddit_posts(subreddit=None):
    limit = request.args.get("limit", default=100, type=int)
    posts = fetch_reddit_posts(subreddit=subreddit, limit=limit)
    return jsonify(posts), 200