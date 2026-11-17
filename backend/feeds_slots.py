import os
import re
import time
import json
import random
import threading
import psycopg2
from psycopg2.extras import RealDictCursor
from concurrent.futures import ThreadPoolExecutor, as_completed
from urllib.parse import urlparse
from datetime import datetime, timedelta
from flask import Blueprint, request, jsonify, g
from post import (
    fetch_single_federation,
    get_relative_time,
    parse_media_list,
    parse_datetime_flexible
)

base_dir = os.path.dirname(os.path.abspath(__file__))
feeds_slots_bp = Blueprint("feeds_slots", __name__)
json_path = os.path.join(base_dir, "backend.json")
with open(json_path, "r") as f:
    modules_flags = json.load(f)
root_flags = modules_flags["modules-flags"]
url_server = root_flags["url-server"]
DATABASE_URL = os.getenv("DATABASE_URL")

_CACHE_LOCK = threading.Lock()
_URL_CACHE = {}
_USER_FEED_CACHE = {}
URL_CACHE_TTL = 300
USER_FEED_TTL = 90


def get_cached_url(url):
    now = time.time()
    with _CACHE_LOCK:
        entry = _URL_CACHE.get(url)
        if entry and (now - entry["ts"]) < URL_CACHE_TTL:
            return entry["data"]
    return None


def set_cached_url(url, data):
    with _CACHE_LOCK:
        if len(_URL_CACHE) > 200:
            _URL_CACHE.clear()
        _URL_CACHE[url] = {"ts": time.time(), "data": data}


def invalidate_user_cache(username):
    with _CACHE_LOCK:
        keys_to_del = [k for k in _USER_FEED_CACHE if k.startswith(f"{username}:")]
        for k in keys_to_del:
            del _USER_FEED_CACHE[k]


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


def get_linka_native_posts(current_user="", limit=30):
    conn = get_db()
    cur = conn.cursor()
    try:
        query = """
            SELECT 
                p.id, 
                p.username, 
                p.text_post, 
                p.datetime, 
                p.media,
                p.media_type,
                p.width,
                p.heigth,
                p.thumbnail,
                m."ProfilePicture" AS avatar_url,
                COUNT(c.id) AS comment_count,
                CASE 
                    WHEN poll.id IS NULL THEN NULL
                    ELSE json_build_object(
                        'poll_id', poll.id,
                        'title', poll.title,
                        'expires_at', poll.expires_at,
                        'user_voted_option_id', pv.option_id,
                        'options', COALESCE(
                            (
                                SELECT json_agg(
                                    json_build_object(
                                        'id', po.id,
                                        'text', po.option_text,
                                        'votes', po.votes_count
                                    ) ORDER BY po.id ASC
                                )
                                FROM poll_options po
                                WHERE po.poll_id = poll.id
                            ),
                            '[]'::json
                        )
                    )
                END AS poll
            FROM posts p
            LEFT JOIN profile m ON p.username = m.username
            LEFT JOIN comments c ON c.post_id = p.id
            LEFT JOIN poll ON poll.post_id = p.id::text
            LEFT JOIN poll_votes pv ON pv.poll_id = poll.id AND pv.username = %s
            GROUP BY 
                p.id, p.username, p.text_post, m."ProfilePicture", 
                p.thumbnail, p.datetime, p.media, p.media_type, 
                p.width, p.heigth, poll.id, pv.option_id
            ORDER BY p.id DESC
            LIMIT %s
        """
        cur.execute(query, (current_user or "", limit))
        posts = cur.fetchall()

        if not posts:
            return []

        post_ids = [p["id"] for p in posts]
        cur.execute("""
            SELECT id, post_id, media_url, media_type, width, heigth, thumbnail
            FROM post_media 
            WHERE post_id = ANY(%s)
        """, (post_ids,))
        all_media_rows = cur.fetchall()

        media_by_post = {}
        for row in all_media_rows:
            pid = row["post_id"]
            if pid not in media_by_post:
                media_by_post[pid] = []
            media_by_post[pid].append({
                "media_url": row["media_url"],
                "media_type": row["media_type"] or "image",
                "width": row["width"] or "",
                "heigth": row["heigth"] or "",
                "thumbnail": row["thumbnail"] or ""
            })

        lista_posts = []
        for post in posts:
            post_id = post["id"]
            text_content = post["text_post"] or ""
            match = re.search(r'\[IMAGE\]\s*(.*)', text_content)
            avatar_url = post["avatar_url"] or ""
            media_url = match.group(1).strip() if match else post["media"]
            media_type = "image" if match else post["media_type"]

            main_width = post["width"] or ""
            main_heigth = post["heigth"] or ""
            main_thumbnail = post["thumbnail"] or ""
            if post_id in media_by_post:
                media_list = media_by_post[post_id]
            else:
                media_list = parse_media_list(media_url, media_type, main_width, main_heigth)

            dt_val = str(post["datetime"]) if post["datetime"] else ""

            lista_posts.append({
                "id": post["id"],
                "username": post["username"],
                "text_post": text_content,
                "datetime": dt_val,
                "avatar_url": avatar_url,
                "relative_datetime": get_relative_time(post["datetime"]),
                "comment_count": post["comment_count"],
                "poll": post["poll"],
                "media": {
                    "media_url": media_url or "",
                    "media_type": media_type or "image",
                    "width": main_width,
                    "heigth": main_heigth,
                    "thumbnail": main_thumbnail
                },
                "media_list": media_list,
                "media_attachments": media_list,
                "platform": "linka",
                "origin_server": url_server
            })

        return lista_posts
    finally:
        cur.close()
        conn.close()


