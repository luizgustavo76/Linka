from flask import Blueprint, request, jsonify, g
import sqlite3
import os
from datetime import datetime
import notificationsModule
import re
from urllib.parse import urlparse
import random
import linkosModule
import mentions_module
import json
from concurrent.futures import ThreadPoolExecutor, as_completed
import requests
from PIL import ImageFile

base_dir = os.path.dirname(os.path.abspath(__file__))
json_path = os.path.join(base_dir, "backend.json")

with open(json_path, "r") as f:
    modules_flags = json.load(f)

root_flags = modules_flags["modules-flags"]
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
post_dir = os.path.join(db_dir, "post.db")

if root_flags.get("bluesky-federation"):
    import bluesky_federation.posts as bluesky
if root_flags.get("reddit-federation"):
    import reddit_federation.posts as reddit
if root_flags.get("mastodon-federation"):
    import mastodon_federation.posts as mastodon

post_bp = Blueprint("post", __name__)

bg_executor = ThreadPoolExecutor(max_workers=5)

if not os.path.exists(db_dir):
    os.makedirs(db_dir)


def get_image_dimensions(url):
    """
    Faz o download rápido do cabeçalho da imagem (Stream)
    e extrai largura e altura sem carregar a imagem inteira na RAM.
    """
    if not url or not isinstance(url, str) or not url.startswith(("http://", "https://")):
        return "", ""
    try:
        response = requests.get(url, stream=True, timeout=2.5, headers={"User-Agent": "LinkaBackend/1.0"})
        if response.status_code == 200:
            parser = ImageFile.Parser()
            for chunk in response.iter_content(chunk_size=1024):
                parser.feed(chunk)
                if parser.image:
                    w, h = parser.image.size
                    return str(w), str(h)
                if len(chunk) == 0:
                    break
    except Exception as e:
        print(f"[DEBUG MEDIA DIMENSIONS ERROR] {url}: {e}")
    return "", ""


def process_dimensions_in_background(post_id, main_media, media_items):
    """
    Task de fundo que roda de forma assíncrona para não travar conexões.
    """
    try:
        conn = get_db()
        cur = conn.cursor()

        if main_media:
            w, h = get_image_dimensions(main_media)
            if w and h:
                cur.execute(
                    "UPDATE posts SET width = ?, heigth = ? WHERE id = ?",
                    (w, h, post_id)
                )

        if media_items and isinstance(media_items, list):
            for item in media_items:
                media_url = item.get("media_url")
                item_id = item.get("id")
                if media_url and item_id:
                    w, h = get_image_dimensions(media_url)
                    if w and h:
                        cur.execute(
                            "UPDATE post_media SET width = ?, heigth = ? WHERE id = ?",
                            (w, h, item_id)
                        )

        conn.commit()
        conn.close()
    except Exception as e:
        print(f"[BACKGROUND TASK ERROR] Falha ao processar post #{post_id}: {e}")


def get_db():
    conn = sqlite3.connect(post_dir, timeout=10)
    conn.row_factory = sqlite3.Row
    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA synchronous=NORMAL;")
    conn.execute("PRAGMA cache_size=-10000;")
    return conn


