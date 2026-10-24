import json
import os
import re
import unicodedata
from datetime import datetime
from pathlib import Path
from urllib.parse import urlparse
from bs4 import BeautifulSoup
import dotenv
import requests

BASE_DIR = Path(__file__).resolve().parent.parent
env_path = BASE_DIR / ".env"
dotenv.load_dotenv(dotenv_path=env_path)

MASTODON_KEY = os.getenv("MASTODON_KEY", "").strip()


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


def fetch_mastodon_user_avatar(username, instance="mastodon.social"):
    clean_username = username.lstrip("@")
    if "@" in clean_username:
        clean_username = clean_username.split("@")[0]

    url = f"https://{instance}/api/v1/accounts/lookup"
    headers = {"User-Agent": "LinkaLiteApp/1.0"}

    if MASTODON_KEY:
        headers["Authorization"] = f"Bearer {MASTODON_KEY}"

    try:
        response = requests.get(
            url, headers=headers, params={"acct": clean_username}, timeout=5
        )
        if response.status_code == 200:
            data = response.json()
            return data.get("avatar_static") or data.get("avatar") or ""
    except Exception as e:
        print(f"[MASTODON AVATAR EXCEPTION] {e}")

    return ""


def formate(posts, instance="mastodon.social"):
    if isinstance(posts, str):
        try:
            posts = json.loads(posts)
        except json.JSONDecodeError:
            return []

    if isinstance(posts, dict):
        if "error" in posts:
            return []
        posts = [posts]

    posts_formatados = []

    for post in posts:
        if not isinstance(post, dict) or "error" in post:
            continue

        target_post = (
            post.get("reblog")
            if isinstance(post.get("reblog"), dict)
            else post
        )

        account_info = target_post.get("account") or {}
        username = (
            account_info.get("acct")
            or account_info.get("username")
            or "desconhecido"
        )

        account_id = str(account_info.get("id", ""))

        datetime_formatado = None
        raw_created_at = target_post.get("created_at")
        if raw_created_at:
            try:
                dt = datetime.fromisoformat(
                    raw_created_at.replace("Z", "+00:00")
                )
                datetime_formatado = dt.strftime("%Y-%m-%d %H:%M:%S")
            except ValueError:
                datetime_formatado = raw_created_at

        html_content = target_post.get("content", "")
        
        html_content = (
            html_content.replace("<br />", "\n")
            .replace("<br>", "\n")
            .replace("</p>", "\n")
        )

        soup = BeautifulSoup(html_content, "html.parser")

        for hashtag_element in soup.find_all("a", class_="hashtag"):
            hashtag_element.decompose()

        text_post = soup.get_text()

        text_post = unicodedata.normalize("NFKC", text_post)
        text_post = "\n".join(
            line.strip() for line in text_post.splitlines() if line.strip()
        ).strip()

        media_url = None
        media_type = None
        width = None
        height = None

        images_for_v2 = []
        
        for media in target_post.get("media_attachments", []):
            if isinstance(media, dict) and media.get("type") == "image":
                img_url = media.get("url") or media.get("preview_url")
                if img_url:
                    images_for_v2.append(img_url)
                    
                    if not media_url:
                        media_url = img_url
                        media_type = "image"
                        
                        meta_original = media.get("meta", {}).get("original", {})
                        if meta_original.get("width"):
                            width = str(meta_original.get("width"))
                        if meta_original.get("height"):
                            height = str(meta_original.get("height"))

        avatar_url = (
            account_info.get("avatar_static") 
            or account_info.get("avatar") 
            or ""
        )

        if images_for_v2:
            links_formatados = "".join(f"[IMAGE]{url}\n" for url in images_for_v2)
            text_final = f"{text_post}\n{links_formatados}".strip()
        else:
            text_final = text_post

        raw_replies = target_post.get("replies_count")
        try:
            comments_count = int(raw_replies) if raw_replies is not None else 0
        except (ValueError, TypeError):
            comments_count = 0

        original_url = target_post.get("url", "")

        if text_final or media_url:
            posts_formatados.append(
                {
                    "username": (
                        f"@{username}"
                        if not username.startswith("@")
                        else username
                    ),
                    "account_id": account_id,
                    "text_post": text_final,
                    "datetime": datetime_formatado,
                    "relative_datetime": get_relative_time(datetime_formatado),
                    "comment_count": comments_count,
                    "avatar_url": avatar_url,
                    "media": {
                        "media_url": media_url,
                        "media_type": media_type,
                        "width": width,
                        "heigth": height
                    },
                    "origin_server": "https://linkaProject.pythonanywhere.com/feed/mastodon",
                    "platform": "mastodon",
                    "unique_id": str(target_post.get("id")),
                    "original_url": original_url,
                }
            )

    return posts_formatados


