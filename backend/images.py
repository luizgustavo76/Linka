import io
import os
import re
import urllib.parse
import uuid
import dotenv
import requests
from flask import Blueprint, Flask, Response, jsonify, redirect, request
from PIL import Image
from supabase import Client, create_client

dotenv.load_dotenv("backend.env")
url = os.getenv("SUPABASE_URL")
key = os.getenv("SUPABASE_KEY")

supabase: Client = create_client(url, key)
image_bp = Blueprint("image_bp", __name__)


def clean_reddit_url(url_str):
    """Limpa e normaliza totalmente URLs de mídia do Reddit."""
    # 1. Decodifica HTML e caracteres codificados
    url_str = urllib.parse.unquote(url_str).replace("&amp;", "&").strip()

    # 2. Se for link do i.redd.it, corta qualquer parâmetro
    if "i.redd.it" in url_str:
        return url_str.split("?")[0]

    # 3. Se contiver múltiplos '?', separa a URL base dos parâmetros
    if "redd.it" in url_str and url_str.count("?") > 1:
        base, rest = url_str.split("?", 1)
        rest = rest.replace("?", "&")
        url_str = f"{base}?{rest}"

    return url_str


@image_bp.route("/lite-render", methods=["GET"])
def lite_render():
    import io
    import time
    import requests

    from flask import Response, request, jsonify
    from PIL import Image, ImageOps

    # ============================================================
    # CONFIGURAÇÃO
    # ============================================================

    CLOUDFLARE_WORKER_URL = (
        "https://fancy-fire-49d2.luizsgustavo76.workers.dev/?url="
    )

    print("\n[LITE-RENDER] =======================================", flush=True)
    print("[LITE-RENDER] NOVA REQUISICAO", flush=True)

    raw_url = request.args.get("url")

    print(
        f"[LITE-RENDER] URL recebida: {raw_url}",
        flush=True,
    )

    if not raw_url:
        print(
            "[LITE-RENDER ERROR] URL missing",
            flush=True,
        )
        return jsonify({"error": "URL missing"}), 400

    # ============================================================
    # LIMPAR URL
    # ============================================================

    try:
        target_url = clean_reddit_url(raw_url)
    except Exception as e:
        print(
            f"[LITE-RENDER] clean_reddit_url falhou: {e}",
            flush=True,
        )
        target_url = raw_url

    print(
        f"[LITE-RENDER] URL final: {target_url}",
        flush=True,
    )

    # ============================================================
    # IMPORTANTE:
    #
    # NÃO fazemos requests.get() diretamente para o Reddit.
    #
    # O PythonAnywhere apenas chama o Cloudflare Worker.
    #
    # A imagem vem para a RAM do processo, é convertida pelo
    # Pillow em RAM e imediatamente devolvida ao Android.
    #
    # NENHUM arquivo é criado.
    # NENHUM cache é criado.
    # ============================================================

    worker_url = (
        CLOUDFLARE_WORKER_URL
        + requests.utils.quote(target_url, safe="")
    )

    print(
        f"[LITE-RENDER] URL do Worker: {worker_url}",
        flush=True,
    )

    headers = {
        "User-Agent": (
            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) "
            "AppleWebKit/537.36 (KHTML, like Gecko) "
            "Chrome/123.0.0.0 Safari/537.36"
        ),
        "Accept": "image/avif,image/webp,image/apng,image/*,*/*;q=0.8",
    }

    # ============================================================
    # BAIXAR PELO WORKER
    #
    # O conteúdo fica SOMENTE na memória.
    # ============================================================

    start = time.time()

    try:
        print(
            "[LITE-RENDER] Chamando Cloudflare Worker...",
            flush=True,
        )

        response = requests.get(
            worker_url,
            headers=headers,
            timeout=15,
            allow_redirects=True,
        )

        elapsed = time.time() - start

        print(
            f"[LITE-RENDER] Worker respondeu em {elapsed:.2f}s",
            flush=True,
        )

        print(
            f"[LITE-RENDER] Status: {response.status_code}",
            flush=True,
        )

        print(
            "[LITE-RENDER] Content-Type: "
            f"{response.headers.get('Content-Type')}",
            flush=True,
        )

        print(
            f"[LITE-RENDER] Bytes recebidos: "
            f"{len(response.content)}",
            flush=True,
        )

        if response.status_code != 200:
            print(
                "[LITE-RENDER ERROR] Worker não conseguiu "
                "buscar a imagem.",
                flush=True,
            )

            print(
                "[LITE-RENDER ERROR] Resposta do Worker:",
                flush=True,
            )

            print(
                response.text[:500],
                flush=True,
            )

            return jsonify(
                {
                    "error": "Failed to fetch image",
                    "worker_status": response.status_code,
                }
            ), 502

        if not response.content:
            print(
                "[LITE-RENDER ERROR] Worker retornou 0 bytes.",
                flush=True,
            )

            return jsonify(
                {"error": "Worker returned empty response"}
            ), 502

    except Exception as e:
        elapsed = time.time() - start

        print(
            "[LITE-RENDER EXCEPTION] Erro chamando Worker "
            f"após {elapsed:.2f}s: {e}",
            flush=True,
        )

        return jsonify(
            {
                "error": "Failed to fetch image",
                "details": str(e),
            }
        ), 502

    try:
        print(
            "[LITE-RENDER] Abrindo imagem com Pillow...",
            flush=True,
        )

        image_data = io.BytesIO(response.content)

        image = Image.open(image_data)

        print(
            f"[LITE-RENDER] Formato original: {image.format}",
            flush=True,
        )

        print(
            f"[LITE-RENDER] Tamanho original: {image.size}",
            flush=True,
        )

        print(
            f"[LITE-RENDER] Modo original: {image.mode}",
            flush=True,
        )

        # Corrigir orientação EXIF sem criar arquivo.
        try:
            image = ImageOps.exif_transpose(image)
        except Exception as e:
            print(
                f"[LITE-RENDER] EXIF não aplicado: {e}",
                flush=True,
            )

        # ========================================================
        # CONVERTER PARA RGB
        #
        # JPEG não aceita:
        # - RGBA
        # - LA
        # - P
        # ========================================================

        if image.mode == "P":
            print(
                "[LITE-RENDER] Convertendo P -> RGBA",
                flush=True,
            )

            image = image.convert("RGBA")

        if image.mode in ("RGBA", "LA"):
            print(
                "[LITE-RENDER] Removendo transparência "
                "para compatibilidade JPEG.",
                flush=True,
            )

            background = Image.new(
                "RGB",
                image.size,
                (255, 255, 255),
            )

            alpha = image.getchannel("A")

            background.paste(
                image,
                (0, 0),
                alpha,
            )

            image = background

        elif image.mode != "RGB":
            print(
                f"[LITE-RENDER] Convertendo "
                f"{image.mode} -> RGB",
                flush=True,
            )

            image = image.convert("RGB")

        # ========================================================
        # JPEG TAMBÉM EM MEMÓRIA
        # ========================================================

        jpeg_buffer = io.BytesIO()

        image.save(
            jpeg_buffer,
            format="JPEG",
            quality=88,
            optimize=True,
        )

        jpeg_buffer.seek(0)

        jpeg_size = jpeg_buffer.getbuffer().nbytes

        print(
            "[LITE-RENDER] Conversão para JPEG concluída.",
            flush=True,
        )

        print(
            f"[LITE-RENDER] JPEG final: {jpeg_size} bytes",
            flush=True,
        )

        print(
            "[LITE-RENDER] Enviando JPEG para Android...",
            flush=True,
        )

        print(
            "[LITE-RENDER] SUCESSO",
            flush=True,
        )

        print(
            "[LITE-RENDER] =======================================\n",
            flush=True,
        )

        # ========================================================
        # DEVOLVE DIRETAMENTE DA MEMÓRIA
        #
        # NÃO salva arquivo.
        # NÃO cria cache.
        # NÃO faz redirect.
        # ========================================================

        return Response(
            jpeg_buffer.getvalue(),
            status=200,
            mimetype="image/jpeg",
            headers={
                "Cache-Control": "no-store",
            },
        )

    except Exception as e:
        print(
            "[LITE-RENDER ERROR] Pillow não conseguiu "
            f"processar a imagem: {e}",
            flush=True,
        )

        print(
            "[LITE-RENDER] =======================================\n",
            flush=True,
        )

        return jsonify(
            {
                "error": "Failed to process image",
                "details": str(e),
            }
        ), 500

@image_bp.route("/upload-image", methods=["POST"])
def upload_image():
    if "image" not in request.files:
        return jsonify({"error": "No image uploaded"}), 400

    file = request.files["image"]

    if file.filename == "":
        return jsonify({"error": "File with no name"}), 400

    try:
        extension = os.path.splitext(file.filename)[1]
        unique_file_name = f"post_{uuid.uuid4().hex}{extension}"

        file_data = file.read()

        bucket_name = "linka-media"
        supabase.storage.from_(bucket_name).upload(
            path=unique_file_name,
            file=file_data,
            file_options={"content-type": file.content_type},
        )

        public_url = supabase.storage.from_(bucket_name).get_public_url(
            unique_file_name
        )

        return jsonify({"status": "success", "image_url": public_url}), 200

    except Exception as e:
        return jsonify({"status": "error", "message": str(e)}), 500