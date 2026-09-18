import re
import time
import urllib.parse
import xml.etree.ElementTree as ET
from html import unescape

from flask import Blueprint, jsonify, request
import requests

post_bp = Blueprint("reddit_post_bp", __name__)

CLOUDFLARE_WORKER_URL = "https://fancy-fire-49d2.luizsgustavo76.workers.dev"

# Todas as imagens do Reddit vão para o lite-render,
# igual ao fluxo do Bluesky/Mastodon.
PROXY_BASE = "http://linkaProject.pythonanywhere.com/lite-render?url="


def sanitizar_url_reddit(url):
    """Remove entidades HTML, trata duplicação de '?' e formata parâmetros da imagem."""
    if not url:
        return ""

    # Decodifica entidades HTML nativas do RSS do Reddit (&amp; -> &)
    clean = unescape(url).replace("&amp;", "&").strip()

    # Corrige duplicação de '?' comum no preview.redd.it
    if "redd.it" in clean and clean.count("?") > 1:
        parts = clean.split("?")
        clean = parts[0] + "?" + "&".join(parts[1:])

    # Ajustes finos de parâmetros para evitar quebra na CDN
    clean = re.sub(r"width=\d+", "width=1080", clean)
    clean = re.sub(r"height=\d+", "", clean)
    clean = re.sub(r"crop=[^&]+", "", clean)
    clean = re.sub(r"&&+", "&", clean).strip("&?")

    return clean


@post_bp.route(
    "/feed/reddit/<path:subreddit>",
    methods=["GET"],
    strict_slashes=False,
)
def subreddit_posts(subreddit=None):
    clear_sub = subreddit.strip("/") if subreddit else "LinkaProject"

    if clear_sub.endswith("/feed"):
        clear_sub = clear_sub[:-5]

    if not clear_sub or clear_sub.lower() in ["feed", "valide-session"]:
        clear_sub = "LinkaProject"

    limit = request.args.get("limit", default=100, type=int)

    posts = []

    target_rss = (
        f"https://www.reddit.com/r/{clear_sub}/new.rss?limit={limit}"
    )

    proxy_url = f"{CLOUDFLARE_WORKER_URL}/?url={target_rss}"

    try:
        start = time.time()

        res = requests.get(proxy_url, timeout=10)

        if res.status_code == 200 and res.text.strip():
            root = ET.fromstring(res.text)

            for elem in root.iter():
                if "}" in elem.tag:
                    elem.tag = elem.tag.split("}", 1)[1]

            for entry in root.findall("entry"):
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
                        author = author_elem.find("uri").text.split("/user/")[-1]

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
                        r'<div class="md">(.*?)</div>',
                        content_html,
                        re.DOTALL,
                    )

                    if text_match:
                        raw_text = text_match.group(1)
                        body = re.sub(
                            r"<[^>]+>",
                            "",
                            raw_text,
                        ).strip()

                    body = re.sub(
                        r"\[link\]|\[comments\]",
                        "",
                        body,
                        flags=re.IGNORECASE,
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

                    # IMPORTANT:
                    # Reddit também sai como [IMAGE] + lite-render,
                    # exatamente como Bluesky/Mastodon.
                    safe_image_url = urllib.parse.quote(
                        clean_img_url,
                        safe="",
                    )

                    components.append(
                        f"[IMAGE]{PROXY_BASE}{safe_image_url}"
                    )

                posts.append(
                    {
                        "id": len(posts) + 1,
                        "text_post": "\n".join(components),
                        "username": (
                            f"@{author}"
                            if not author.startswith("@")
                            else author
                        ),
                    }
                )

                if len(posts) >= limit:
                    break

            return jsonify(posts), 200

    except Exception as e:
        print(f"⚠️ Erro ao processar XML no Flask: {e}")

    return jsonify(posts), 200