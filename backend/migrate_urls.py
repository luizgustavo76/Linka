import os
import sqlite3
import re

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
POST_DB = os.path.join(BASE_DIR, "DB", "post.db")
PROFILE_DB = os.path.join(BASE_DIR, "DB", "profile.db")

PROXY_DOMAIN = "https://linkaProject.pythonanywhere.com/midia/"

# Regex para capturar qualquer URL antiga do Supabase
SUPABASE_URL_PATTERN = re.compile(
    r"https?://[a-zA-Z0-9\.-]+\.supabase\.co/storage/v1/object/public/[^/\s\)]+/([a-zA-Z0-9_\.-]+\.(?:jpg|jpeg|png|webp))"
)


def replace_supabase_url(text):
    if not text or not isinstance(text, str):
        return text, False

    def _replacer(match):
        filename = match.group(1)
        return f"{PROXY_DOMAIN}{filename}"

    new_text, count = SUPABASE_URL_PATTERN.subn(_replacer, text)
    return new_text, count > 0


def migrate_post_db():
    if not os.path.exists(POST_DB):
        print(f"[!] Banco {POST_DB} não encontrado.")
        return

    conn = sqlite3.connect(POST_DB)
    cur = conn.cursor()

    # 1. Atualiza a coluna text_post (onde o [IMAGE] url fica guardado)
    cur.execute("SELECT id, text_post FROM posts WHERE text_post LIKE '%.supabase.co%'")
    rows_text = cur.fetchall()
    updated_texts = 0

    for post_id, text_content in rows_text:
        new_text, changed = replace_supabase_url(text_content)
        if changed:
            cur.execute("UPDATE posts SET text_post = ? WHERE id = ?", (new_text, post_id))
            updated_texts += 1

    # 2. Atualiza a coluna media (se houver)
    cur.execute("SELECT id, media FROM posts WHERE media LIKE '%.supabase.co%'")
    rows_media = cur.fetchall()
    updated_posts_media = 0

    for post_id, media_url in rows_media:
        new_media, changed = replace_supabase_url(media_url)
        if changed:
            cur.execute("UPDATE posts SET media = ? WHERE id = ?", (new_media, post_id))
            updated_posts_media += 1

    # 3. Atualiza a tabela post_media
    cur.execute("SELECT id, media_url FROM post_media WHERE media_url LIKE '%.supabase.co%'")
    media_rows = cur.fetchall()
    updated_post_media_table = 0

    for m_id, media_url in media_rows:
        new_media, changed = replace_supabase_url(media_url)
        if changed:
            cur.execute("UPDATE post_media SET media_url = ? WHERE id = ?", (new_media, m_id))
            updated_post_media_table += 1

    conn.commit()
    conn.close()
    print(f"✅ post.db atualizado:")
    print(f"   - {updated_texts} textos com [IMAGE] corrigidos.")
    print(f"   - {updated_posts_media} colunas 'media' corrigidas.")
    print(f"   - {updated_post_media_table} mídias na tabela 'post_media' corrigidas.")


def migrate_profile_db():
    if not os.path.exists(PROFILE_DB):
        print(f"[!] Banco {PROFILE_DB} não encontrado.")
        return

    conn = sqlite3.connect(PROFILE_DB)
    cur = conn.cursor()

    cur.execute("SELECT username, ProfilePicture FROM profile WHERE ProfilePicture LIKE '%.supabase.co%'")
    rows = cur.fetchall()
    updated_profiles = 0

    for username, pic_url in rows:
        new_url, changed = replace_supabase_url(pic_url)
        if changed:
            cur.execute("UPDATE profile SET ProfilePicture = ? WHERE username = ?", (new_url, username))
            updated_profiles += 1

    conn.commit()
    conn.close()
    print(f"✅ profile.db atualizado: {updated_profiles} fotos de perfil corrigidas.")


if __name__ == "__main__":
    print("Iniciando migração Regex no SQLite...")
    migrate_post_db()
    migrate_profile_db()
    print("🚀 Migração concluída com sucesso!")