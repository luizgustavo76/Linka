import os
import json
import psycopg2
from psycopg2.extras import RealDictCursor
from concurrent.futures import ThreadPoolExecutor, as_completed
from flask import Blueprint, request, jsonify, g
from post import fetch_single_federation, sort_and_interleave_posts
from .mastodon_federation import get_posts
import random
base_dir = os.path.dirname(os.path.abspath(__file__))
feeds_slots_bp = Blueprint("feeds_slots", __name__)
json_path = os.path.join(base_dir, "backend.json")
with open(json_path, "r") as f:
    modules_flags = json.load(f)
root_flags = modules_flags["modules-flags"]
url_server = root_flags["url-server"]
DATABASE_URL = os.getenv("DATABASE_URL")


def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)


def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
    CREATE TABLE IF NOT EXISTS feeds_slots(
        id SERIAL PRIMARY KEY,
        username TEXT NOT NULL,
        feed_url TEXT NOT NULL,
        feed_name TEXT NOT NULL,
        slot INTEGER NOT NULL,
        UNIQUE(username, slot)
    )""")
    conn.commit()
    cur.close()
    conn.close()


create_db()
def sort_explore_post():
    platforms = ["mastodon", "bluesky", "reddit", "threads", "twitter"]
    selected = random.choice(platforms)
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT url FROM federation_index ILIKE %s",(selected))
    result = cur.fetchall()
    for urls in result:
        if selected == "mastodon":
            pass
@feeds_slots_bp.route("/new-slot", methods=["POST"])
def new_slot():
    data = request.get_json(silent=True) or {}
    username = data.get("username")

    if not username or username != g.username:
        return jsonify({"status": "unauthorized"}), 403

    feed_url = data.get("feed_url")
    feed_name = data.get("feed_name")
    target_slot = data.get("slot")

    if None in (feed_url, feed_name):
        return jsonify({"status": "feed_url or feed_name are missing"}), 400

    conn = None
    cur = None
    try:
        conn = get_db()
        cur = conn.cursor()

        cur.execute(
            "SELECT slot, feed_url, feed_name FROM feeds_slots WHERE username = %s ORDER BY slot ASC",
            (username,)
        )
        current_slots = cur.fetchall()

        for row in current_slots:
            if row["feed_url"] == feed_url:
                return jsonify({"status": "already_in_slot", "slot": row["slot"]}), 200

        if target_slot is not None:
            if target_slot not in (1, 2, 3, 4):
                return jsonify({"status": "invalid_slot"}), 400
            slot_to_save = target_slot
        else:
            occupied = {row["slot"] for row in current_slots}
            free_slots = [s for s in (1, 2, 3, 4) if s not in occupied]

            if not free_slots:
                return jsonify({
                    "status": "slots_full",
                    "slots": current_slots
                }), 409

            slot_to_save = free_slots[0]

        cur.execute("""
            INSERT INTO feeds_slots (username, feed_url, feed_name, slot)
            VALUES (%s, %s, %s, %s)
            ON CONFLICT (username, slot) DO UPDATE SET
                feed_url = EXCLUDED.feed_url,
                feed_name = EXCLUDED.feed_name
        """, (username, feed_url, feed_name, slot_to_save))

        conn.commit()
        return jsonify({"status": "ok", "slot": slot_to_save}), 200

    except Exception as e:
        if conn:
            conn.rollback()
        return jsonify({"status": "error", "message": str(e)}), 500
    finally:
        if cur:
            cur.close()
        if conn:
            conn.close()


def fetch_slot_item(item, filters=None):
    url = item["feed_url"]
    result = fetch_single_federation(url)
    posts_list = []

    if isinstance(result, list):
        posts_list = result
    elif isinstance(result, dict) and "posts" in result and isinstance(result["posts"], list):
        posts_list = result["posts"]

    filtered_posts = []
    for post in posts_list:
        if not isinstance(post, dict):
            continue
        text = post.get("text_post", "")
        if filters:
            if isinstance(filters, list) and any(f.lower() in text.lower() for f in filters if f):
                continue
            elif isinstance(filters, str) and filters.lower() in text.lower():
                continue
        filtered_posts.append(post)

    if filtered_posts:
        filtered_posts = sort_and_interleave_posts(filtered_posts)
        for index, post in enumerate(filtered_posts, start=1):
            post["id"] = index

    return {
        "feed_name": item["feed_name"],
        "feed_url": item["feed_url"],
        "slot": item["slot"],
        "posts": filtered_posts
    }


@feeds_slots_bp.route("/slot-feed", methods=["POST"])
def slot_feed():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    filters = data.get("filters")

    if not username or username != g.username:
        return jsonify({"status": "unauthorized"}), 403

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute(
            """SELECT feed_name, feed_url, slot FROM feeds_slots WHERE username = %s ORDER BY slot ASC""",
            (username,)
        )
        result = cur.fetchall()
    finally:
        cur.close()
        conn.close()

    if not result:
        return jsonify({"status": "ok", "slots": []}), 200

    slots = [
        {
            "feed_name": feed["feed_name"],
            "feed_url": feed["feed_url"],
            "slot": feed["slot"]
        }
        for feed in result
    ]

    slots_com_posts = []
    with ThreadPoolExecutor(max_workers=4) as executor:
        futures = [executor.submit(fetch_slot_item, item, filters) for item in slots]
        for future in as_completed(futures):
            try:
                slots_com_posts.append(future.result())
            except Exception as e:
                print(f"DEBUG - Erro na thread do slot: {e}")

    slots_com_posts.sort(key=lambda x: x["slot"])

    return jsonify({"status": "ok", "slots": slots_com_posts}), 200