def extract_platform_from_url(url):
    parts = [p for p in urlparse(url).path.strip("/").split("/") if p]
    if len(parts) >= 2 and parts[0].lower() == "feed":
        return parts[1].lower()
    return "other"


def fetch_federation_cached(url):
    cached = get_cached_url(url)
    if cached is not None:
        return cached
    data = fetch_single_federation(url)
    if data:
        set_cached_url(url, data)
    return data


def fetch_by_url(url, current_user=""):
    parsed = urlparse(url)
    path = parsed.path.rstrip("/")
    if path == "/feed":
        return get_linka_native_posts(current_user=current_user, limit=30)
    return fetch_federation_cached(url)


def filter_posts_list(result, filters=None, fallback_platform=""):
    posts_list = []
    if isinstance(result, list):
        posts_list = result
    elif isinstance(result, dict) and "posts" in result and isinstance(result["posts"], list):
        posts_list = result["posts"]

    filtered = []
    for post in posts_list:
        if not isinstance(post, dict):
            continue
        text = post.get("text_post", "")
        if filters:
            if isinstance(filters, list) and any(f.lower() in text.lower() for f in filters if f):
                continue
            elif isinstance(filters, str) and filters.lower() in text.lower():
                continue
        item = dict(post)
        if not item.get("platform") and fallback_platform:
            item["platform"] = fallback_platform
        if item.get("id") is None:
            orig_id = item.get("unique_id") or item.get("uri") or item.get("cid")
            if orig_id is not None:
                item["id"] = orig_id
        filtered.append(item)

    return filtered


