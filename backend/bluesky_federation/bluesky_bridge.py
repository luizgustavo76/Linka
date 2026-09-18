import requests
import json
import unicodedata
from datetime import datetime
import urllib.parse

BSKY_HANDLE = "luizsgustavo76.bsky.social"
BSKY_APP_PASSWORD = "acas-tyic-vd47-i6ci"
PROXY_BASE = "http://linkaProject.pythonanywhere.com/lite-render?url="

def get_bluesky_token():
    url = "https://bsky.social/xrpc/com.atproto.server.createSession"
    payload = {
        "identifier": BSKY_HANDLE,
        "password": BSKY_APP_PASSWORD
    }
    headers = {
        "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36"
    }
    try:
        response = requests.post(url, json=payload, headers=headers, timeout=4)
        if response.status_code == 200:
            return response.json().get("accessJwt")
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
        username = author_info.get("handle") or author_info.get("displayName") or "anonimo"

        raw_created_at = post.get("record", {}).get("createdAt") or post.get("indexedAt")
        datetime_formatado = None
        if raw_created_at:
            try:
                dt = datetime.fromisoformat(raw_created_at.replace("Z", "+00:00"))
                datetime_formatado = dt.strftime("%Y-%m-%d %H:%M:%S")
            except Exception:
                datetime_formatado = raw_created_at

        record = post.get("record") or {}
        text_post = record.get("text", "")
        text_post = unicodedata.normalize("NFKC", text_post)
        text_post = "\n".join(line.strip() for line in text_post.splitlines()).strip()

        images = []
        embed = post.get("embed") or {}
        record_embed = record.get("embed") or {}

        imgs = embed.get("images") or record_embed.get("images") or []
        
        if not imgs and "media" in embed:
            imgs = embed.get("media", {}).get("images") or []

        for img in imgs:
            if isinstance(img, dict):
                img_url = img.get("fullsize") or img.get("thumb")
                if not img_url and "image" in img:
                    ref = img["image"].get("ref", {}).get("$link")
                    if ref:
                        img_url = f"https://cdn.bsky.app/img/feed_thumbnail/plain/did:plc:{author_info.get('did')}/{ref}@jpeg"
                if img_url:
                    images.append(img_url)

        if not text_post and not images:
            continue

        if images:
            links_formatados = "".join(
                f"[IMAGE]{PROXY_BASE}{urllib.parse.quote(url, safe='')}\n" 
                for url in images
            )
            text_final = f"{text_post}\n{links_formatados}".strip()
        else:
            text_final = text_post

        posts_formatados.append({
            "username": f"@{username}" if not username.startswith("@") else username,
            "text_post": text_final,
            "datetime": datetime_formatado
        })

    return posts_formatados

def fetch_bluesky_posts(query="retrocomputing", limit=40):
    token = get_bluesky_token()
    if not token:
        print("[BLUESKY] Falha de autenticação.")
        return []

    url = "https://bsky.social/xrpc/app.bsky.feed.searchPosts"
    headers = {
        "Authorization": f"Bearer {token}",
        "User-Agent": "LinkaLiteApp/1.0"
    }
    params = {'q': query, 'limit': limit}
    try:
        response = requests.get(url, headers=headers, params=params, timeout=4)
        if response.status_code != 200:
            return []

        data = response.json()
        return formate_bluesky(data.get("posts", []))
    except Exception as e:
        print(f"[BLUESKY EXCEPTION] {e}")
        return []