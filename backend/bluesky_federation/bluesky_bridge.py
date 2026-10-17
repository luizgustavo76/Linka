import os
import unicodedata
from datetime import datetime
import requests
from pathlib import Pat
from dotenv import load_dotenv

BASE_DIR = Path(__file__).resolve().parent
load_dotenv(os.path.join(BASE_DIR, ".env"))
BSKY_HANDLE = os.getenv("BSKY_HANDLE")
BSKY_APP_PASSWORD = os.getenv("BSKY_APP_PASSWORD")


def get_bluesky_token():
    url = "https://bsky.social/xrpc/com.atproto.server.createSession"
    payload = {
        "identifier": BSKY_HANDLE,
        "password": BSKY_APP_PASSWORD,
    }
    # Removido o User-Agent sintético de browser para evitar block no Cloudflare
    headers = {"Content-Type": "application/json"}
    try:
        response = requests.post(
            url, json=payload, headers=headers, timeout=10
        )
        if response.status_code == 200:
            return response.json().get("accessJwt")
        print(
            f"[BLUESKY AUTH ERROR] Status: {response.status_code} - {response.text}"
        )
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

        # O 'record' contem os dados puros gravados na rede AT Protocol
        record = post.get("record") or {}

        # Busca texto no record ou direto na view (fallback)
        text_post = record.get("text") or post.get("text") or ""
        text_post = unicodedata.normalize("NFKC", text_post)
        text_post = "\n".join(
            line.strip() for line in text_post.splitlines()
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

        images = []
        embed = post.get("embed") or {}
        record_embed = record.get("embed") or {}

        # Mapeamento estendido de imagens no Bluesky ATProto
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

                # Se for apenas a referencia CID/Link da imagem no Did
                if img_url and not img_url.startswith("http"):
                    did = author_info.get("did")
                    img_url = f"https://cdn.bsky.app/img/feed_fullsize/plain/{did}/{img_url}@jpeg"

                if img_url:
                    images.append(img_url)

        # Se não tiver texto nem imagem, pula
        if not text_post and not images:
            continue

        if images:
            links_formatados = "".join(f"\n[IMAGE]{url}" for url in images)
            text_final = f"{text_post}{links_formatados}".strip()
        else:
            text_final = text_post

        posts_formatados.append(
            {
                "username": (
                    f"@{username}"
                    if not username.startswith("@")
                    else username
                ),
                "text_post": text_final,
                "datetime": datetime_formatado,
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
            print(
                f"[BLUESKY SEARCH ERROR] Status: {response.status_code} - {response.text}"
            )
            return []

        data = response.json()
        return formate_bluesky(data.get("posts", []))
    except Exception as e:
        print(f"[BLUESKY EXCEPTION] {e}")
        return []

