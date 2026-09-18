import cloudscraper
import unicodedata
from datetime import datetime

BSKY_HANDLE = "luizsgustavo76.bsky.social"
BSKY_APP_PASSWORD = "acas-tyic-vd47-i6ci"

def get_bluesky_token():
    url = "https://bsky.social/xrpc/com.atproto.server.createSession"
    payload = {
        "identifier": BSKY_HANDLE,
        "password": BSKY_APP_PASSWORD
    }
    
    scraper = cloudscraper.create_scraper()
    try:
        response = scraper.post(url, json=payload, timeout=10)
        if response.status_code == 200:
            return response.json().get("accessJwt")
        print(f"[BLUESKY AUTH ERROR] Status: {response.status_code} - {response.text}")
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

        datetime_formatado = None
        raw_created_at = post.get("record", {}).get("createdAt") or post.get("indexedAt")
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
        
        images_list = embed.get("images") or embed.get("media", {}).get("images") or []
        for img in images_list:
            if isinstance(img, dict):
                img_url = img.get("fullsize") or img.get("thumb")
                if img_url:
                    images.append(img_url)

        if not text_post and not images:
            continue

        if images:
            texto_com_imagens = (text_post + "\n" if text_post else "") + "".join(f"[IMAGE]{url}\n" for url in images)
            text_final = texto_com_imagens.strip()
        else:
            text_final = text_post

        posts_formatados.append({
            "username": f"@{username}",
            "text_post": text_final,
            "datetime": datetime_formatado
        })

    return posts_formatados

def fetch_bluesky_posts(query="retrocomputing", limit=40):
    token = get_bluesky_token()
    if not token:
        print("[BLUESKY] Nao foi possivel autenticar. Abortando busca.")
        return []

    url = "https://bsky.social/xrpc/app.bsky.feed.searchPosts"
    headers = {
        "Authorization": f"Bearer {token}"
    }
    params = {
        'q': query,
        'limit': limit
    }

    scraper = cloudscraper.create_scraper()

    try:
        response = scraper.get(url, headers=headers, params=params, timeout=10)
        
        if response.status_code != 200:
            print(f"[BLUESKY ERROR] Status: {response.status_code} - {response.text[:200]}")
            return []

        data = response.json()
        posts_brutos = data.get("posts", [])
        
        print(f"[BLUESKY SUCCESS] Puxou {len(posts_brutos)} posts para a busca '{query}'.")
        return formate_bluesky(posts_brutos)

    except Exception as e:
        print(f"[BLUESKY EXCEPTION] {e}")
        return []