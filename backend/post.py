from flask import Blueprint, request, jsonify, g
import sqlite3
import os
from datetime import datetime, timezone
import notificationsModule
import re
from urllib.parse import urlparse
import random
import linkosModule
import mentions_module
import json
from concurrent.futures import ThreadPoolExecutor, as_completed
import requests

base_dir = os.path.dirname(os.path.abspath(__file__))
json_path = os.path.join(base_dir, "backend.json")

with open(json_path, "r") as f:
    modules_flags = json.load(f)

root_flags = modules_flags.get("modules-flags", {})
db_dir = os.path.join(base_dir, "DB")
post_dir = os.path.join(db_dir, "post.db")

if root_flags.get("bluesky-federation"):
    import bluesky_federation.posts as bluesky
if root_flags.get("reddit-federation"):
    import reddit_federation.posts as reddit
if root_flags.get("mastodon-federation"):
    import mastodon_federation.posts as mastodon

post_bp = Blueprint("post", __name__)

if not os.path.exists(db_dir):
    os.makedirs(db_dir)


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
            text_post TEXT,
            datetime TEXT
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
    conn.commit()
    conn.close()


create_db()


def parse_datetime(val):
    """Normaliza Unix Timestamps e strings ISO para datetime comparável."""
    if not val:
        return datetime.min.replace(tzinfo=timezone.utc)
    
    if isinstance(val, (int, float)) or (isinstance(val, str) and val.replace(".", "", 1).isdigit()):
        try:
            return datetime.fromtimestamp(float(val), tz=timezone.utc)
        except Exception:
            pass

    val_str = str(val).strip()
    formats = [
        "%Y-%m-%dT%H:%M:%S.%fZ",
        "%Y-%m-%dT%H:%M:%SZ",
        "%Y-%m-%dT%H:%M:%S%z",
        "%Y-%m-%d %H:%M:%S",
        "%a, %d %b %Y %H:%M:%S %z"
    ]
    for fmt in formats:
        try:
            dt = datetime.strptime(val_str, fmt)
            return dt if dt.tzinfo else dt.replace(tzinfo=timezone.utc)
        except ValueError:
            continue

    return datetime.min.replace(tzinfo=timezone.utc)


def extract_posts_from_response(result):
    """Extrai listas de posts de múltiplos formatos de API (Bluesky, Reddit, Mastodon, etc)."""
    if not result:
        return []
    if isinstance(result, list):
        return result
    if isinstance(result, dict):
        for key in ["posts", "feed", "items", "data"]:
            if key in result and isinstance(result[key], list):
                return result[key]
        
        if "data" in result and isinstance(result["data"], dict):
            children = result["data"].get("children")
            if isinstance(children, list):
                return [item.get("data", item) for item in children if isinstance(item, dict)]

    return []


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
            "id": single_posts[0],
            "username": single_posts[1],
            "text_post": single_posts[2],
            "datetime": single_posts[3]
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
    data = request.get_json(force=True)
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
        post_owner = op[0] if op else None
        date = datetime.now()
        if post_owner:
            notificationsModule.CreateNotification(username, post_owner, date, "comment", text_comment)
        return jsonify({"status": "the comment has been created with sucess!"}), 200
    else:
        return jsonify({"status": "forbidden"}), 403


@post_bp.route("/view-comments", methods=["POST"])
def view_comments():
    data = request.get_json(force=True)
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
    """Busca os posts locais do banco de dados sem usar jsonify()."""
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT id, username, text_post, datetime FROM posts ORDER BY id DESC")
    posts = cur.fetchall()
    conn.close()

    lista_posts = []
    for post in posts:
        lista_posts.append({
            "id": post[0],
            "username": post[1],
            "text_post": post[2],
            "datetime": post[3]
        })
    return lista_posts


def fetch_single_federation(node_url):
    parsed = urlparse(node_url)
    netloc = parsed.netloc.lower()
    path = parsed.path.rstrip('/')

    if any(host in netloc for host in ["linkaproject.pythonanywhere.com", "127.0.0.1", "localhost"]) or not netloc:
        try:
            if path == "/feed":
                return get_internal_feed_posts()

            elif path.startswith("/feed/reddit/"):
                if root_flags.get("reddit-federation") and 'reddit' in globals():
                    sub_name = path.replace("/feed/reddit/", "")
                    if hasattr(reddit, "fetch_reddit_posts"):
                        return reddit.fetch_reddit_posts(subreddit=sub_name)

            elif path.startswith("/feed/mastodon/"):
                if root_flags.get("mastodon-federation") and 'mastodon' in globals():
                    tag_name = path.replace("/feed/mastodon/", "")
                    if hasattr(mastodon, "fetch_mastodon_posts"):
                        return mastodon.fetch_mastodon_posts(tag=tag_name, limit=100)

            elif path.startswith("/feed/bluesky/"):
                if root_flags.get("bluesky-federation") and 'bluesky' in globals():
                    tag_name = path.replace("/feed/bluesky/", "")
                    if hasattr(bluesky, "fetch_bluesky_posts"):
                        return bluesky.fetch_bluesky_posts(query=tag_name, limit=100)

        except Exception as e:
            print(f"DEBUG - Erro na rota interna {path}: {e}")
            return None

    try:
        response = requests.get(node_url, timeout=6.0)
        if response.status_code == 200:
            return response.json()
    except Exception as e:
        print(f"DEBUG - Erro na requisição externa {node_url}: {e}")
        return None

    return None


