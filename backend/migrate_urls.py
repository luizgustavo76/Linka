import os
import sqlite3
import re

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
POST_DB = os.path.join(BASE_DIR, "DB", "post.db")
PROFILE_DB = os.path.join(BASE_DIR, "DB", "profile.db")

PROXY_DOMAIN = "https://linkaProject.pythonanywhere.com/midia/"


def extract_filename(url):
    if not url or not isinstance(url, str):
        return None
    # Extrai o nome do arquivo ignorando parametros e wrappers do wsrv
    clean = url.split("?")[0].split("#")[0]
    filename = os.path.basename(clean)
    if filename and (filename.startswith("post_") or filename.startswith("profile_") or filename.endswith((".jpg", ".png", ".jpeg", ".webp"))):
        return filename
    return None


def migrate_post_db():
    if not os.path.exists(POST_DB):
        print(f"[!] Banco {POST_DB} não encontrado.")
        return

    conn = sqlite3.connect(POST_DB)
    cur = conn.cursor()

    # 1. Atualiza tabela posts (coluna media)
    cur.execute("SELECT id, media FROM posts WHERE media IS NOT NULL AND media != ''")
    rows = cur.fetchall()
    updated_posts = 0

    for post_id, media_url in rows:
        filename = extract_filename(media_url)
        if filename:
            new_url = f"{PROXY_DOMAIN}{filename}"
            cur.execute("UPDATE posts SET media = ? WHERE id = ?", (new_url, post_id))
            updated_posts += 1

    # 2. Atualiza tabela post_media (coluna media_url)
    cur.execute("SELECT id, media_url FROM post_media WHERE media_url IS NOT NULL AND media_url != ''")
    media_rows = cur.fetchall()
    updated_media = 0

    for m_id, media_url in media_rows:
        filename = extract_filename(media_url)
        if filename:
            new_url = f"{PROXY_DOMAIN}{filename}"
            cur.execute("UPDATE post_media SET media_url = ? WHERE id = ?", (new_url, m_id))
            updated_media += 1

    conn.commit()
    conn.close()
    print(f"✅ Migração em post.db concluída: {updated_posts} posts e {updated_media} mídias atualizadas.")


def migrate_profile_db():
    if not os.path.exists(PROFILE_DB):
        print(f"[!] Banco {PROFILE_DB} não encontrado.")
        return

    conn = sqlite3.connect(PROFILE_DB)
    cur = conn.cursor()

    cur.execute("SELECT username, ProfilePicture FROM profile WHERE ProfilePicture IS NOT NULL AND ProfilePicture != ''")
    rows = cur.fetchall()
    updated_profiles = 0

    for username, pic_url in rows:
        filename = extract_filename(pic_url)
        if filename:
            new_url = f"{PROXY_DOMAIN}{filename}"
            cur.execute("UPDATE profile SET ProfilePicture = ? WHERE username = ?", (new_url, username))
            updated_profiles += 1

    conn.commit()
    conn.close()
    print(f"✅ Migração em profile.db concluída: {updated_profiles} fotos de perfil atualizadas.")


if __name__ == "__main__":
    print("Iniciando migração de URLs no banco SQLite...")
    migrate_post_db()
    migrate_profile_db()
    print("🚀 Migração concluída com sucesso!")