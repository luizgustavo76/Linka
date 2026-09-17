import requests
import unicodedata
from datetime import datetime

def formate_bluesky(posts):
    posts_formatados = []
    
    for post in posts:
        if not isinstance(post, dict):
            continue

        # Informações do Autor no Bluesky
        author_info = post.get("author") or {}
        # O 'handle' no Bluesky é o username completo (ex: usuario.bsky.social)
        username = author_info.get("handle") or author_info.get("displayName") or "anonimo"

        # Formatação de Data
        datetime_formatado = None
        raw_created_at = post.get("record", {}).get("createdAt") or post.get("indexedAt")
        if raw_created_at:
            try:
                dt = datetime.fromisoformat(raw_created_at.replace("Z", "+00:00"))
                datetime_formatado = dt.strftime("%Y-%m-%d %H:%M:%S")
            except Exception:
                datetime_formatado = raw_created_at

        # Conteúdo de Texto
        record = post.get("record") or {}
        text_post = record.get("text", "")
        
        # Normalização de Texto
        text_post = unicodedata.normalize("NFKC", text_post)
        text_post = "\n".join(line.strip() for line in text_post.splitlines()).strip()

        # Extração de Imagens (Embeds do Bluesky)
        images = []
        embed = post.get("embed") or {}
        
        # O Bluesky pode retornar imagens em 'images' ou em 'media' (se for post com link+imagem)
        images_list = embed.get("images") or embed.get("media", {}).get("images") or []
        for img in images_list:
            if isinstance(img, dict):
                # Pega a imagem completa ou a thumbnail de visualização
                img_url = img.get("fullsize") or img.get("thumb")
                if img_url:
                    images.append(img_url)

        # Filtro de conteúdo vazio
        if not text_post and not images:
            continue

        # Formatação final de imagem no padrão Linka
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
    # Endpoint PÚBLICO do Bluesky (não precisa de login/token)
    url = "https://public.api.bsky.app/xrpc/app.bsky.feed.searchPosts"

    headers = {
        "User-Agent": "LinkaLiteBridge/1.0"
    }

    params = {
        'q': query,
        'limit': limit
    }

    try:
        response = requests.get(url, headers=headers, params=params, timeout=10)
        
        if response.status_code != 200:
            print(f"[BLUESKY ERROR] Status: {response.status_code}")
            return []

        data = response.json()
        posts_brutos = data.get("posts", [])
        
        return formate_bluesky(posts_brutos)

    except Exception as e:
        print(f"[BLUESKY EXCEPTION] {e}")
        return []