@post_bp.route("/view-external-posts", methods=["POST"])
def view_external_posts():
    data = request.get_json() or {}
    urls = data.get("urls")

    if not urls or not isinstance(urls, list):
        return jsonify({"error": "url invalid or bad formatted"}), 400

    if len(urls) > 50:
        return jsonify({"status": "max limit with federation url"}), 400

    results_by_source = []

    with ThreadPoolExecutor(max_workers=50) as executor:
        futures = [executor.submit(fetch_single_federation, url) for url in urls]
        for future in as_completed(futures):
            try:
                raw_result = future.result()
                extracted_posts = extract_posts_from_response(raw_result)
                if extracted_posts:
                    results_by_source.append(extracted_posts)
            except Exception as e:
                print(f"DEBUG - Erro na thread: {e}")

    if not results_by_source:
        return jsonify([]), 200

    interleaved_posts = []
    max_len = max(len(s) for s in results_by_source)
    for i in range(max_len):
        for source in results_by_source:
            if i < len(source):
                interleaved_posts.append(source[i])

    interleaved_posts.sort(
        key=lambda x: parse_datetime(
            x.get("datetime") or x.get("created_at") or x.get("createdAt") or x.get("created_utc")
        ) if isinstance(x, dict) else parse_datetime(None),
        reverse=True
    )

    formated_feed = []
    for index, post in enumerate(interleaved_posts, start=1):
        if isinstance(post, dict):
            post["id"] = index
            formated_feed.append(post)

    return jsonify(formated_feed), 200


@post_bp.route("/new", methods=["POST"])
def new_post():
    try:
        data = request.get_json(silent=True)

        if data is None:
            return jsonify({"status": "JSON inválido ou vazio"}), 400

        username = data.get("username")
        current_user = getattr(g, "username", None)

        if username == current_user:
            text_post = data.get("text_post")
            datetime_post = data.get("datetime")

            if not username:
                return jsonify({"status": "username not send"}), 400

            if not text_post:
                return jsonify({"status": "its not possible create a post with no cotent"}), 400

            if "@" in text_post:
                users_mention = re.findall(r'@([^\s]+)', text_post)
                for user in users_mention:
                    date = datetime.now()
                    notificationsModule.CreateNotification(username, user, date, "mention", f"{username} mentioned you in a post")
                    mentions_module.createMention(username, user, text_post, None, None, "post")

            conn = get_db()
            cur = conn.cursor()
            cur.execute(
                "INSERT INTO posts(username, text_post, datetime) VALUES (?, ?, ?)",
                (username, text_post, datetime_post)
            )
            conn.commit()
            conn.close()

            linkosModule.add_linkos(username, "5")
            return jsonify({"status": "post created with sucess"}), 200
        else:
            return jsonify({"status": "forbidden"}), 403
    except Exception as e:
        print("ERROR", e)
        return jsonify({"status": "error internal"}), 500


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
        posts_id.append(row[0])
    for i in posts_id:
        cur.execute("SELECT * FROM posts WHERE id = ?", (i,))
        result = cur.fetchall()
        for row in result:
            posts.append({
                "id": row[0],
                "username": row[1],
                "text_post": row[2],
                "datetime": row[3]
            })
    conn.close()
    return jsonify(posts)


@post_bp.route("/feed", methods=["GET"])
def feed():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT id, username, text_post, datetime FROM posts ORDER BY id DESC")
    posts = cur.fetchall()
    conn.close()

    lista_posts = []
    for post in posts:
        lista_posts.append({
            "id": post[0],
            "username": post[1],
            "text_post": post[2],
            "datetime": post[3]
        })

    return jsonify(lista_posts), 200


@post_bp.route("/star", methods=["POST"])
def star():
    data = request.get_json(force=True)

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

        if op and op[0]:
            post_owner = op[0]
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
    qtd = cur.fetchone()[0]

    conn.close()

    return str(qtd), 200


@post_bp.route("/has-star", methods=["POST"])
def has_star():
    data = request.get_json(force=True)

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