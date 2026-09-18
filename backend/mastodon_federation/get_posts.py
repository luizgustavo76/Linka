import requests
import json
import unicodedata
from bs4 import BeautifulSoup
from datetime import datetime

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

        target_post = post.get("reblog") if isinstance(post.get("reblog"), dict) else post

        account_info = target_post.get("account") or {}
        username = account_info.get("acct") or account_info.get("username") or "desconhecido"

        datetime_formatado = None
        raw_created_at = target_post.get("created_at")
        if raw_created_at:
            try:
                dt = datetime.fromisoformat(raw_created_at.replace("Z", "+00:00"))
                datetime_formatado = dt.strftime("%Y-%m-%d %H:%M:%S")
            except ValueError:
                datetime_formatado = raw_created_at

        html_content = target_post.get("content", "")
        html_content = html_content.replace("<br />", "\n").replace("<br>", "\n").replace("</p>", "\n")

        soup = BeautifulSoup(html_content, "html.parser")
        text_post = soup.get_text()

        text_post = unicodedata.normalize("NFKC", text_post)
        text_post = "\n".join(line.strip() for line in text_post.splitlines()).strip()

        images = []
        for media in target_post.get("media_attachments", []):
            if isinstance(media, dict) and media.get("type") == "image":
                img_url = media.get("url") or media.get("preview_url")
                if img_url:
                    images.append(img_url)

        if images:
            texto_com_imagens = text_post + "\n" + "".join(f"[IMAGE]{url}\n" for url in images)
            text_final = texto_com_imagens.strip()
        else:
            text_final = text_post

        if text_final or images:
            posts_formatados.append({
                "username": username,
                "text_post": text_final,
                "datetime": datetime_formatado
            })

    return posts_formatados

def fetch_mastodon_posts(tag="retrocomputing", instance="mastodon.social", limit=100):
    url = f"https://{instance}/api/v1/timelines/tag/{tag}"

    headers = {
        "Authorization": "Bearer DlvaQpDNhhb2ZvxjFsElKNQrWYUmpqGfM0v6oKrtUeM",
        "User-Agent": "LinkaLiteApp/1.0"
    }

    try:
        response = requests.get(url, headers=headers, params={'limit': limit}, timeout=10)
        
        if response.status_code != 200:
            print(f"[MASTODON ERROR] Status: {response.status_code} - Body: {response.text}")
            return []

        return formate(response.json())
    except Exception as e:
        print(f"[MASTODON EXCEPTION] {e}")
        return []