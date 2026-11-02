import os
import sqlite3
import psycopg2
from psycopg2.extras import RealDictCursor
import dotenv

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
dotenv.load_dotenv(os.path.join(BASE_DIR, "backend.env"))

DATABASE_URL = os.getenv("DATABASE_URL")
DB_DIR = os.path.join(BASE_DIR, "DB")

if not DATABASE_URL:
    print("[!] DATABASE_URL não configurada no backend.env!")
    exit(1)

pg_conn = psycopg2.connect(DATABASE_URL)
pg_cur = pg_conn.cursor()

def migrar_tabela_posts():
    post_db_path = os.path.join(DB_DIR, "post.db")
    if not os.path.exists(post_db_path):
        return

    print("📦 Migrando post.db -> Neon.tech...")
    sqlite_conn = sqlite3.connect(post_db_path)
    sqlite_conn.row_factory = sqlite3.Row
    sq_cur = sqlite_conn.cursor()

    # 1. Tabela posts
    sq_cur.execute("SELECT id, username, media, media_type, width, heigth, text_post, datetime FROM posts")
    posts = sq_cur.fetchall()
    for p in posts:
        pg_cur.execute("""
            INSERT INTO posts (id, username, media, media_type, width, heigth, text_post, datetime)
            VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
            ON CONFLICT (id) DO NOTHING;
        """, (p["id"], p["username"], p["media"], p["media_type"], p["width"], p["heigth"], p["text_post"], p["datetime"]))

    # Ajusta a sequência do SERIAL no Postgres para os próximos inserts não colidirem
    pg_cur.execute("SELECT setval('posts_id_seq', (SELECT MAX(id) FROM posts));")

    # 2. Tabela post_media (se existir)
    try:
        sq_cur.execute("SELECT id, post_id, media_url, media_type, width, heigth FROM post_media")
        medias = sq_cur.fetchall()
        for m in medias:
            pg_cur.execute("""
                INSERT INTO post_media (id, post_id, media_url, media_type, width, heigth)
                VALUES (%s, %s, %s, %s, %s, %s)
                ON CONFLICT (id) DO NOTHING;
            """, (m["id"], m["post_id"], m["media_url"], m["media_type"], m["width"], m["heigth"]))
        pg_cur.execute("SELECT setval('post_media_id_seq', (SELECT MAX(id) FROM post_media));")
    except Exception:
        pass

    # 3. Tabela comments
    try:
        sq_cur.execute("SELECT id, text_comment, post_id, username FROM comments")
        comments = sq_cur.fetchall()
        for c in comments:
            pg_cur.execute("""
                INSERT INTO comments (id, text_comment, post_id, username)
                VALUES (%s, %s, %s, %s)
                ON CONFLICT (id) DO NOTHING;
            """, (c["id"], c["text_comment"], c["post_id"], c["username"]))
        pg_cur.execute("SELECT setval('comments_id_seq', (SELECT MAX(id) FROM comments));")
    except Exception:
        pass

    # 4. Tabela stars
    try:
        sq_cur.execute("SELECT id, post_id, username FROM stars")
        stars = sq_cur.fetchall()
        for s in stars:
            pg_cur.execute("""
                INSERT INTO stars (id, post_id, username)
                VALUES (%s, %s, %s)
                ON CONFLICT (id) DO NOTHING;
            """, (s["id"], s["post_id"], s["username"]))
        pg_cur.execute("SELECT setval('stars_id_seq', (SELECT MAX(id) FROM stars));")
    except Exception:
        pass

    sqlite_conn.close()

def migrar_tabela_profiles():
    profile_db_path = os.path.join(DB_DIR, "profile.db")
    if not os.path.exists(profile_db_path):
        return

    print("👤 Migrando profile.db -> Neon.tech...")
    sqlite_conn = sqlite3.connect(profile_db_path)
    sqlite_conn.row_factory = sqlite3.Row
    sq_cur = sqlite_conn.cursor()

    try:
        sq_cur.execute("SELECT username, bio, actual_badge, ProfilePicture, followers, following, hex_banner, profile_banner_color FROM profile")
        profiles = sq_cur.fetchall()
        for pr in profiles:
            pg_cur.execute("""
                INSERT INTO profile (username, bio, actual_badge, "ProfilePicture", followers, following, hex_banner, profile_banner_color)
                VALUES (%s, %s, %s, %s, %s, %s, %s, %s)
                ON CONFLICT (username) DO NOTHING;
            """, (pr["username"], pr["bio"], pr["actual_badge"], pr["ProfilePicture"], pr["followers"], pr["following"], pr["hex_banner"], pr["profile_banner_color"]))
    except Exception as e:
        print(f"Aviso no profile: {e}")

    sqlite_conn.close()

if __name__ == "__main__":
    # Importa os módulos para garantir que create_db() rodou e as tabelas existem
    import post
    import profiles
    post.create_db()
    profiles.create_table()

    migrar_tabela_posts()
    migrar_tabela_profiles()

    pg_conn.commit()
    pg_cur.close()
    pg_conn.close()
    print("🚀 MIGRAÇÃO CONCLUÍDA COM SUCESSO! Todos os dados estão no Neon.tech!")