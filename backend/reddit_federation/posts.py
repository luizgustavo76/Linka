from datetime import datetime
from html import unescape
import re
import urllib.parse
import xml.etree.ElementTree as ET
from flask import Blueprint, jsonify, request
import requests

post_bp = Blueprint("reddit_post_bp", __name__)

CLOUDFLARE_WORKER_URL = "https://fancy-fire-49d2.luizsgustavo76.workers.dev"


def aplicar_proxy_wsrv(original_url):
    if not original_url:
        return ""
    url_enc = urllib.parse.quote(original_url, safe="")
    return f"https://wsrv.nl/?url={url_enc}&output=jpg&q=75"


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
                title = (
                    title_elem.text.strip()
                    if title_elem is not None and title_elem.text
                    else ""
                )

                author = "entity404"
                author_elem = entry.find("author")
                if author_elem is not None:
                    name_elem = author_elem.find("name")
                    if name_elem is not None and name_elem.text:
                        author = name_elem.text
                    elif (
                        author_elem.find("uri") is not None
                        and author_elem.find("uri").text
                    ):
                        author = (
                            author_elem.find("uri").text.split("/user/")[-1]
                        )
                    elif author_elem.text:
                        author = author_elem.text
                    author = re.sub(r"^/?u/", "", author).strip()

                if not author or author in ["[deleted]", "AutoModerator"]:
                    continue

                body = ""
                image_url = ""

                if content_elem is not None and content_elem.text:
                    content_html = unescape(content_elem.text)
                    img_match = re.search(
                        r'src=["\'](https://(?:i|preview|external-preview)\.redd\.it/[^"\']+|https://i\.imgur\.com/[^"\']+)["\']',
                        content_html,
                        re.IGNORECASE,
                    )
                    if img_match:
                        image_url = img_match.group(1)
                    else:
                        link_match = re.search(
                            r'href=["\'](https://i\.redd\.it/[^"\']+\.(?:jpg|jpeg|png|gif))["\']',
                            content_html,
                            re.IGNORECASE,
                        )
                        if link_match:
                            image_url = link_match.group(1)

                    text_match = re.search(
                        r'<div class="md">(.*?)</div>', content_html, re.DOTALL
                    )
                    if text_match:
                        body = re.sub(
                            r"<[^>]+>", "", text_match.group(1)
                        ).strip()
                    body = re.sub(
                        r"\[link\]|\[comments\]", "", body, flags=re.IGNORECASE
                    ).strip()

                if not image_url:
                    media_elem = entry.find("thumbnail")
                    if (
                        media_elem is not None
                        and "url" in media_elem.attrib
                    ):
                        thumb = media_elem.attrib["url"]
                        if thumb.startswith("http"):
                            image_url = thumb

                if not title and not body and not image_url:
                    continue

                components = []
                if title:
                    components.append(title)
                if body:
                    components.append(body)
                if image_url:
                    clean_img_url = sanitizar_url_reddit(image_url)
                    proxied_img = aplicar_proxy_wsrv(clean_img_url)
                    components.append(f"[IMAGE]{proxied_img}")

                posts.append(
                    {
                        "id": len(posts) + 1,
                        "text_post": "\n".join(components),
                        "username": (
                            f"@{author}"
                            if not author.startswith("@")
                            else author
                        ),
                        "datetime": datetime.now().strftime(
                            "%Y-%m-%d %H:%M:%S"
                        ),
                    }
                )
                if len(posts) >= limit:
                    break
    except Exception as e:
        print(f"[REDDIT EXCEPTION] {e}")

    return posts


@post_bp.route(
    "/feed/reddit/<path:subreddit>", methods=["GET"], strict_slashes=False
)
def subreddit_posts(subreddit=None):
    limit = request.args.get("limit", default=100, type=int)
    posts = fetch_reddit_posts(subreddit=subreddit, limit=limit)
    return jsonify(posts), 200