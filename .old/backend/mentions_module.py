import os
import psycopg2
from psycopg2.extras import RealDictCursor

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def create_db():
    if not DATABASE_URL:
        return

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute("""
            CREATE TABLE IF NOT EXISTS mentions (
                id SERIAL PRIMARY KEY,
                username TEXT NOT NULL,
                author TEXT NOT NULL,
                content TEXT,
                post_id INTEGER,
                comment_id INTEGER,
                type TEXT,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
        """)
        conn.commit()
    except Exception as e:
        conn.rollback()
        print(f"[ERRO create_db mentions]: {e}")
    finally:
        cur.close()
        conn.close()

def createMention(username, author, content, post_id, comment_id, mention_type):
    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute(
            """
            INSERT INTO mentions (username, author, content, post_id, comment_id, type)
            VALUES (%s, %s, %s, %s, %s, %s)
            """,
            (username, author, content, post_id, comment_id, mention_type)
        )
        conn.commit()
        return True
    except Exception as e:
        conn.rollback()
        print(f"[ERRO createMention]: {e}")
        return False
    finally:
        cur.close()
        conn.close()