def fetch_mastodon_posts(
    tag="retrocomputing", instance="mastodon.social", limit=100
):
    url = f"https://{instance}/api/v1/timelines/tag/{tag}"
    headers = {"User-Agent": "LinkaLiteApp/1.0"}

    if MASTODON_KEY:
        headers["Authorization"] = f"Bearer {MASTODON_KEY}"

    raw_posts = []
    max_id = None

    while len(raw_posts) < limit:
        fetch_limit = min(limit - len(raw_posts), 40)
        params = {"limit": fetch_limit}

        if max_id:
            params["max_id"] = max_id

        try:
            response = requests.get(
                url, headers=headers, params=params, timeout=10
            )

            if response.status_code != 200:
                print(f"[MASTODON ERROR] Status Code: {response.status_code}")
                break

            data = response.json()
            if not isinstance(data, list) or not data:
                break

            raw_posts.extend(data)
            max_id = data[-1].get("id")

        except Exception as e:
            print(f"[MASTODON EXCEPTION] {e}")
            break

    formatted_posts = formate(raw_posts, instance=instance)
    return formatted_posts[:limit]


def fetch_mastodon_comments(original_post_id, instance="mastodon.social", original_url=None):
    if not original_post_id:
        print("[MASTODON COMMENTS ERROR] ID do post não foi informado.")
        return []

    clean_id = str(original_post_id).strip().replace('"', '')
    target_instance = instance

    if original_url:
        parsed_url = urlparse(original_url)
        if parsed_url.netloc:
            target_instance = parsed_url.netloc
            url_id = parsed_url.path.split("/")[-1]
            if url_id.isdigit():
                clean_id = url_id

    url = f"https://{target_instance}/api/v1/statuses/{clean_id}/context"
    headers = {"User-Agent": "LinkaLiteApp/1.0"}

    if MASTODON_KEY and target_instance == instance:
        headers["Authorization"] = f"Bearer {MASTODON_KEY}"

    try:
        response = requests.get(url, headers=headers, timeout=10)

        if response.status_code != 200 and target_instance != instance:
            print(f"[MASTODON COMMENTS FALLBACK] Falhou em {target_instance}. Tentando {instance}...")
            url = f"https://{instance}/api/v1/statuses/{original_post_id}/context"
            clean_id = str(original_post_id)
            response = requests.get(url, headers=headers, timeout=10)

        if response.status_code != 200:
            print(f"[MASTODON COMMENTS ERROR] Status {response.status_code} na URL: {url}")
            return []

        data = response.json()
        descendants = data.get("descendants", [])

        print(f"[DEBUG MASTODON] A API achou {len(descendants)} comentários para o post {clean_id} em {target_instance}!")

        comments_formatados = []
        for item in descendants:
            account_info = item.get("account") or {}
            username = account_info.get("acct") or account_info.get("username") or "Anonimo"

            html_content = item.get("content", "")
            html_content = html_content.replace("<br />", "\n").replace("<br>", "\n").replace("</p>", "\n")
            soup = BeautifulSoup(html_content, "html.parser")

            for hashtag_element in soup.find_all("a", class_="hashtag"):
                hashtag_element.decompose()

            text_comment = soup.get_text()
            text_comment = unicodedata.normalize("NFKC", text_comment).strip()

            comments_formatados.append({
                "comment_id": item.get("id"),
                "post_id": clean_id,
                "username": f"@{username}" if not username.startswith("@") else username,
                "text_comment": text_comment
            })

        return comments_formatados

    except Exception as e:
        print(f"[MASTODON COMMENTS EXCEPTION] {e}")
        return []