import re
import time
import urllib.parse
import xml.etree.ElementTree as ET
from html import unescape

from flask import Blueprint, jsonify, request
import requests


post_bp = Blueprint("reddit_post_bp", __name__)

CLOUDFLARE_WORKER_URL = (
    "https://fancy-fire-49d2.luizsgustavo76.workers.dev"
)

# Todas as imagens do Reddit passam pelo lite-render.
# O Android antigo NÃO acessa o Reddit diretamente.
PROXY_BASE = (
    "http://linkaProject.pythonanywhere.com/lite-render?url="
)


def sanitizar_url_reddit(url):
    """
    Limpa e normaliza URLs de imagens do Reddit.
    """
    if not url:
        print("[REDDIT IMAGE] URL vazia")
        return ""

    original = url

    # Decodifica entidades HTML do RSS.
    clean = unescape(url).strip()

    print("[REDDIT IMAGE] URL recebida:")
    print(f"[REDDIT IMAGE]   {original}")
    print("[REDDIT IMAGE] URL após unescape:")
    print(f"[REDDIT IMAGE]   {clean}")

    # Corrige URLs quebradas que possuem vários '?'.
    if "redd.it" in clean and clean.count("?") > 1:
        parts = clean.split("?")
        clean = parts[0] + "?" + "&".join(parts[1:])

        print("[REDDIT IMAGE] Corrigido múltiplos '?'")
        print(f"[REDDIT IMAGE]   {clean}")

    # Ajustes de tamanho.
    clean = re.sub(r"width=\d+", "width=1080", clean)
    clean = re.sub(r"height=\d+", "", clean)
    clean = re.sub(r"crop=[^&]+", "", clean)

    # Evita parâmetros duplicados.
    clean = re.sub(r"&&+", "&", clean)

    clean = clean.strip("&?")

    print("[REDDIT IMAGE] URL final sanitizada:")
    print(f"[REDDIT IMAGE]   {clean}")

    return clean


