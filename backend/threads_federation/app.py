import json
import os
import unicodedata
from datetime import datetime
from pathlib import Path
from bs4 import BeautifulSoup
import dotenv
from flask import jsonify, request, Blueprint
import requests

BASE_DIR = Path(__file__).resolve().parent.parent
env_path = BASE_DIR / ".env"
dotenv.load_dotenv()

THREADS_APP_ID = os.getenv("THREADS_APP_ID", "").strip()
THREADS_APP_SECRET = os.getenv("THREADS_APP_SECRET", "").strip()
THREADS_ACCESS_TOKEN = os.getenv("THREADS_ACCESS_TOKEN", "").strip()

threads_bp = Blueprint("threads_bp", __name__)


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


def formate(posts):
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

        account_info = post.get("owner", {}) or post.get("user", {})
        username = post.get("username") or account_info.get("username") or "desconhecido"
        account_id = str(account_info.get("id", post.get("owner_id", "")))

        datetime_formatado = None
        raw_created_at = post.get("timestamp") or post.get("created_at") or post.get("taken_at")
        if raw_created_at:
            if isinstance(raw_created_at, (int, float)):
                dt = datetime.fromtimestamp(raw_created_at)
                datetime_formatado = dt.strftime("%Y-%m-%d %H:%M:%S")
            else:
                try:
                    dt = datetime.fromisoformat(str(raw_created_at).replace("Z", "+00:00"))
                    datetime_formatado = dt.strftime("%Y-%m-%d %H:%M:%S")
                except ValueError:
                    datetime_formatado = str(raw_created_at)

        caption_obj = post.get("caption")
        if isinstance(caption_obj, dict):
            raw_text = caption_obj.get("text", "")
        else:
            raw_text = post.get("text", "") or caption_obj or ""

        soup = BeautifulSoup(raw_text, "html.parser")
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
        media_list = []

        children = post.get("children", {}).get("data", [])
        media_elements = children if children else [post]

        for elem in media_elements:
            m_type = str(elem.get("media_type", "")).upper()
            img_candidates = elem.get("image_versions2", {}).get("candidates", [])
            img_url = elem.get("media_url") or (img_candidates[0].get("url") if img_candidates else None)
            
            if img_url:
                images_for_v2.append(img_url)

                m_w = str(elem.get("width", "")) if elem.get("width") else ""
                m_h = str(elem.get("height", "")) if elem.get("height") else ""

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

        avatar_url = (
            post.get("threads_profile_picture_url")
            or account_info.get("threads_profile_picture_url")
            or account_info.get("profile_pic_url")
            or ""
        )

        if images_for_v2:
            links_formatados = "".join(f"\n[IMAGE]{url}" for url in images_for_v2)
            text_final = f"{text_post}{links_formatados}".strip()
        else:
            text_final = text_post

        comments_count = post.get("replies_count", 0)
        original_url = post.get("permalink", "")
        if not original_url and post.get("code"):
            original_url = f"https://www.threads.net/@{username}/post/{post.get('code')}"

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
                    "media_list": media_list,
                    "media_attachments": media_list,
                    "origin_server": "https://linkaProject.pythonanywhere.com/feed/threads",
                    "platform": "threads",
                    "unique_id": str(post.get("id")),
                    "original_url": original_url,
                }
            )

    return posts_formatados


def fetch_threads_posts(threads_user_id="me", limit=100, access_token=None):
    token = access_token or THREADS_ACCESS_TOKEN
    if not token:
        print("[THREADS ERROR] Token de acesso não configurado no .env.")
        return []

    url = f"https://graph.threads.net/v1.0/{threads_user_id}/threads"
    fields = (
        "id,media_product_type,media_type,media_url,permalink,owner,username,"
        "text,timestamp,children{id,media_url,media_type}"
    )

    params = {
        "fields": fields,
        "limit": min(limit, 100),
        "access_token": token
    }

    try:
        response = requests.get(url, params=params, timeout=10)
        if response.status_code != 200:
            print(f"[THREADS ERROR] Status Code: {response.status_code} Response: {response.text}")
            return []

        data = response.json()
        raw_posts = data.get("data", [])
        return formate(raw_posts)[:limit]

    except Exception as e:
        print(f"[THREADS EXCEPTION] {e}")
        return []

