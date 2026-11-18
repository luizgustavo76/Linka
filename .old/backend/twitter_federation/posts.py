import json
import os
import re
import unicodedata
import xml.etree.ElementTree as ET
from datetime import datetime
from pathlib import Path
from urllib.parse import quote, unquote, urlparse

from bs4 import BeautifulSoup
import dotenv
from flask import Blueprint, jsonify, request
import requests

BASE_DIR = Path(__file__).resolve().parent.parent
env_path = BASE_DIR / ".env"
dotenv.load_dotenv()

twitter_bp = Blueprint("twitter_bp", __name__)

CODEBERG_INSTANCES_URL = "https://codeberg.org/shitter/shitter/raw/branch/master/instances.json"


def get_relative_time(post_date):
    if not post_date:
        return ""
    if isinstance(post_date, str):
        try:
            post_date = datetime.strptime(post_date, "%Y-%m-%d %H:%M:%S")
        except ValueError:
            try:
                from email.utils import parsedate_to_datetime
                dt_obj = parsedate_to_datetime(post_date)
                post_date = dt_obj.replace(tzinfo=None)
            except Exception:
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


def get_active_shitter_instances():
    headers = {
        "User-Agent": "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0"
    }
    try:
        response = requests.get(CODEBERG_INSTANCES_URL, headers=headers, timeout=4)
        if response.status_code == 200:
            data = response.json()
            instances = []
            if isinstance(data, list):
                for item in data:
                    url = item.get("url") or item.get("clearnet")
                    if url and url.startswith("http") and not url.endswith(".onion"):
                        clean_url = url.rstrip("/").replace("http://", "https://")
                        instances.append(clean_url)
            if instances:
                return instances
    except Exception:
        pass

    return [
        "https://nitter.meowing.monster",
        "https://shitter.thepixora.com",
    ]


def get_video_vxtwitter(tweet_id):
    """
    Busca URL direta .mp4, thumbnail, width e height reais na API do VxTwitter
    """
    try:
        url = f"https://api.vxtwitter.com/Twitter/status/{tweet_id}"
        headers = {"User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)"}
        res = requests.get(url, headers=headers, timeout=4.0)

        if res.status_code == 200:
            data = res.json()
            media_list = data.get("media_extended", [])
            for media in media_list:
                if media.get("type") in ("video", "gif") and media.get("url"):
                    size = media.get("size") or {}
                    return {
                        "url": media.get("url"),
                        "thumb": media.get("thumbnail_url") or "",
                        "width": str(size.get("width") or media.get("width") or ""),
                        "height": str(size.get("height") or media.get("height") or ""),
                    }
    except Exception:
        pass

    return None


def get_direct_video_cobalt(tweet_url):
    """
    Usa instâncias públicas abertas do Cobalt sem JWT
    """
    try:
        match = re.search(r"/status/(\d+)", tweet_url)
        clean_url = f"https://x.com/i/status/{match.group(1)}" if match else tweet_url

        payload = {"url": clean_url, "videoQuality": "480"}
        headers = {
            "Accept": "application/json",
            "Content-Type": "application/json",
            "User-Agent": "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36"
        }

        cobalt_instances = [
            "https://cobalt-api.kwippy.com/",
            "https://api.cobalt.lol/"
        ]

        for instance in cobalt_instances:
            try:
                res = requests.post(instance, json=payload, headers=headers, timeout=3.5)
                if res.status_code == 200:
                    data = res.json()
                    status = data.get("status")
                    if status in ["tunnel", "redirect"]:
                        return data.get("url")
                    elif status == "picker" and data.get("picker"):
                        for item in data.get("picker"):
                            if item.get("type") == "video" and item.get("url"):
                                return item.get("url")
            except Exception:
                continue
    except Exception:
        pass

    return None


