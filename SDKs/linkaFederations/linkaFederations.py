import requests
import sqlite3
slugs_pre_loaded = {}
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
    global slugs_pre_loaded 
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT slug, url FROM slug_cache")
    result = cur.fetchall() 
    conn.close()
    for slug, url in result:
        slugs_pre_loaded[slug] = url
load_slugs()
class LinkaFederations:
    def sendPayload(payload, url):
        if slugs_pre_loaded[url]:
            url_instance = slugs_pre_loaded[url]["url"]
            response = requests.post(
                url,
                json=payload,
                timeout=10
            )
            if response.status_code == "200" or response.status_code == "201":
                return response
            
            