@post_bp.route(
    "/feed/reddit/<path:subreddit>",
    methods=["GET"],
    strict_slashes=False,
)
def subreddit_posts(subreddit=None):

    clear_sub = subreddit.strip("/") if subreddit else "LinkaProject"

    if clear_sub.endswith("/feed"):
        clear_sub = clear_sub[:-5]

    if not clear_sub or clear_sub.lower() in [
        "feed",
        "valide-session",
    ]:
        clear_sub = "LinkaProject"

    limit = request.args.get(
        "limit",
        default=100,
        type=int,
    )

    posts = []

    target_rss = (
        f"https://www.reddit.com/r/{clear_sub}/new.rss"
        f"?limit={limit}"
    )

    proxy_url = (
        f"{CLOUDFLARE_WORKER_URL}/?url="
        f"{urllib.parse.quote(target_rss, safe='')}"
    )

    print("")
    print("==================================================")
    print("[REDDIT] INICIANDO FEDERAÇÃO")
    print("==================================================")
    print(f"[REDDIT] subreddit={clear_sub}")
    print(f"[REDDIT] limit={limit}")
    print(f"[REDDIT] RSS original={target_rss}")
    print(f"[REDDIT] RSS via Cloudflare={proxy_url}")
    print("==================================================")

    try:
        start = time.time()

        print("[REDDIT] Fazendo GET do RSS via Cloudflare...")

        res = requests.get(
            proxy_url,
            timeout=10,
        )

        elapsed = time.time() - start

        print("[REDDIT] RSS respondeu:")
        print(f"[REDDIT]   HTTP={res.status_code}")
        print(f"[REDDIT]   tempo={elapsed:.2f}s")
        print(f"[REDDIT]   content-type={res.headers.get('Content-Type')}")
        print(f"[REDDIT]   content-length={res.headers.get('Content-Length')}")
        print(f"[REDDIT]   tamanho={len(res.content)} bytes")

        if res.status_code != 200:
            print("[REDDIT] ERRO: RSS não retornou HTTP 200")
            print(f"[REDDIT] Resposta={res.text[:1000]}")

        if res.status_code == 200 and res.text.strip():

            print("[REDDIT] RSS recebido. Fazendo parse XML...")

            root = ET.fromstring(res.text)

            for elem in root.iter():
                if "}" in elem.tag:
                    elem.tag = elem.tag.split("}", 1)[1]

            entries = root.findall("entry")

            print(f"[REDDIT] Entradas encontradas={len(entries)}")

            for entry_index, entry in enumerate(entries, start=1):

                print("")
                print("-----------------------------------------------")
                print(f"[REDDIT] PROCESSANDO POST #{entry_index}")
                print("-----------------------------------------------")

                title_elem = entry.find("title")

                content_elem = entry.find("content")

                title = (
                    title_elem.text.strip()
                    if title_elem is not None
                    and title_elem.text
                    else ""
                )

                print(f"[REDDIT] título={title}")

                author = "entity404"

                author_elem = entry.find("author")

                if author_elem is not None:

                    name_elem = author_elem.find("name")

                    if (
                        name_elem is not None
                        and name_elem.text
                    ):
                        author = name_elem.text

                    elif (
                        author_elem.find("uri") is not None
                        and author_elem.find("uri").text
                    ):
                        author = (
                            author_elem.find("uri").text
                            .split("/user/")[-1]
                        )

                    elif author_elem.text:
                        author = author_elem.text

                    author = re.sub(
                        r"^/?u/",
                        "",
                        author,
                    ).strip()

                print(f"[REDDIT] autor={author}")

                if not author or author in [
                    "[deleted]",
                    "AutoModerator",
                ]:
                    print(
                        "[REDDIT] Post ignorado: autor inválido."
                    )
                    continue

                body = ""
                image_url = ""

                # ==================================================
                # TENTA ENCONTRAR IMAGEM DENTRO DO CONTENT
                # ==================================================

                if (
                    content_elem is not None
                    and content_elem.text
                ):

                    content_html = unescape(
                        content_elem.text
                    )

                    print(
                        "[REDDIT IMAGE] Procurando imagem no content..."
                    )

                    img_match = re.search(
                        r'src=["\']'
                        r'(https://(?:i|preview|external-preview)'
                        r'\.redd\.it/[^"\']+'
                        r'|https://i\.imgur\.com/[^"\']+)'
                        r'["\']',
                        content_html,
                        re.IGNORECASE,
                    )

                    if img_match:

                        image_url = img_match.group(1)

                        print(
                            "[REDDIT IMAGE] Imagem encontrada no src:"
                        )
                        print(
                            f"[REDDIT IMAGE]   {image_url}"
                        )

                    else:

                        link_match = re.search(
                            r'href=["\']'
                            r'(https://i\.redd\.it/[^"\']+'
                            r'\.(?:jpg|jpeg|png|gif))'
                            r'["\']',
                            content_html,
                            re.IGNORECASE,
                        )

                        if link_match:

                            image_url = link_match.group(1)

                            print(
                                "[REDDIT IMAGE] Imagem encontrada no href:"
                            )
                            print(
                                f"[REDDIT IMAGE]   {image_url}"
                            )

                    # ==================================================
                    # TEXTO DO POST
                    # ==================================================

                    text_match = re.search(
                        r'<div class="md">(.*?)</div>',
                        content_html,
                        re.DOTALL,
                    )

                    if text_match:

                        raw_text = text_match.group(1)

                        body = re.sub(
                            r"<[^>]+>",
                            "",
                            raw_text,
                        ).strip()

                    body = re.sub(
                        r"\[link\]|\[comments\]",
                        "",
                        body,
                        flags=re.IGNORECASE,
                    ).strip()

                    print(
                        f"[REDDIT] body={body[:300]}"
                    )

                # ==================================================
                # SE NÃO ACHOU, TENTA THUMBNAIL
                # ==================================================

                if not image_url:

                    print(
                        "[REDDIT IMAGE] Nenhuma imagem no content."
                    )

                    media_elem = entry.find("thumbnail")

                    if (
                        media_elem is not None
                        and "url" in media_elem.attrib
                    ):

                        thumb = media_elem.attrib["url"]

                        print(
                            "[REDDIT IMAGE] Thumbnail encontrada:"
                        )
                        print(
                            f"[REDDIT IMAGE]   {thumb}"
                        )

                        if thumb.startswith("http"):
                            image_url = thumb

                # ==================================================
                # DESCARTA POST COMPLETAMENTE VAZIO
                # ==================================================

                if (
                    not title
                    and not body
                    and not image_url
                ):
                    print(
                        "[REDDIT] Post ignorado: completamente vazio."
                    )
                    continue

                components = []

                if title:
                    components.append(title)

                if body:
                    components.append(body)

                # ==================================================
                # IMAGEM
                # ==================================================

                if image_url:

                    print("")
                    print("[REDDIT IMAGE] ===============================")
                    print("[REDDIT IMAGE] IMAGEM ENCONTRADA")
                    print("[REDDIT IMAGE] ===============================")
                    print(
                        f"[REDDIT IMAGE] original={image_url}"
                    )

                    clean_img_url = sanitizar_url_reddit(
                        image_url
                    )

                    print(
                        "[REDDIT IMAGE] URL limpa="
                        f"{clean_img_url}"
                    )

                    # Codifica a URL inteira para ficar dentro
                    # do parâmetro ?url= do lite-render.
                    safe_image_url = urllib.parse.quote(
                        clean_img_url,
                        safe="",
                    )

                    lite_render_url = (
                        f"{PROXY_BASE}{safe_image_url}"
                    )

                    print(
                        "[REDDIT IMAGE] URL codificada="
                    )
                    print(
                        f"[REDDIT IMAGE]   {safe_image_url}"
                    )

                    print(
                        "[REDDIT IMAGE] URL FINAL PARA O ANDROID:"
                    )
                    print(
                        f"[REDDIT IMAGE]   {lite_render_url}"
                    )

                    print(
                        "[REDDIT IMAGE] ==============================="
                    )

                    # O Android recebe SOMENTE o lite-render.
                    components.append(
                        f"[IMAGE]{lite_render_url}"
                    )

                # ==================================================
                # MONTA POST
                # ==================================================

                final_text = "\n".join(components)

                print("")
                print("[REDDIT] POST FINAL:")
                print(f"[REDDIT] autor={author}")
                print(f"[REDDIT] texto={final_text[:1000]}")
                print("-----------------------------------------------")

                posts.append(
                    {
                        "id": len(posts) + 1,
                        "text_post": final_text,
                        "username": (
                            f"@{author}"
                            if not author.startswith("@")
                            else author
                        ),
                    }
                )

                if len(posts) >= limit:
                    print(
                        f"[REDDIT] Limite de {limit} posts atingido."
                    )
                    break

            print("")
            print("==================================================")
            print(
                f"[REDDIT] FEDERAÇÃO FINALIZADA: "
                f"{len(posts)} posts"
            )
            print("==================================================")

            return jsonify(posts), 200

    except ET.ParseError as e:

        print("")
        print("[REDDIT] ERRO AO FAZER PARSE DO XML")
        print(f"[REDDIT] {e}")
        print("==================================================")

    except requests.RequestException as e:

        print("")
        print("[REDDIT] ERRO HTTP AO BUSCAR RSS")
        print(f"[REDDIT] {repr(e)}")
        print("==================================================")

    except Exception as e:

        print("")
        print("[REDDIT] ERRO INESPERADO")
        print(f"[REDDIT] tipo={type(e).__name__}")
        print(f"[REDDIT] erro={repr(e)}")
        print("==================================================")

    return jsonify(posts), 200