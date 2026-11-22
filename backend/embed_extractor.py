import re
import json
import requests
from urllib.parse import urlparse, quote
from bs4 import BeautifulSoup

URL_REGEX = re.compile(r'(https?://[^\s<>"\']+)')

OEMBED_ENDPOINTS = [
    (r'(youtube\.com|youtu\.be)', 'https://www.youtube.com/oembed?format=json&url='),
    (r'(twitter\.com|x\.com)', 'https://publish.twitter.com/oembed?omit_script=true&url='),
    (r'bsky\.app', 'https://embed.bsky.app/oembed?url='),
    (r'tiktok\.com', 'https://www.tiktok.com/oembed?url='),
    (r'vimeo\.com', 'https://vimeo.com/api/oembed.json?url='),
    (r'reddit\.com', 'https://www.reddit.com/oembed?url='),
    (r'soundcloud\.com', 'https://soundcloud.com/oembed?format=json&url='),
    (r'open\.spotify\.com', 'https://open.spotify.com/oembed?url='),
]

def to_android2_safe_image(img_url, width=360):
    """Converte HTTPS/WebP para HTTP JPG leve compatível com Android 2.0"""
    if not img_url or not img_url.startswith("http"):
        return ""
    if "wsrv.nl" in img_url:
        return img_url
    return f"http://wsrv.nl/?url={quote(img_url, safe='')}&w={width}&q=70&output=jpg"

def detect_platform(url):
    domain = urlparse(url).netloc.lower().replace("www.", "")
    if "youtube.com" in domain or "youtu.be" in domain:
        return "YouTube"
    if "twitter.com" in domain or domain == "x.com":
        return "X / Twitter"
    if "bsky.app" in domain:
        return "Bluesky"
    if "instagram.com" in domain:
        return "Instagram"
    if "tiktok.com" in domain:
        return "TikTok"
    if "reddit.com" in domain or "redd.it" in domain:
        return "Reddit"
    if "spotify.com" in domain:
        return "Spotify"
    if "soundcloud.com" in domain:
        return "SoundCloud"
    if "vimeo.com" in domain:
        return "Vimeo"
    return domain

def extract_embed_from_text(text, redis_client=None):
    if not text:
        return None

    match = URL_REGEX.search(text)
    if not match:
        return None

    target_url = match.group(1).rstrip('.,;!?)')
    cache_key = f"embed_meta:{target_url}"

    if redis_client:
        try:
            cached = redis_client.get(cache_key)
            if cached:
                return json.loads(cached)
        except Exception:
            pass

    platform = detect_platform(target_url)
    embed_data = {
        "url": target_url,
        "platform": platform,
        "title": "",
        "description": "",
        "author": "",
        "thumbnail": "",
        "video_url": "",
        "type": "link"
    }

    # 1. Tenta oEmbed oficial (rápido e estruturado)
    for pattern, endpoint in OEMBED_ENDPOINTS:
        if re.search(pattern, target_url, re.IGNORECASE):
            try:
                resp = requests.get(endpoint + quote(target_url, safe=''), timeout=2.5, headers={"User-Agent": "LinkaBot/1.0"})
                if resp.status_code == 200:
                    data = resp.json()
                    embed_data["title"] = data.get("title", "")
                    embed_data["author"] = data.get("author_name", "")
                    embed_data["type"] = data.get("type", "rich")
                    thumb = data.get("thumbnail_url", "")
                    if thumb:
                        embed_data["thumbnail"] = to_android2_safe_image(thumb)
                    # Para Twitter/Bluesky oEmbed, extrai o texto limpo do HTML retornado
                    if not embed_data["title"] and data.get("html"):
                        soup = BeautifulSoup(data["html"], "html.parser")
                        p_tag = soup.find("p")
                        if p_tag:
                            embed_data["description"] = p_tag.get_text(" ", strip=True)[:220]
                    break
            except Exception:
                pass

    # 2. Fallback OpenGraph (Instagram, Mastodon, sites de notícias, etc.)
    if not embed_data["title"] and not embed_data["description"]:
        try:
            resp = requests.get(target_url, timeout=2.5, headers={"User-Agent": "Mozilla/5.0 (compatible; LinkaBot/1.0)"})
            if resp.status_code == 200 and "text/html" in resp.headers.get("Content-Type", ""):
                soup = BeautifulSoup(resp.text[:65536], "html.parser")  # Lê só os primeiros 64KB
                og_title = soup.find("meta", property="og:title") or soup.find("meta", attrs={"name": "twitter:title"})
                og_desc = soup.find("meta", property="og:description") or soup.find("meta", attrs={"name": "twitter:description"})
                og_img = soup.find("meta", property="og:image") or soup.find("meta", attrs={"name": "twitter:image"})
                og_vid = soup.find("meta", property="og:video:url") or soup.find("meta", property="og:video")

                if og_title and og_title.get("content"):
                    embed_data["title"] = og_title["content"].strip()[:140]
                elif soup.title:
                    embed_data["title"] = soup.title.get_text(strip=True)[:140]

                if og_desc and og_desc.get("content"):
                    embed_data["description"] = og_desc["content"].strip()[:220]

                if og_img and og_img.get("content"):
                    embed_data["thumbnail"] = to_android2_safe_image(og_img["content"].strip())

                if og_vid and og_vid.get("content") and og_vid["content"].endswith(".mp4"):
                    embed_data["video_url"] = og_vid["content"].strip()
                    embed_data["type"] = "video"
        except Exception:
            pass

    if not embed_data["title"] and not embed_data["description"] and not embed_data["thumbnail"]:
        return None

    if redis_client:
        try:
            redis_client.setex(cache_key, 86400, json.dumps(embed_data))  # Cache de 24h
        except Exception:
            pass

    return embed_data