def fetch_threads_search_fast(query="brasil", limit=20):
    """
    Scraping HTTP limpo via extração de JSON da página web pública do Threads.
    Consome < 5MB de RAM e não estoura a memória na Render.
    """
    tag_clean = query.lstrip("#").strip().lower()
    url = f"https://www.threads.net/search?q={tag_clean}"
    
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8",
        "Accept-Language": "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7",
        "Sec-Fetch-Dest": "document",
        "Sec-Fetch-Mode": "navigate",
        "Sec-Fetch-Site": "none",
        "Sec-Fetch-User": "?1",
        "Upgrade-Insecure-Requests": "1"
    }

    try:
        response = requests.get(url, headers=headers, timeout=10)
        if response.status_code == 200:
            soup = BeautifulSoup(response.text, "html.parser")
            scripts = soup.find_all("script", {"type": "application/json"})
            raw_posts = []

            for script in scripts:
                content = script.string or ""
                if "thread_items" in content or "searchResults" in content:
                    try:
                        data = json.loads(content)
                        # Função recursiva para vasculhar nós de post dentro da árvore JSON
                        def find_posts(obj):
                            if isinstance(obj, dict):
                                if "thread_items" in obj and isinstance(obj["thread_items"], list):
                                    for item in obj["thread_items"]:
                                        if isinstance(item, dict) and "post" in item:
                                            raw_posts.append(item["post"])
                                for k, v in obj.items():
                                    find_posts(v)
                            elif isinstance(obj, list):
                                for item in obj:
                                    find_posts(item)

                        find_posts(data)
                    except json.JSONDecodeError:
                        continue

            if raw_posts:
                return formate(raw_posts)[:limit]
        else:
            print(f"[THREADS SCRAPE SEARCH] Status Code: {response.status_code}")
    except Exception as e:
        print(f"[THREADS SCRAPE SEARCH EXCEPTION] {e}")

    # Fallback local seguro caso a raspagem web seja bloqueada
    all_posts = fetch_threads_posts(limit=100)
    return [p for p in all_posts if query.lower() in p.get("text_post", "").lower()][:limit]
def fetch_threads_comments(original_post_id, access_token=None):
    token = access_token or THREADS_ACCESS_TOKEN
    if not original_post_id or not token:
        print("[THREADS COMMENTS ERROR] ID do post ou Token não informado.")
        return []

    clean_id = str(original_post_id).strip().replace('"', '')
    url = f"https://graph.threads.net/v1.0/{clean_id}/replies"
    fields = "id,username,text,timestamp,owner"

    params = {
        "fields": fields,
        "access_token": token
    }

    try:
        response = requests.get(url, params=params, timeout=10)
        if response.status_code != 200:
            print(f"[THREADS COMMENTS ERROR] Status {response.status_code} na URL: {url}")
            return []

        data = response.json()
        replies = data.get("data", [])

        comments_formatados = []
        for item in replies:
            username = item.get("username") or item.get("owner", {}).get("username") or "Anonimo"
            text_comment = item.get("text", "").strip()
            text_comment = unicodedata.normalize("NFKC", text_comment)

            comments_formatados.append({
                "comment_id": item.get("id"),
                "post_id": clean_id,
                "username": f"@{username}" if not username.startswith("@") else username,
                "text_comment": text_comment
            })

        return comments_formatados

    except Exception as e:
        print(f"[THREADS COMMENTS EXCEPTION] {e}")
        return []


# --- ROTAS FLASK PARA O LINKA ---

@threads_bp.route("/feed/threads", methods=["GET"])
def get_threads_feed():
    limit = request.args.get("limit", default=20, type=int)
    posts = fetch_threads_posts(limit=limit)
    return jsonify(posts)


@threads_bp.route("/feed/threads/search", methods=["GET"])
def get_threads_search_feed():
    query = request.args.get("q", default="brasil", type=str)
    limit = request.args.get("limit", default=20, type=int)
    
    posts = fetch_threads_search_fast(query=query, limit=limit)
    return jsonify(posts)


@threads_bp.route("/feed/threads/comments", methods=["GET"])
def get_threads_comments():
    post_id = request.args.get("post_id", type=str)
    if not post_id:
        return jsonify({"error": "Parametro post_id é obrigatório"}), 400

    comments = fetch_threads_comments(original_post_id=post_id)
    return jsonify(comments)