def resolve_twitter_video(tweet_link, base_instance):
    """
    Retorna dict {url, thumb, width, height} usando:
    1. VxTwitter API (traz mp4 + dimensões reais)
    2. Cobalt API
    3. Scraping Nitter/Shitter
    """
    match = re.search(r"/status/(\d+)", tweet_link)
    tweet_id = match.group(1) if match else None

    if tweet_id:
        vx_data = get_video_vxtwitter(tweet_id)
        if vx_data:
            return vx_data

    cobalt_url = get_direct_video_cobalt(tweet_link)
    if cobalt_url:
        return {"url": cobalt_url, "thumb": "", "width": "", "height": ""}

    try:
        headers = {"User-Agent": "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0"}
        parsed = urlparse(tweet_link)
        target_url = f"{base_instance}{parsed.path}"

        res = requests.get(target_url, headers=headers, timeout=3.5)
        if res.status_code == 200:
            soup = BeautifulSoup(res.text, "html.parser")
            video_tag = soup.find("video")
            if video_tag:
                v_url = video_tag.get("src") or (video_tag.find("source").get("src") if video_tag.find("source") else None)
                poster = video_tag.get("poster") or ""
                if v_url:
                    if v_url.startswith("/"):
                        v_url = f"{base_instance}{v_url}"
                    if poster.startswith("/"):
                        poster = f"{base_instance}{poster}"
                    return {"url": unquote(v_url), "thumb": unquote(poster), "width": "", "height": ""}

            mp4_matches = re.findall(r'https?://video\.twimg\.com/[^"\']+\.mp4', res.text)
            if mp4_matches:
                return {"url": mp4_matches[0], "thumb": "", "width": "", "height": ""}

    except Exception:
        pass

    return None


def formate(items, base_instance="https://nitter.meowing.monster", default_query=""):
    posts_formatados = []

    if base_instance.startswith("http://"):
        base_instance = base_instance.replace("http://", "https://")

    for item in items:
        if not isinstance(item, dict):
            continue

        title_raw = item.get("title", "")
        creator = item.get("creator") or item.get("author") or ""
        link = item.get("link", "")
        description_raw = item.get("description", "")
        pub_date_raw = item.get("pubDate", "")

        if link.startswith("http://"):
            link = link.replace("http://", "https://")

        username = creator.strip()
        if not username:
            parsed_link = urlparse(link)
            parts = [p for p in parsed_link.path.split("/") if p]
            if parts:
                username = parts[0]
            else:
                username = "twitter_user"

        clean_user = username.lstrip("@").strip()
        username_formatted = f"@{clean_user}"

        avatar_url = f"https://unavatar.io/twitter/{clean_user}"

        soup = BeautifulSoup(title_raw or description_raw, "html.parser")
        text_post = soup.get_text()
        text_post = unicodedata.normalize("NFKC", text_post)
        text_post = "\n".join(
            line.strip() for line in text_post.splitlines() if line.strip()
        ).strip()

        media_list = []

        if description_raw:
            desc_soup = BeautifulSoup(description_raw, "html.parser")

            for img in desc_soup.find_all("img"):
                src = img.get("src")
                if src:
                    if src.startswith("//"):
                        src = f"https:{src}"
                    elif src.startswith("/"):
                        src = f"{base_instance}{src}"
                    elif src.startswith("http://"):
                        src = src.replace("http://", "https://")

                    src = unquote(src)
                    img_w = str(img.get("width") or "")
                    img_h = str(img.get("height") or "")

                    is_video_thumb = (
                        "video_thumb" in src
                        or "amplify_video" in src
                        or "ext_tw_video" in src
                        or "/card_img/" in src
                    )

                    if is_video_thumb:
                        vid_info = resolve_twitter_video(link, base_instance)

                        if vid_info and vid_info.get("url"):
                            v_w = vid_info.get("width") or img_w
                            v_h = vid_info.get("height") or img_h
                            thumb = vid_info.get("thumb") or src
                            media_list.append({
                                "media_url": vid_info["url"],
                                "thumbnail": thumb,
                                "thumbnail_url": thumb,
                                "media_type": "video",
                                "width": v_w,
                                "height": v_h,
                                "heigth": v_h
                            })
                        else:
                            media_list.append({
                                "media_url": src,
                                "media_type": "image",
                                "width": img_w,
                                "height": img_h,
                                "heigth": img_h
                            })
                    else:
                        media_list.append({
                            "media_url": src,
                            "media_type": "image",
                            "width": img_w,
                            "height": img_h,
                            "heigth": img_h
                        })

        # Fallback: quando o RSS omite <img> mas inclui tag <video> ou link de vídeo no HTML/texto
        if not media_list and (
            "<video" in description_raw
            or "video.twimg.com" in description_raw
            or "pic.twitter.com" in text_post
            or "https://t.co/" in text_post
        ):
            vid_info = resolve_twitter_video(link, base_instance)
            if vid_info and vid_info.get("url"):
                v_w = vid_info.get("width") or ""
                v_h = vid_info.get("height") or ""
                thumb = vid_info.get("thumb") or ""
                media_list.append({
                    "media_url": vid_info["url"],
                    "thumbnail": thumb,
                    "thumbnail_url": thumb,
                    "media_type": "video",
                    "width": v_w,
                    "height": v_h,
                    "heigth": v_h
                })

        media_url = media_list[0]["media_url"] if media_list else None
        media_type = media_list[0]["media_type"] if media_list else "image"
        media_thumb = media_list[0].get("thumbnail_url", "") if media_list else ""
        width = media_list[0].get("width", "") if media_list else ""
        height = media_list[0].get("height", "") if media_list else ""

        datetime_formatado = None
        if pub_date_raw:
            try:
                from email.utils import parsedate_to_datetime
                dt_obj = parsedate_to_datetime(pub_date_raw)
                datetime_formatado = dt_obj.strftime("%Y-%m-%d %H:%M:%S")
            except Exception:
                datetime_formatado = str(pub_date_raw)

        if len(media_list) > 1:
            links_formatados = "".join(f"\n[{m['media_type'].upper()}]{m['media_url']}" for m in media_list[1:])
            text_final = f"{text_post}{links_formatados}".strip()
        else:
            text_final = text_post

        unique_id = str(hash(link))
        match_id = re.search(r"/status/(\d+)", link)
        if match_id:
            unique_id = match_id.group(1)

        if text_final or media_url:
            posts_formatados.append(
                {
                    "username": username_formatted,
                    "account_id": clean_user,
                    "text_post": text_final,
                    "datetime": datetime_formatado,
                    "relative_datetime": get_relative_time(datetime_formatado),
                    "comment_count": 0,
                    "avatar_url": avatar_url,
                    "media": {
                        "media_url": media_url or "",
                        "media_type": media_type,
                        "thumbnail": media_thumb,
                        "thumbnail_url": media_thumb,
                        "width": width,
                        "height": height,
                        "heigth": height
                    },
                    "media_list": media_list,
                    "media_attachments": media_list,
                    "origin_server": "https://linka-project.onrender.com/feed/twitter",
                    "platform": "twitter",
                    "unique_id": unique_id,
                    "original_url": link,
                }
            )

    return posts_formatados