def get_explore_candidate_pool(exclude_urls=None, exclude_platforms=None, filters=None, max_sources=2):
    exclude_urls = exclude_urls or set()
    exclude_platforms = {p.lower() for p in (exclude_platforms or set())}

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("SELECT url FROM federation_index WHERE url IS NOT NULL AND url != ''")
        rows = cur.fetchall()
    finally:
        cur.close()
        conn.close()

    by_platform = {}
    for r in rows:
        u = r.get("url")
        if not u or u in exclude_urls or u.rstrip("/").endswith("/feed"):
            continue
        plat = extract_platform_from_url(u)
        if plat in exclude_platforms:
            continue
        by_platform.setdefault(plat, []).append(u)

    if not by_platform:
        return []

    platforms = list(by_platform.keys())
    random.shuffle(platforms)

    selected = []
    for plat in platforms[:max_sources]:
        cached_candidates = [u for u in by_platform[plat] if get_cached_url(u) is not None]
        chosen_u = random.choice(cached_candidates) if cached_candidates else random.choice(by_platform[plat])
        selected.append((plat, chosen_u))

    pool = []
    with ThreadPoolExecutor(max_workers=len(selected)) as executor:
        future_map = {
            executor.submit(fetch_federation_cached, u): plat
            for plat, u in selected
        }
        for future, plat in future_map.items():
            try:
                res = future.result(timeout=15)
                posts = filter_posts_list(res, filters, fallback_platform=plat)
                random.shuffle(posts)
                pool.extend(posts[:5])
            except Exception as e:
                print(f"DEBUG - Erro ao buscar pool explore ({plat}): {e}")

    random.shuffle(pool)
    return pool


def pop_next_different_platform(pool, last_platform):
    if not pool:
        return None
    for idx, item in enumerate(pool):
        if item.get("platform", "").lower() != (last_platform or "").lower():
            return pool.pop(idx)
    return pool.pop(0)


def fill_temporal_gaps(
    base_posts,
    external_pool,
    hours_per_post=12.0,
    max_posts_per_gap=2,
    max_total_external=8
):
    if not external_pool or max_total_external <= 0:
        return list(base_posts)

    available_pool = list(external_pool)
    injected_total = 0
    now = datetime.now()

    if not base_posts:
        picked = []
        last_plat = ""
        while available_pool and len(picked) < min(5, max_total_external):
            item = pop_next_different_platform(available_pool, last_plat)
            if not item:
                break
            last_plat = item.get("platform", "")
            picked.append(item)
        return picked

    sorted_base = sorted(
        base_posts,
        key=lambda x: parse_datetime_flexible(x.get("datetime")),
        reverse=True
    )

    result = []
    last_platform = ""

    first_dt = parse_datetime_flexible(sorted_base[0].get("datetime"))
    if first_dt != datetime.min and available_pool and injected_total < max_total_external:
        top_gap_hours = (now - first_dt).total_seconds() / 3600.0
        top_fill = min(
            int(top_gap_hours / hours_per_post),
            max_posts_per_gap,
            max_total_external - injected_total
        )
        for step in range(top_fill):
            if not available_pool:
                break
            filler = pop_next_different_platform(available_pool, last_platform)
            if not filler:
                break
            last_platform = filler.get("platform", "")
            injected_total += 1
            fake_dt = now - timedelta(hours=(step + 1) * (top_gap_hours / (top_fill + 1)))
            filler["datetime"] = fake_dt.strftime("%Y-%m-%d %H:%M:%S")
            filler["relative_datetime"] = get_relative_time(fake_dt)
            result.append(filler)

    for i in range(len(sorted_base)):
        current_post = sorted_base[i]
        result.append(current_post)
        last_platform = current_post.get("platform", last_platform)

        if i + 1 < len(sorted_base) and available_pool and injected_total < max_total_external:
            dt_newer = parse_datetime_flexible(current_post.get("datetime"))
            dt_older = parse_datetime_flexible(sorted_base[i + 1].get("datetime"))

            if dt_newer != datetime.min and dt_older != datetime.min and dt_newer > dt_older:
                gap_hours = (dt_newer - dt_older).total_seconds() / 3600.0
                fill_count = min(
                    int(gap_hours / hours_per_post),
                    max_posts_per_gap,
                    max_total_external - injected_total
                )

                if fill_count > 0:
                    step_hours = gap_hours / (fill_count + 1)
                    for step in range(1, fill_count + 1):
                        if not available_pool:
                            break
                        filler = pop_next_different_platform(available_pool, last_platform)
                        if not filler:
                            break
                        last_platform = filler.get("platform", "")
                        injected_total += 1
                        interpolated_dt = dt_newer - timedelta(hours=step * step_hours)
                        filler["datetime"] = interpolated_dt.strftime("%Y-%m-%d %H:%M:%S")
                        filler["relative_datetime"] = get_relative_time(interpolated_dt)
                        result.append(filler)

    return result