def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS federated_posts(
            federation_name TEXT,
            text_post TEXT,
            username TEXT,
            post_id TEXT,
            created_at TEXT,
            platform TEXT
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS posts(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT,
            media TEXT,
            media_type TEXT,
            width TEXT,
            heigth TEXT,
            text_post TEXT,
            datetime TEXT
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS post_media(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            post_id INTEGER,
            media_url TEXT,
            media_type TEXT,
            width TEXT,
            heigth TEXT
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS stars(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            post_id INTEGER,
            username TEXT,
            UNIQUE(post_id, username)
        )
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS comments(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            text_comment TEXT,
            post_id INTEGER,
            username TEXT
        )
    """)

    cur.execute("PRAGMA table_info(posts);")
    columns = [column[1] for column in cur.fetchall()]

    required_columns = {
        "media": "TEXT",
        "media_type": "TEXT",
        "width": "TEXT",
        "heigth": "TEXT"
    }

    for col_name, col_type in required_columns.items():
        if col_name not in columns:
            cur.execute(f"ALTER TABLE posts ADD COLUMN {col_name} {col_type};")

    conn.commit()
    conn.close()


create_db()


def get_relative_time(post_date):
    if not post_date:
        return ""
    if isinstance(post_date, str):
        try:
            post_date = datetime.strptime(post_date, "%Y-%m-%d %H:%M:%S")
        except ValueError:
            try:
                post_date = datetime.fromisoformat(post_date)
            except ValueError:
                return post_date

    now = datetime.now()
    diff = now - post_date
    seconds = int(diff.total_seconds())

    if seconds < 60:
        return "just now"
    
    minutes = seconds // 60
    if minutes < 60:
        return f"{minutes} minutes ago"
    
    hours = minutes // 60
    if hours < 24:
        return f"{hours} hours ago"
    
    days = hours // 24
    if days < 30:
        return f"{days} days ago"
    
    months = days // 30
    if months < 12:
        return f"{months} month ago" if months == 1 else f"{months} months ago"
    
    years = days // 365
    return f"{years} year ago" if years == 1 else f"{years} years ago"


def parse_media_list(media_raw, media_type_raw, width_raw, height_raw):
    media_items = []
    if not media_raw:
        return media_items

    if isinstance(media_raw, str) and media_raw.strip().startswith("["):
        try:
            parsed = json.loads(media_raw)
            if isinstance(parsed, list):
                for item in parsed:
                    if isinstance(item, dict):
                        media_items.append({
                            "media_url": item.get("media_url", item.get("url", "")),
                            "media_type": item.get("media_type", "image"),
                            "width": str(item.get("width", "")),
                            "heigth": str(item.get("heigth", item.get("height", "")))
                        })
                    elif isinstance(item, str):
                        media_items.append({
                            "media_url": item,
                            "media_type": "image",
                            "width": "",
                            "heigth": ""
                        })
                return media_items
        except Exception:
            pass

    if isinstance(media_raw, str) and "," in media_raw:
        urls = [u.strip() for u in media_raw.split(",") if u.strip()]
        for u in urls:
            media_items.append({
                "media_url": u,
                "media_type": media_type_raw or "image",
                "width": str(width_raw or ""),
                "heigth": str(height_raw or "")
            })
        return media_items

    media_items.append({
        "media_url": media_raw,
        "media_type": media_type_raw or "image",
        "width": str(width_raw or ""),
        "heigth": str(height_raw or "")
    })
    return media_items


@post_bp.route("/view-profile-posts", methods=["POST"])
def view_profile_posts():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    if isinstance(username, list) and len(username) > 0:
        username = username[0]
    username = str(username).strip() if username else ""
    
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT * FROM posts WHERE username = ? ORDER by datetime DESC", (username,))
    result = cur.fetchall()
    conn.close()

    posts = []
    for single_posts in result:
        posts.append({
            "id": single_posts["id"],
            "username": single_posts["username"],
            "text_post": single_posts["text_post"],
            "datetime": single_posts["datetime"],
            "relative_datetime": get_relative_time(single_posts["datetime"])
        })
    return jsonify(posts)


@post_bp.route("/view-post", methods=["POST"])
def view_post():
    data = request.get_json(silent=True) or {}
    post_id = data.get("post_id")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT * FROM posts WHERE id = ?", (post_id,))
    posts = cur.fetchall()
    conn.close()
    return jsonify([dict(row) for row in posts])


@post_bp.route("/comments", methods=["POST"])
def new_comment():
    data = request.get_json(force=True) or {}
    username = data.get("username")
    current_user = getattr(g, "username", None)

    if username and username == current_user:
        text_comment = data.get("text_comment") or data.get("text_post")
        post_id = data.get("post_id")

        if not text_comment or not post_id:
            return jsonify({"status": "the informations is empty"}), 400

        conn = get_db()
        cur = conn.cursor()
        cur.execute("INSERT INTO comments (text_comment, username, post_id) VALUES(?,?,?)", (text_comment, username, post_id))
        cur.execute("SELECT username FROM posts WHERE id = ?", (post_id,))
        op = cur.fetchone()
        conn.commit()
        conn.close()

        linkosModule.add_linkos(username, 2)
        post_owner = op["username"] if op else None        
        date = datetime.now()
        if post_owner:
            notificationsModule.CreateNotification(username, post_owner, date, "comment", text_comment)
        return jsonify({"status": "the comment has been created with sucess!"}), 200
    else:
        return jsonify({"status": "forbidden"}), 403


@post_bp.route("/edit-post", methods=["POST"])
def edit_post():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    if username == getattr(g, "username", None):
        text_post = data.get("text_post")
        post_id = data.get("post_id")
        conn = get_db()
        cur = conn.cursor()
        cur.execute("UPDATE posts SET text_post = ? WHERE id = ?", (text_post, post_id))
        conn.commit()
        conn.close()
        return jsonify({"status": "the post has edited"}), 200
    else:
        return jsonify({"status": "forbidden"}), 403


@post_bp.route("/delete-post", methods=["POST"])
def delete_post():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    if username == getattr(g, "username", None):
        post_id = data.get("post_id")
        conn = get_db()
        cur = conn.cursor()
        cur.execute("DELETE FROM posts WHERE id = ?", (post_id,))
        cur.execute("DELETE FROM post_media WHERE post_id = ?", (post_id,))
        conn.commit()
        conn.close()
        return jsonify({"status": "the post has deleted"}), 200
    else:
        return jsonify({"status": "forbidden"}), 403


@post_bp.route("/view-comments", methods=["POST"])
def view_comments():
    data = request.get_json(force=True) or {}
    post_id = data.get("post_id")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT * FROM comments WHERE post_id = ?", (post_id,))      
    rows = cur.fetchall()
    conn.close()

    comments = []
    for row in rows:
        comments.append({
            "username": row["username"],
            "text_comment": row["text_comment"],
            "post_id": row["post_id"],
            "comment_id": row["id"]
        })
    return jsonify({"comments": comments})


def get_internal_feed_posts():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT id, username, text_post, datetime FROM posts ORDER BY id DESC")
    posts = cur.fetchall()
    conn.close()

    lista_posts = []
    for post in posts:
        lista_posts.append({
            "id": post["id"],
            "username": post["username"],
            "text_post": post["text_post"],
            "datetime": post["datetime"]
        })
    return lista_posts


def fetch_single_federation(node_url):
    parsed = urlparse(node_url)
    netloc = parsed.netloc.lower()
    path = parsed.path.rstrip('/')

    if "linkaproject.pythonanywhere.com" in netloc or "127.0.0.1" in netloc or "localhost" in netloc or netloc.startswith("192.168."):
        try:
            if path == "/feed":
                return get_internal_feed_posts() 
            
            elif path.startswith("/feed/reddit/"):
                if root_flags.get("reddit-federation") and 'reddit' in globals():
                    sub_name = path.replace("/feed/reddit/", "")
                    if hasattr(reddit, "fetch_reddit_posts"):
                        posts = reddit.fetch_reddit_posts(subreddit=sub_name)
                        for p in (posts or []):
                            if isinstance(p, dict):
                                p["platform"] = "reddit"
                        return posts

            elif path.startswith("/feed/mastodon/"):
                if root_flags.get("mastodon-federation") and 'mastodon' in globals():
                    tag_name = path.replace("/feed/mastodon/", "")
                    if hasattr(mastodon, "fetch_mastodon_posts"):
                        posts = mastodon.fetch_mastodon_posts(tag=tag_name, limit=100)
                        for p in (posts or []):
                            if isinstance(p, dict):
                                p["platform"] = "mastodon"
                        return posts

            elif path.startswith("/feed/bluesky/"):
                if root_flags.get("bluesky-federation") and 'bluesky' in globals():
                    tag_name = path.replace("/feed/bluesky/", "")
                    if hasattr(bluesky, "fetch_bluesky_posts"):
                        posts = bluesky.fetch_bluesky_posts(query=tag_name, limit=100)
                        for p in (posts or []):
                            if isinstance(p, dict):
                                p["platform"] = "bluesky"
                        return posts

        except Exception as e:
            print(f"DEBUG - Erro na rota interna {path}: {e}")
            return None

    try:
        response = requests.get(node_url, timeout=2.5)
        if response.status_code == 200:
            return response.json()
    except Exception as e:
        print(f"DEBUG - Erro na requisição externa {node_url}: {e}")
        return None

    return None


def parse_datetime_flexible(dt_str):
    if not dt_str:
        return datetime.min
    if isinstance(dt_str, (int, float)):
        return datetime.fromtimestamp(dt_str)
    
    dt_clean = str(dt_str).replace('T', ' ').replace('Z', '')
    
    formats = [
        '%Y-%m-%d %H:%M:%S',
        '%Y-%m-%d %H:%M:%S.%f',
        '%Y-%m-%d'
    ]
    for fmt in formats:
        try:
            return datetime.strptime(dt_clean, fmt)
        except ValueError:
            pass
    return datetime.min


def sort_and_interleave_posts(all_posts):
    for post in all_posts:
        post['_dt_obj'] = parse_datetime_flexible(post.get('datetime'))
    sorted_posts = sorted(all_posts, key=lambda x: x['_dt_obj'], reverse=True)

    final_feed = []
    consecutive_count = 0
    last_platform = None

    while sorted_posts:
        chosen_idx = 0
        if consecutive_count >= 2 and len(sorted_posts) > 1:
            for idx in range(1, min(6, len(sorted_posts))):
                if sorted_posts[idx].get('platform') != last_platform:
                    chosen_idx = idx
                    break

        post = sorted_posts.pop(chosen_idx)
        current_platform = post.get('platform', 'unknown')

        del post['_dt_obj']
        final_feed.append(post)

        if current_platform == last_platform:
            consecutive_count += 1
        else:
            consecutive_count = 1
            last_platform = current_platform

    return final_feed


@post_bp.route("/view-external-posts", methods=["POST"])
def view_external_posts():
    data = request.get_json(silent=True) or {}
    urls = data.get("urls")
    filters = data.get("filters")
    if not urls or not isinstance(urls, list):
        return jsonify({"error": "url invalid or bad formatted"}), 400

    if len(urls) > 50:
        return jsonify({"status": "max limit with federation url"}), 400

    aggregated_posts = []

    with ThreadPoolExecutor(max_workers=20) as executor:
        futures = [executor.submit(fetch_single_federation, url) for url in urls]
        
        for future in as_completed(futures):
            try:
                result = future.result()
                if not result:
                    continue
                posts_list = []
                if isinstance(result, list):
                    posts_list = result
                elif isinstance(result, dict) and "posts" in result and isinstance(result["posts"], list):
                    posts_list = result["posts"]

                for post in posts_list:
                    if not isinstance(post, dict):
                        continue
                    text = post.get("text_post", "")
                    if filters:
                        if isinstance(filters, list) and any(f.lower() in text.lower() for f in filters if f):
                            continue
                        elif isinstance(filters, str) and filters.lower() in text.lower():
                            continue

                    aggregated_posts.append(post)

            except Exception as e:
                print(f"DEBUG - Erro na thread: {e}")

    if not aggregated_posts:
        return jsonify([]), 200

    aggregated_posts = sort_and_interleave_posts(aggregated_posts)
    formated_feed = []
    for index, post in enumerate(aggregated_posts, start=1):
        if isinstance(post, dict):
            post["id"] = index
            formated_feed.append(post)

    return jsonify(formated_feed), 200


@post_bp.route("/new", methods=["POST"])
def new_post():
    try:
        data = request.get_json(silent=True)

        if data is None:
            return jsonify({"status": "Invalid json"}), 400

        username = data.get("username")
        current_user = getattr(g, "username", None)
        if username == current_user:
            text_post = data.get("text_post", "")
            media = data.get("media")
            media_type = data.get("media_type")
            datetime_post = data.get("datetime") or datetime.now().strftime("%Y-%m-%d %H:%M:%S")
            
            width = data.get("width", "")
            heigth = data.get("heigth", "")

            media_list_input = data.get("media_list") or data.get("media_attachments")

            match = re.search(r'\[IMAGE\]\s*(.*)', text_post)
            if match:
                media = match.group(1).strip()
                media_type = "image"

            if not username:
                return jsonify({"status": "username not send"}), 400

            if not text_post and not media and not media_list_input:
                return jsonify({"status": "its not possible create a post with no cotent"}), 400

            if "@" in text_post:
                users_mention = re.findall(r'@([^\s]+)', text_post)
                for user in users_mention:
                    date = datetime.now()
                    notificationsModule.CreateNotification(username, user, date, "mention", f"{username} mentioned you in a post")
                    mentions_module.createMention(username, user, text_post, None, None, "post")

            main_media = media
            if not main_media and media_list_input and len(media_list_input) > 0:
                first_item = media_list_input[0]
                if isinstance(first_item, dict):
                    main_media = first_item.get("media_url")
                    media_type = first_item.get("media_type", "image")
                    width = first_item.get("width", "")
                    heigth = first_item.get("heigth") or first_item.get("height", "")
                elif isinstance(first_item, str):
                    main_media = first_item

            conn = get_db()
            cur = conn.cursor()

            cur.execute(
                "INSERT INTO posts(username, text_post, datetime, media, media_type, width, heigth) VALUES (?, ?, ?, ?, ?, ?, ?)",
                (username, text_post, datetime_post, main_media, media_type, str(width or ""), str(heigth or ""))
            )
            post_id = cur.lastrowid

            background_media_items = []

            if media_list_input and isinstance(media_list_input, list):
                for item in media_list_input:
                    if isinstance(item, dict):
                        m_url = item.get("media_url") or item.get("url")
                        m_type = item.get("media_type", "image")
                        m_w = str(item.get("width", ""))
                        m_h = str(item.get("heigth", item.get("height", "")))
                    else:
                        m_url = str(item)
                        m_type = "image"
                        m_w = ""
                        m_h = ""

                    if m_url:
                        cur.execute(
                            "INSERT INTO post_media(post_id, media_url, media_type, width, heigth) VALUES (?, ?, ?, ?, ?)",
                            (post_id, m_url, m_type, m_w, m_h)
                        )
                        item_id = cur.lastrowid
                        if not m_w or not m_h:
                            background_media_items.append({"id": item_id, "media_url": m_url})

            conn.commit()
            conn.close()

            if (main_media and (not width or not heigth)) or background_media_items:
                bg_executor.submit(
                    process_dimensions_in_background,
                    post_id,
                    main_media if (not width or not heigth) else None,
                    background_media_items
                )

            linkosModule.add_linkos(username, "5")
            return jsonify({"status": "post created with sucess"}), 200
        else:
            return jsonify({"status": "forbidden"}), 403
    except Exception as e:
        print("ERROR", e)
        return jsonify({"status": "error", "message": "internal server error"}), 500


@post_bp.route("/trending-feed", methods=["GET"])
def trending_feed():
    posts_id = []
    posts = []
    conn = get_db()
    cur = conn.cursor()    
    cur.execute("""
        SELECT post_id, COUNT(id) as total_stars 
        FROM stars 
        GROUP BY post_id 
        ORDER BY total_stars DESC;
    """)
    result = cur.fetchall()
    for row in result:
        posts_id.append(row["post_id"])
    for i in posts_id:
        cur.execute("SELECT * FROM posts WHERE id = ?", (i,))
        result = cur.fetchall()
        for row in result:
            posts.append({
                "id": row["id"],
                "username": row["username"],
                "text_post": row["text_post"],
                "datetime": row["datetime"],
                "relative_datetime": get_relative_time(row["datetime"])
            })
    conn.close()    
    return jsonify(posts)


@post_bp.route("/feed", methods=["GET"])
def feed():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        SELECT 
            p.id, 
            p.username, 
            p.text_post, 
            p.datetime, 
            p.media,
            p.media_type,
            p.width,
            p.heigth,
            COUNT(c.id) AS comment_count
        FROM posts p
        LEFT JOIN comments c ON c.post_id = p.id
        GROUP BY p.id, p.username, p.text_post, p.datetime, p.media, p.media_type, p.width, p.heigth
        ORDER BY p.id DESC
    """)
    posts = cur.fetchall()

    lista_posts = []
    
    # Lista para acumular updates que precisam ser persistidos no SQLite
    pending_updates_posts = []
    pending_updates_media = []

    for post in posts:
        post_id = post["id"]
        text_content = post["text_post"] or ""
        match = re.search(r'\[IMAGE\]\s*(.*)', text_content)
        
        media_url = match.group(1).strip() if match else post["media"]
        media_type = "image" if match else post["media_type"]

        main_width = post["width"] or ""
        main_heigth = post["heigth"] or ""

        # SE TIVER IMAGEM E NÃO TIVER AS MEDIDAS, BUSCA NA HORA E MARCA PRA SALVAR NO DB
        if media_url and (not main_width or not main_heigth):
            auto_w, auto_h = get_image_dimensions(media_url)
            if auto_w and auto_h:
                main_width = auto_w
                main_heigth = auto_h
                pending_updates_posts.append((main_width, main_heigth, post_id))

        cur.execute("SELECT id, media_url, media_type, width, heigth FROM post_media WHERE post_id = ?", (post_id,))
        media_rows = cur.fetchall()

        media_list = []
        if media_rows:
            for row in media_rows:
                m_id = row["id"]
                m_url = row["media_url"]
                m_w = row["width"] or ""
                m_h = row["heigth"] or ""

                if m_url and (not m_w or not m_h):
                    auto_w, auto_h = get_image_dimensions(m_url)
                    if auto_w and auto_h:
                        m_w = auto_w
                        m_h = auto_h
                        pending_updates_media.append((m_w, m_h, m_id))

                media_list.append({
                    "media_url": m_url,
                    "media_type": row["media_type"] or "image",
                    "width": m_w,
                    "heigth": m_h
                })
        else:
            media_list = parse_media_list(media_url, media_type, main_width, main_heigth)

        lista_posts.append({
            "id": post["id"],
            "username": post["username"],
            "text_post": text_content,
            "datetime": post["datetime"],
            "relative_datetime": get_relative_time(post["datetime"]),
            "comment_count": post["comment_count"],
            "media": {
                "media_url": media_url or "",
                "media_type": media_type or "image",
                "width": main_width,
                "heigth": main_heigth
            },
            "media_list": media_list,
            "media_attachments": media_list,
            "platform": "linka",
            "origin_server": root_flags.get("url-server", "")
        })

    # GRAVA TUDO DE UMA VEZ NO BANCO DE DADOS
    if pending_updates_posts:
        cur.executemany("UPDATE posts SET width = ?, heigth = ? WHERE id = ?", pending_updates_posts)
    if pending_updates_media:
        cur.executemany("UPDATE post_media SET width = ?, heigth = ? WHERE id = ?", pending_updates_media)

    if pending_updates_posts or pending_updates_media:
        conn.commit()

    conn.close()
    return jsonify(lista_posts), 200


@post_bp.route("/star", methods=["POST"])
def star():
    data = request.get_json(force=True) or {}

    post_id = data.get("post_id")
    username = getattr(g, "username", None)

    if not username:
        return jsonify({"status": "forbidden"}), 403

    if not post_id:
        return jsonify({"status": "post_id is missing"}), 400

    conn = get_db()
    cur = conn.cursor()

    cur.execute(
        "SELECT id FROM stars WHERE post_id = ? AND username = ?",
        (post_id, username)
    )
    existing = cur.fetchone()

    if existing:
        cur.execute(
            "DELETE FROM stars WHERE post_id = ? AND username = ?",
            (post_id, username)
        )
        conn.commit()
        conn.close()
        return jsonify({"status": "removed"}), 200

    else:
        cur.execute(
            "INSERT INTO stars(post_id, username) VALUES (?, ?)",
            (post_id, username)
        )
        
        cur.execute("SELECT username FROM posts WHERE id = ?", (post_id,))
        op = cur.fetchone()
        post_owner = op["username"] if op else None
        
        if post_owner:
            linkosModule.add_linkos(post_owner, "3")
            date = datetime.now()
            notificationsModule.CreateNotification(
                username, 
                post_owner, 
                date, 
                "star", 
                f"{username} starred your post!"
            )
        
        conn.commit()
        conn.close()
        return jsonify({"status": "added"}), 200


@post_bp.route("/return-stars/<int:post_id>", methods=["GET"])
def return_stars(post_id):
    conn = get_db()
    cur = conn.cursor()

    cur.execute("SELECT COUNT(*) FROM stars WHERE post_id = ?", (post_id,))
    row = cur.fetchone()
    qtd = row[0] if row else 0

    conn.close()

    return str(qtd), 200

@post_bp.route("/new-feed", methods=["POST"])
def new_feed():
    data = request.get_json()
    last_id = data.get("last_id")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT * FROM posts WHERE id = ?",(last_id,))
@post_bp.route("/has-star", methods=["POST"])
def has_star():
    data = request.get_json(force=True) or {}

    post_id = data.get("post_id")
    username = getattr(g, "username", None)

    if not post_id or not username:
        return jsonify({"starred": False}), 200

    conn = get_db()
    cur = conn.cursor()

    cur.execute(
        "SELECT 1 FROM stars WHERE post_id = ? AND username = ?",
        (post_id, username)
    )
    exists = cur.fetchone()

    conn.close()

    return jsonify({"starred": exists is not None}), 200