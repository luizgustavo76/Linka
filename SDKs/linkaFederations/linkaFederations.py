import requests
import sqlite3
slugs_pre_loaded = []
def get_db():
    conn = sqlite3.connect("slug-cache.db")
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
CREATE TABLE IF NOT EXISTS slug-cache(
                slug TEXT,
                url TEXT)""")
    conn.commit()
    conn.close()
create_db()
def load_slugs():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT * FROM slug-cache")
    result = cur.fetchone()
    for i in len(result):
        slugs_pre_loaded.append(result[i])
load_slugs()
class LinkaFederations:
