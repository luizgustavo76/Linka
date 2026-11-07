import json
import os
import re
import unicodedata
import xml.etree.ElementTree as ET
from datetime import datetime
from pathlib import Path
from urllib.parse import quote, urlparse

from bs4 import BeautifulSoup
import dotenv
from flask import Blueprint, jsonify, request
import requests

BASE_DIR = Path(__file__).resolve().parent.parent
env_path = BASE_DIR / ".env"
dotenv.load_dotenv()

twitter_bp = Blueprint("twitter_bp", __name__)

CODEBERG_INSTANCES_URL = "https://codeberg.org/shitter/shitter/raw/branch/master/instances.json"


def get_relative_time(post_date):
    if not post_date:
        return ""
    if isinstance(post_date, str):
        try:
            post_date = datetime.strptime(post_date, "%Y-%m-%d %H:%M:%S")
        except ValueError:
            try:
                # Trata datas padrão RSS RFC 822/1123 (ex: 'Sat, 07 Nov 2026 15:28:15 GMT')
                from email.utils import parsedate_to_datetime
                dt_obj = parsedate_to_datetime(post_date)
                post_date = dt_obj.replace(tzinfo=None)
            except Exception:
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


def get_active_shitter_instances():
    """Puxa a lista dinâmica de instâncias do Shitter direto do repositório no Codeberg."""
    headers = {
        "User-Agent": "Mozilla/5.0 (X11; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0"
    }
    try:
        response = requests.get(CODEBERG_INSTANCES_URL, headers=headers, timeout=4)
        if response.status_code == 200:
            data = response.json()
            instances = []
            if isinstance(data, list):
                for item in data:
                    url = item.get("url") or item.get("clearnet")
                    if url and url.startswith("http") and not url.endswith(".onion"):
                        instances.append(url.rstrip("/"))
            if instances:
                return instances
    except Exception as e:
        print(f"[TWITTER INSTANCES ERROR] {e}")

    # Fallback seguro com espelhos conhecidos
    return [
        "https://nitter.meowing.monster",
        "https://shitter.thepixora.com",
    ]


def formate(items, default_query=""):
    posts_formatados = []

    for item in items:
        if not isinstance(item, dict):
            continue

        title_raw = item.get("title", "")
        creator = item.get("creator") or item.get("author") or ""
        link = item.get("link", "")
        description_raw = item.get("description", "")
        pub_date_raw = item.get("pubDate", "")

        # Trata o Username
        username = creator.strip()
        if not username:
            # Tenta extrair o nome de usuário da URL original (ex: /NASA/status/123)
            parsed_link = urlparse(link)
            parts = [p for p in parsed_link.path.split("/") if p]
            if parts:
                username = parts[0]
            else:
                username = "twitter_user"

        username = f"@{username}" if not username.startswith("@") else username

        # Trata o Texto e remove tags HTML da descrição
        soup = BeautifulSoup(title_raw or description_raw, "html.parser")
        text_post = soup.get_text()
        text_post = unicodedata.normalize("NFKC", text_post)
        text_post = "\n".join(
            line.strip() for line in text_post.splitlines() if line.strip()
        ).strip()

        # Trata a Mídia e mídias secundárias (Links de imagens no HTML do RSS)
        media_url = None
        media_type = None
        images_found = []
        media_list = []

        if description_raw:
            desc_soup = BeautifulSoup(description_raw, "html.parser")
            for img in desc_soup.find_all("img"):
                src = img.get("src")
                if src and src.startswith("http"):
                    images_found.append(src)

        for img_url in images_found:
            media_list.append({
                "media_url": img_url,
                "media_type": "image",
                "width": "",
                "heigth": ""
            })
            if not media_url:
                media_url = img_url
                media_type = "image"

        # Trata Data e Hora
        datetime_formatado = None
        if pub_date_raw:
            try:
                from email.utils import parsedate_to_datetime
                dt_obj = parsedate_to_datetime(pub_date_raw)
                datetime_formatado = dt_obj.strftime("%Y-%m-%d %H:%M:%S")
            except Exception:
                datetime_formatado = str(pub_date_raw)

        # Monta a Tag [IMAGE] no texto assim como no Threads se houver imagens adicionais
        if len(images_found) > 1:
            links_formatados = "".join(f"\n[IMAGE]{url}" for url in images_found[1:])
            text_final = f"{text_post}{links_formatados}".strip()
        else:
            text_final = text_post

        # Extrai o ID único do tweet
        unique_id = str(hash(link))
        match_id = re.search(r"/status/(\d+)", link)
        if match_id:
            unique_id = match_id.group(1)

        if text_final or media_url:
            posts_formatados.append(
                {
                    "username": username,
                    "account_id": username.replace("@", ""),
                    "text_post": text_final,
                    "datetime": datetime_formatado,
                    "relative_datetime": get_relative_time(datetime_formatado),
                    "comment_count": 0,
                    "avatar_url": "",
                    "media": {
                        "media_url": media_url or "",
                        "media_type": media_type or "",
                        "width": "",
                        "heigth": ""
                    },
                    "media_list": media_list,
                    "media_attachments": media_list,
                    "origin_server": "https://linka-project.onrender.com/feed/twitter",
                    "platform": "twitter",
                    "unique_id": unique_id,
                    "original_url": link,
                }
            )

    return posts_formatados


