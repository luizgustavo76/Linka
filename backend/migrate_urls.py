import os
import psycopg2
from psycopg2.extras import RealDictCursor

DATABASE_URL = os.getenv("DATABASE_URL")
# Puxa a URL pública do Render; se não houver, usa fallback
PROXY_DOMAIN = os.getenv("MY_DOMINIO_PROXY", "https://seu-app-linka.onrender.com").rstrip("/") + "/midia/"

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def migrate_post_urls():
    if not DATABASE_URL:
        print("[!] DATABASE_URL não foi configurada nas variáveis de ambiente.")
        return

    conn = get_db()
    cur = conn.cursor()
    
    # Regex SQL para capturar URLs do Supabase e extrair o nome do arquivo no grupo \1
    pattern = r"https?://[a-zA-Z0-9\.-]+\.supabase\.co/storage/v1/object/public/[^/\s\)]+/([a-zA-Z0-9_\.-]+\.(?:jpg|jpeg|png|webp))"
    replacement = f"{PROXY_DOMAIN}\\1"

    try:
        # 1. Atualiza text_post na tabela posts
        cur.execute(
            """
            UPDATE posts 
            SET text_post = REGEXP_REPLACE(text_post, %s, %s, 'g')
            WHERE text_post LIKE '%%.supabase.co%%';
            """,
            (pattern, replacement)
        )
        updated_texts = cur.rowcount

        # 2. Atualiza a coluna media na tabela posts
        cur.execute(
            """
            UPDATE posts 
            SET media = REGEXP_REPLACE(media, %s, %s, 'g')
            WHERE media LIKE '%%.supabase.co%%';
            """,
            (pattern, replacement)
        )
        updated_media = cur.rowcount

        # 3. Atualiza media_url na tabela post_media (se a tabela existir no seu schema)
        cur.execute(
            """
            UPDATE post_media 
            SET media_url = REGEXP_REPLACE(media_url, %s, %s, 'g')
            WHERE media_url LIKE '%%.supabase.co%%';
            """,
            (pattern, replacement)
        )
        updated_post_media = cur.rowcount

        conn.commit()
        print(f"✅ Tabela 'posts' e 'post_media' atualizadas no PostgreSQL:")
        print(f"   - {updated_texts} textos com [IMAGE] corrigidos.")
        print(f"   - {updated_media} colunas 'media' corrigidas.")
        print(f"   - {updated_post_media} registros na tabela 'post_media' corrigidos.")

    except Exception as e:
        conn.rollback()
        print(f"[ERRO ao migrar posts]: {e}")
    finally:
        cur.close()
        conn.close()


def migrate_profile_urls():
    if not DATABASE_URL:
        return

    conn = get_db()
    cur = conn.cursor()

    pattern = r"https?://[a-zA-Z0-9\.-]+\.supabase\.co/storage/v1/object/public/[^/\s\)]+/([a-zA-Z0-9_\.-]+\.(?:jpg|jpeg|png|webp))"
    replacement = f"{PROXY_DOMAIN}\\1"

    try:
        cur.execute(
            """
            UPDATE profile 
            SET "ProfilePicture" = REGEXP_REPLACE("ProfilePicture", %s, %s, 'g')
            WHERE "ProfilePicture" LIKE '%%.supabase.co%%';
            """,
            (pattern, replacement)
        )
        updated_profiles = cur.rowcount
        conn.commit()
        print(f"✅ Tabela 'profile' atualizada: {updated_profiles} fotos de perfil corrigidas.")

    except Exception as e:
        conn.rollback()
        print(f"[ERRO ao migrar profile]: {e}")
    finally:
        cur.close()
        conn.close()


if __name__ == "__main__":
    print("Iniciando migração de URLs via PostgreSQL REGEXP_REPLACE no Neon.tech...")
    migrate_post_urls()
    migrate_profile_urls()
    print("🚀 Migração do banco concluída com sucesso!")