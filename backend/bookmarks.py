from flask import Flask, jsonify, request, g, Blueprint
import sqlite3
import os

base_dir = os.path.dirname(os.path.abspath(__file__))
bookmarks_bp = Blueprint("bookmarks_bp", __name__)

db_dir = os.path.join(base_dir, "DB")
post_db_path = os.path.join(db_dir, "bookmarks.db")
posts_main_db_path = os.path.join(db_dir, "post.db")

if not os.path.exists(db_dir):
    os.makedirs(db_dir)

def get_db():
    conn = sqlite3.connect(post_db_path, timeout=10)
    conn.row_factory = sqlite3.Row
    
    # Anexa o posts.db sob o alias 'posts_db'
    if os.path.exists(posts_main_db_path):
        try:
            conn.execute(f"ATTACH DATABASE '{posts_main_db_path}' AS posts_db")
        except Exception as e:
            print(f"[Bookmarks] Erro ao anexar posts.db: {e}")
        
    return conn

def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS bookmarks(
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            username TEXT NOT NULL,
            post_id TEXT NOT NULL,
            is_public INTEGER DEFAULT 1,
            is_external INTEGER DEFAULT 0,
            source_url TEXT,
            original_id TEXT,
            platform TEXT,
            UNIQUE(username, post_id)
        )
    """)
    conn.commit()
    conn.close()

create_db()

@bookmarks_bp.route("/view-bookmarks", methods=["POST"])
def view_bookmarks():
    data = request.get_json(silent=True) or {}
    username = data.get("username")
    receiver = data.get("receiver") or username
    current_user = getattr(g, "username", None)

    if username and username == current_user:
        conn = get_db()
        cur = conn.cursor()
        
        # Faz o LEFT JOIN selecionando APENAS as colunas que existem na sua tabela 'posts'
        if receiver == username:
            query = """
                SELECT 
                    b.id AS bookmark_id,
                    b.username AS bookmark_user,
                    b.post_id,
                    b.is_public,
                    b.is_external,
                    b.platform,
                    p.id,
                    p.username,
                    p.text_post,
                    p.datetime,
                    p.media,
                    p.media_type,
                    p.width,
                    p.heigth
                FROM bookmarks b
                LEFT JOIN posts_db.posts p ON b.post_id = p.id
                WHERE b.username = ?
                ORDER BY b.id DESC
            """
            cur.execute(query, (username,))
        else:
            query = """
                SELECT 
                    b.id AS bookmark_id,
                    b.username AS bookmark_user,
                    b.post_id,
                    b.is_public,
                    b.is_external,
                    b.platform,
                    p.id,
                    p.username,
                    p.text_post,
                    p.datetime,
                    p.media,
                    p.media_type,
                    p.width,
                    p.heigth
                FROM bookmarks b
                LEFT JOIN posts_db.posts p ON b.post_id = p.id
                WHERE b.username = ? AND b.is_public = 1
                ORDER BY b.id DESC
            """
            cur.execute(query, (receiver,))
            
        rows = cur.fetchall()
        
        # Converte as linhas do SQLite para uma lista de dicionários ajustando objetos de mídia se existirem
        result = []
        for row in rows:
            post_dict = dict(row)
            
            # Se o post tiver uma mídia simples, monta a estrutura tratada que o cliente Android espera
            if post_dict.get("media"):
                post_dict["media_list"] = [{
                    "media_url": post_dict.get("media"),
                    "media_type": post_dict.get("media_type", "image"),
                    "width": post_dict.get("width", ""),
                    "heigth": post_dict.get("heigth", "")
                }]
            result.append(post_dict)

        conn.close()
        return jsonify(result), 200
    
    return jsonify({"status": "forbidden"}), 403

@bookmarks_bp.route("/toggle-bookmark", methods=["POST"])
def toggle_bookmark():
    data = request.get_json(silent=True) or {}
    username = data.get("username")

    if username == getattr(g, "username", None):
        post_id = data.get("post_id")
        is_public = data.get("is_public")
        original_id = data.get("original_id")
        is_external = data.get("is_external")
        source_url = data.get("source_url")
        platform = data.get("platform")

        if None in (is_public, is_external, platform) or not post_id:
            return jsonify({"status": "missing data"}), 400

        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute("SELECT id FROM bookmarks WHERE username = ? AND (post_id = ? OR original_id = ?)", (username, post_id, original_id))
            result = cur.fetchone()

            if result:
                cur.execute("DELETE FROM bookmarks WHERE username = ? AND (post_id = ? OR original_id = ?)", (username, post_id, original_id))
                conn.commit()
                return jsonify({"status": "bookmark removed"}), 200
            else:
                cur.execute("""
                    INSERT INTO bookmarks (username, post_id, is_public, is_external, source_url, platform, original_id)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                """, (username, post_id, is_public, is_external, source_url, platform, original_id))
                conn.commit()
                return jsonify({"status": "bookmark inserted"}), 200
        except Exception as e:
            return jsonify({"status": "error", "message": str(e)}), 500
        finally:
            conn.close()
            
    return jsonify({"status": "forbidden"}), 403