def fetch_twitter_search_fast(query="android", limit=20):
    """Realiza busca por termos ou tags via RSS das instâncias do Shitter em rotação."""
    instances = get_active_shitter_instances()
    encoded_query = quote(query.strip())
    
    headers = {
        "User-Agent": "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
        "Accept-Language": "en-US,en;q=0.5",
    }

    raw_items = []

    for instance in instances:
        try:
            url = f"{instance}/search/rss?f=tweets&q={encoded_query}"
            response = requests.get(url, headers=headers, timeout=3.5)
            
            if response.status_code == 200 and ("<rss" in response.text or "<feed" in response.text):
                root = ET.fromstring(response.text)
                channel = root.find("channel")
                items = channel.findall("item") if channel is not None else root.findall(".//item")
                
                for item in items[:limit]:
                    raw_items.append({
                        "title": item.findtext("title", ""),
                        "link": item.findtext("link", ""),
                        "pubDate": item.findtext("pubDate", ""),
                        "description": item.findtext("description", ""),
                        "creator": item.findtext("{http://purl.org/dc/elements/1.1/}creator", "")
                    })

                if raw_items:
                    return formate(raw_items, default_query=query)[:limit]

        except Exception as e:
            print(f"[TWITTER SEARCH DEBUG] Instância {instance} falhou: {e}")
            continue

    return []


def fetch_twitter_user_posts(username="NASA", limit=20):
    """Puxa os tweets mais recentes de um perfil específico."""
    instances = get_active_shitter_instances()
    clean_user = username.lstrip("@").strip()
    
    headers = {
        "User-Agent": "Mozilla/5.0 (X11; Ubuntu; Linux x86_64; rv:128.0) Gecko/20100101 Firefox/128.0",
        "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
    }

    raw_items = []

    for instance in instances:
        try:
            url = f"{instance}/{clean_user}/rss"
            response = requests.get(url, headers=headers, timeout=3.5)
            
            if response.status_code == 200 and ("<rss" in response.text or "<feed" in response.text):
                root = ET.fromstring(response.text)
                channel = root.find("channel")
                items = channel.findall("item") if channel is not None else root.findall(".//item")

                for item in items[:limit]:
                    raw_items.append({
                        "title": item.findtext("title", ""),
                        "link": item.findtext("link", ""),
                        "pubDate": item.findtext("pubDate", ""),
                        "description": item.findtext("description", ""),
                        "creator": clean_user
                    })

                if raw_items:
                    return formate(raw_items)[:limit]

        except Exception as e:
            print(f"[TWITTER USER DEBUG] Instância {instance} falhou: {e}")
            continue

    return []


# --- ROTAS FLASK PARA O LINKA ---

@twitter_bp.route("/feed/twitter", methods=["GET"])
def get_twitter_feed():
    username = request.args.get("user", default="NASA", type=str)
    limit = request.args.get("limit", default=20, type=int)
    posts = fetch_twitter_user_posts(username=username, limit=limit)
    return jsonify(posts)


@twitter_bp.route("/feed/twitter/search", methods=["GET"])
def get_twitter_search_feed():
    query = request.args.get("q", default="android", type=str)
    limit = request.args.get("limit", default=20, type=int)
    posts = fetch_twitter_search_fast(query=query, limit=limit)
    return jsonify(posts)