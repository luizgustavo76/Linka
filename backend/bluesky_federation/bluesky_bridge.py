import os
import unicodedata
from datetime import datetime
import requests
from pathlib import Path
from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent
load_dotenv(os.path.join(BASE_DIR, ".env"))
BSKY_HANDLE = os.getenv("BSKY_HANDLE")
BSKY_APP_PASSWORD = os.getenv("BSKY_APP_PASSWORD")


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


def get_bluesky_token():
    url = "https://bsky.social/xrpc/com.atproto.server.createSession"
    payload = {
        "identifier": BSKY_HANDLE,
        "password": BSKY_APP_PASSWORD,
    }
    headers = {"Content-Type": "application/json"}
    try:
        response = requests.post(
            url, json=payload, headers=headers, timeout=10
        )
        if response.status_code == 200:
            return response.json().get("accessJwt")
        print(f"[BLUESKY AUTH ERROR] Status: {response.status_code} {response.text}")
        return None
    except Exception as e:
        print(f"[BLUESKY AUTH EXCEPTION] {e}")
        return None


def formate_bluesky(posts):
    posts_formatados = []

    for post in posts:
        if not isinstance(post, dict):
            continue

        author_info = post.get("author") or {}
        username = (
            author_info.get("handle")
            or author_info.get("displayName")
            or "anonimo"
        )

        record = post.get("record") or {}

        text_post = record.get("text") or post.get("text") or ""
        text_post = unicodedata.normalize("NFKC", text_post)
        text_post = "\n".join(
            line.strip() for line in text_post.splitlines() if line.strip()
        ).strip()

        raw_created_at = (
            record.get("createdAt")
            or post.get("indexedAt")
            or post.get("createdAt")
        )
        datetime_formatado = None
        if raw_created_at:
            try:
                dt = datetime.fromisoformat(
                    str(raw_created_at).replace("Z", "+00:00")
                )
                datetime_formatado = dt.strftime("%Y-%m-%d %H:%M:%S")
            except Exception:
                datetime_formatado = str(raw_created_at)

        media_url = None
        media_type = None
        width = None
        height = None

        images_for_v2 = []
        media_list = []

        embed = post.get("embed") or {}
        record_embed = record.get("embed") or {}

        imgs = (
            embed.get("images")
            or record_embed.get("images")
            or embed.get("media", {}).get("images")
            or []
        )

        for img in imgs:
            if isinstance(img, dict):
                img_url = (
                    img.get("fullsize")
                    or img.get("thumb")
                    or img.get("image", {}).get("ref", {}).get("$link")
                )

                if img_url and not img_url.startswith("http"):
                    did = author_info.get("did")
                    img_url = f"https://cdn.bsky.app/img/feed_fullsize/plain/{did}/{img_url}@jpeg"

                if img_url:
                    images_for_v2.append(img_url)

                    aspect_ratio = img.get("aspectRatio") or img.get("image", {}).get("aspectRatio") or {}
                    m_w = str(aspect_ratio.get("width", "")) if aspect_ratio.get("width") else ""
                    m_h = str(aspect_ratio.get("height", "")) if aspect_ratio.get("height") else ""

                    media_list.append({
                        "media_url": img_url,
                        "media_type": "image",
                        "width": m_w,
                        "heigth": m_h
                    })

                    if not media_url:
                        media_url = img_url
                        media_type = "image"
                        width = m_w
                        height = m_h

        if not text_post and not images_for_v2:
            continue

        if images_for_v2:
            links_formatados = "".join(f"\n[IMAGE]{url}" for url in images_for_v2)
            text_final = f"{text_post}{links_formatados}".strip()
        else:
            text_final = text_post

        raw_replies = post.get("replyCount")
        try:
            comments_count = int(raw_replies) if raw_replies is not None else 0
        except (ValueError, TypeError):
            comments_count = 0

        posts_formatados.append(
            {
                "username": (
                    f"@{username}"
                    if not username.startswith("@")
                    else username
                ),
                "text_post": text_final,
                "datetime": datetime_formatado,
                "relative_datetime": get_relative_time(datetime_formatado),
                "comment_count": comments_count,
                "media": {
                    "media_url": media_url,
                    "media_type": media_type,
                    "width": width,
                    "heigth": height
                },
                "media_list": media_list,
                "media_attachments": media_list,
                "platform": "bluesky",
                "origin_server": "https://linkaProject.pythonanywhere.com/feed/bluesky"
            }
        )

    return posts_formatados


def fetch_bluesky_posts(query="retrocomputing", limit=40):
    token = get_bluesky_token()
    if not token:
        print("[BLUESKY] Falha de autenticação. Verifique .env")
        return []

    url = "https://bsky.social/xrpc/app.bsky.feed.searchPosts"
    headers = {
        "Authorization": f"Bearer {token}",
    }
    params = {"q": query, "limit": limit}
    try:
        response = requests.get(
            url, headers=headers, params=params, timeout=10
        )
        if response.status_code != 200:
            print(f"[BLUESKY SEARCH ERROR] Status: {response.status_code} {response.text}")
            return []

        data = response.json()
        return formate_bluesky(data.get("posts", []))
    except Exception as e:
        print(f"[BLUESKY EXCEPTION] {e}")
        return []