import json
import os
import unicodedata
from datetime import datetime
from pathlib import Path
from bs4 import BeautifulSoup
import dotenv
import requests

BASE_DIR = Path(__file__).resolve().parent.parent
env_path = BASE_DIR / ".env"
dotenv.load_dotenv(dotenv_path=env_path)

MASTODON_KEY = os.getenv("MASTODON_KEY", "").strip()


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
        
        # Guarda o ID da conta para buscar o avatar de forma independente
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
        text_post = soup.get_text()

        text_post = unicodedata.normalize("NFKC", text_post)
        text_post = "\n".join(
            line.strip() for line in text_post.splitlines()
        ).strip()

        images = []
        for media in target_post.get("media_attachments", []):
            if isinstance(media, dict) and media.get("type") == "image":
                img_url = media.get("url") or media.get("preview_url")
                if img_url:
                    images.append(img_url)

        if images:
            links_formatados = "".join(f"[IMAGE]{url}\n" for url in images)
            text_final = f"{text_post}\n{links_formatados}".strip()
        else:
            text_final = text_post

        if text_final or images:
            posts_formatados.append(
                {
                    "username": (
                        f"@{username}"
                        if not username.startswith("@")
                        else username
                    ),
                    "account_id": account_id,
                    "text_post": text_final,
                    "comments_count": target_post.get("replies_count", 0),
                    "datetime": datetime_formatado,
                    "origin_server": f"https://linkaProject.pythonanywhere.com/feed/mastodon",
                    "platform": "mastodon",
                    "unique_id": str(target_post.get("id")),
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


def fetch_mastodon_comments(original_post_id, instance="mastodon.social"):
    url = f"https://{instance}/api/v1/statuses/{original_post_id}/context"
    headers = {"User-Agent": "LinkaLiteApp/1.0"}

    if MASTODON_KEY:
        headers["Authorization"] = f"Bearer {MASTODON_KEY}"

    try:
        response = requests.get(url, headers=headers, timeout=10)
        if response.status_code != 200:
            return []

        data = response.json()
        descendants = data.get("descendants", [])
        return formate(descendants, instance=instance)

    except Exception as e:
        print(f"[MASTODON COMMENTS EXCEPTION] {e}")
        return []