def sort_strict_no_repeat_platform(all_posts):
    for post in all_posts:
        post["_dt_obj"] = parse_datetime_flexible(post.get("datetime"))
    sorted_posts = sorted(all_posts, key=lambda x: x["_dt_obj"], reverse=True)

    final_feed = []
    last_platform = None

    while sorted_posts:
        chosen_idx = 0
        if last_platform and len(sorted_posts) > 1:
            for idx in range(len(sorted_posts)):
                if sorted_posts[idx].get("platform", "").lower() != last_platform:
                    chosen_idx = idx
                    break

        post = sorted_posts.pop(chosen_idx)
        last_platform = post.get("platform", "").lower()
        del post["_dt_obj"]
        final_feed.append(post)

    return final_feed


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
        invalidate_user_cache(username)
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


@feeds_slots_bp.route("/slot-feed", methods=["POST", "GET"])
def slot_feed():
    data = request.get_json(silent=True) or {}
    username = data.get("username") or request.args.get("username") or getattr(g, "username", None)
    filters = data.get("filters")

    if not username or username != g.username:
        return jsonify({"status": "unauthorized"}), 403

    cache_key = f"{username}:{json.dumps(filters, sort_keys=True)}"
    now = time.time()
    with _CACHE_LOCK:
        cached_feed = _USER_FEED_CACHE.get(cache_key)
        if cached_feed and (now - cached_feed["ts"]) < USER_FEED_TTL:
            return jsonify(cached_feed["data"]), 200

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute(
            """SELECT feed_name, feed_url, slot FROM feeds_slots WHERE username = %s ORDER BY slot ASC""",
            (username,)
        )
        saved_slots = cur.fetchall()
    finally:
        cur.close()
        conn.close()

    base_posts = filter_posts_list(
        get_linka_native_posts(current_user=username, limit=30),
        filters,
        fallback_platform="linka"
    )
    occupied_urls = {row["feed_url"] for row in saved_slots}
    occupied_platforms = {extract_platform_from_url(row["feed_url"]) for row in saved_slots}

    if saved_slots:
        with ThreadPoolExecutor(max_workers=4) as executor:
            future_map = {
                executor.submit(fetch_by_url, row["feed_url"], username): extract_platform_from_url(row["feed_url"])
                for row in saved_slots
            }
            for future, plat in future_map.items():
                try:
                    res = future.result(timeout=15)
                    base_posts.extend(filter_posts_list(res, filters, fallback_platform=plat))
                except Exception as e:
                    print(f"DEBUG - Erro na thread do slot ({plat}): {e}")

    has_empty_slots = len(saved_slots) < 4
    if has_empty_slots:
        max_external = max(2, min(8, len(base_posts)))
        explore_pool = get_explore_candidate_pool(
            exclude_urls=occupied_urls,
            exclude_platforms=occupied_platforms,
            filters=filters,
            max_sources=2
        )
        combined_posts = fill_temporal_gaps(
            base_posts,
            explore_pool,
            hours_per_post=12.0,
            max_posts_per_gap=2,
            max_total_external=max_external
        )
    else:
        combined_posts = base_posts

    if not combined_posts:
        return jsonify([]), 200

    final_feed = sort_strict_no_repeat_platform(combined_posts)

    with _CACHE_LOCK:
        if len(_USER_FEED_CACHE) > 200:
            _USER_FEED_CACHE.clear()
        _USER_FEED_CACHE[cache_key] = {"ts": time.time(), "data": final_feed}

    return jsonify(final_feed), 200