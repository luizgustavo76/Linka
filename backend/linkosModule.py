import os
import psycopg2
from psycopg2.extras import RealDictCursor

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def create_db():
    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("""
            CREATE TABLE IF NOT EXISTS linkos_table (
                id SERIAL PRIMARY KEY,
                username TEXT UNIQUE NOT NULL,
                linkos INTEGER DEFAULT 0
            )
        """)
        conn.commit()
    finally:
        cur.close()
        conn.close()

def add_linkos(username, quantity):
    """
    Soma 'quantity' ao saldo atual de Linkos do usuário.
    Se o usuário ainda não tiver um registro, insere-o automaticamente.
    """
    conn = get_db()
    cur = conn.cursor()
    try:
        # Tenta incrementar o saldo existente; se não existir, cria o registro inicial
        cur.execute("""
            INSERT INTO linkos_table (username, linkos)
            VALUES (%s, %s)
            ON CONFLICT (username) 
            DO UPDATE SET linkos = linkos_table.linkos + EXCLUDED.linkos
        """, (username, quantity))
        conn.commit()
        return True
    except Exception as e:
        conn.rollback()
        print(f"[ERRO] Falha ao adicionar linkos para {username}: {e}")
        return False
    finally:
        cur.close()
        conn.close()