def fetch_twitter_search_fast(query="android", limit=20):
    instances = get_active_shitter_instances()
    encoded_query = quote(query.strip())

    headers = {
        "User-Agent": "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "en-US,en;q=0.5",
    }

    raw_items = []

    for instance in instances:
        try:
            url = f"{instance}/search/rss?f=tweets&q={encoded_query}"
            response = requests.get(url, headers=headers, timeout=3.5)

            if response.status_code == 200 and ("<rss" in response.text or "<feed" in response.text):
                root = ET.fromstring(response.text)
                channel = root.find("channel")
                items = channel.findall("item") if channel is not None else root.findall(".//item")

                for item in items[:limit]:
                    raw_items.append({
                        "title": item.findtext("title", ""),
                        "link": item.findtext("link", ""),
                        "pubDate": item.findtext("pubDate", ""),
                        "description": item.findtext("description", ""),
                        "creator": item.findtext("{http://purl.org/dc/elements/1.1/}creator", "")
                    })

                if raw_items:
                    return formate(raw_items, base_instance=instance, default_query=query)[:limit]

        except Exception:
            continue

    return []


def fetch_twitter_user_posts(username="NASA", limit=20):
    instances = get_active_shitter_instances()
    clean_user = username.lstrip("@").strip()

    headers = {
        "User-Agent": "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    }

    raw_items = []

    for instance in instances:
        try:
            url = f"{instance}/{clean_user}/rss"
            response = requests.get(url, headers=headers, timeout=3.5)

            if response.status_code == 200 and ("<rss" in response.text or "<feed" in response.text):
                root = ET.fromstring(response.text)
                channel = root.find("channel")
                items = channel.findall("item") if channel is not None else root.findall(".//item")

                for item in items[:limit]:
                    raw_items.append({
                        "title": item.findtext("title", ""),
                        "link": item.findtext("link", ""),
                        "pubDate": item.findtext("pubDate", ""),
                        "description": item.findtext("description", ""),
                        "creator": clean_user
                    })

                if raw_items:
                    return formate(raw_items, base_instance=instance)[:limit]

        except Exception:
            continue

    return []


# --- ROTAS FLASK PARA O LINKA ---

@twitter_bp.route("/feed/twitter", methods=["GET"])
def get_twitter_feed():
    username = request.args.get("user", default="NASA", type=str)
    limit = request.args.get("limit", default=20, type=int)
    posts = fetch_twitter_user_posts(username=username, limit=limit)
    return jsonify(posts)


@twitter_bp.route("/feed/twitter/search", methods=["GET"])
def get_twitter_search_feed():
    query = request.args.get("q", default="android", type=str)
    limit = request.args.get("limit", default=20, type=int)
    posts = fetch_twitter_search_fast(query=query, limit=limit)
    return jsonify(posts)