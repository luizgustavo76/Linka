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
dotenv.load_dotenv(dotenv_path=env_path)

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


def fetch_threads_user_avatar(threads_user_id, access_token=None):
    token = access_token or THREADS_ACCESS_TOKEN
    if not token or not threads_user_id:
        return ""

    url = f"https://graph.threads.net/v1.0/{threads_user_id}"
    params = {
        "fields": "id,username,threads_profile_picture_url",
        "access_token": token,
    }

    try:
        response = requests.get(url, params=params, timeout=5)
        if response.status_code == 200:
            data = response.json()
            return data.get("threads_profile_picture_url", "")
    except Exception as e:
        print(f"[THREADS AVATAR EXCEPTION] {e}")

    return ""


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

        account_info = post.get("owner", {})
        username = post.get("username") or account_info.get("username") or "desconhecido"
        account_id = str(account_info.get("id", post.get("owner_id", "")))

        datetime_formatado = None
        raw_created_at = post.get("timestamp") or post.get("created_at")
        if raw_created_at:
            try:
                dt = datetime.fromisoformat(raw_created_at.replace("Z", "+00:00"))
                datetime_formatado = dt.strftime("%Y-%m-%d %H:%M:%S")
            except ValueError:
                datetime_formatado = raw_created_at

        raw_text = post.get("text", "") or post.get("caption", "")

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
            m_type = elem.get("media_type", "").upper()
            if m_type in ["IMAGE", "CAROUSEL_ALBUM"] or elem.get("media_url"):
                img_url = elem.get("media_url")
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
            or ""
        )

        if images_for_v2:
            links_formatados = "".join(f"\n[IMAGE]{url}" for url in images_for_v2)
            text_final = f"{text_post}{links_formatados}".strip()
        else:
            text_final = text_post

        comments_count = post.get("replies_count", 0)
        original_url = post.get("permalink", "")

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
            print(f"[THREADS ERROR] Status Code: {response.status_code} - Response: {response.text}")
            return []

        data = response.json()
        raw_posts = data.get("data", [])
        print(f"[THREADS DEBUG] Quantidade de posts brutos retornados: {len(raw_posts)}")

        formatted_posts = formate(raw_posts)
        return formatted_posts[:limit]

    except Exception as e:
        print(f"[THREADS EXCEPTION] {e}")
        return []


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
# --- BUSCA DE POSTS PUBLICOS (FEED DE TOPICOS/HASHTAGS) ---

def fetch_threads_search_feed(query="tech", limit=20, access_token=None):
    token = access_token or THREADS_ACCESS_TOKEN
    if not token:
        print("[THREADS SEARCH ERROR] Token não configurado.")
        return []

    url = "https://graph.threads.net/v1.0/threads/search"
    fields = (
        "id,media_product_type,media_type,media_url,permalink,owner,username,"
        "text,timestamp,children{id,media_url,media_type}"
    )

    params = {
        "q": query,
        "fields": fields,
        "limit": min(limit, 50),
        "access_token": token
    }

    try:
        response = requests.get(url, params=params, timeout=10)

        if response.status_code != 200:
            print(f"[THREADS SEARCH ERROR] Status: {response.status_code} - {response.text}")
            return []

        data = response.json()
        raw_posts = data.get("data", [])
        return formate(raw_posts)

    except Exception as e:
        print(f"[THREADS SEARCH EXCEPTION] {e}")
        return []


# Nova Rota para buscar o Feed de Tópicos
@threads_bp.route("/feed/threads/search", methods=["GET"])
def get_threads_search_feed():
    query = request.args.get("q", default="brasil", type=str)
    limit = request.args.get("limit", default=20, type=int)
    
    posts = fetch_threads_search_feed(query=query, limit=limit)
    return jsonify(posts)

# --- ROTAS FLASK PARA O LINKA ---

@threads_bp.route("/feed/threads", methods=["GET"])
def get_threads_feed():
    limit = request.args.get("limit", default=20, type=int)
    posts = fetch_threads_posts(limit=limit)
    return jsonify(posts)


@threads_bp.route("/feed/threads/comments", methods=["GET"])
def get_threads_comments():
    post_id = request.args.get("post_id", type=str)
    if not post_id:
        return jsonify({"error": "Parametro post_id é obrigatório"}), 400

    comments = fetch_threads_comments(original_post_id=post_id)
    return jsonify(comments)