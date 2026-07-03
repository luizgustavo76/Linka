import requests
import sqlite3

class LinkaFederations:
    def __init__(self, db_path="slug-cache.db"):
        self.db_path = db_path
        self.slugs_pre_loaded = {}
        self.create_db()
        self.load_slugs()

    def get_db(self):
        return sqlite3.connect(self.db_path)

    def create_db(self):
        conn = self.get_db()
        cur = conn.cursor()
        cur.execute("""
            CREATE TABLE IF NOT EXISTS slug_cache (
                slug TEXT UNIQUE,
                url TEXT
            )
        """)
        conn.commit()
        conn.close()

    def load_slugs(self):
        conn = self.get_db()
        cur = conn.cursor()
        cur.execute("SELECT slug, url FROM slug_cache")
        result = cur.fetchall() 
        conn.close()
        
        for slug, url in result:
            self.slugs_pre_loaded[slug] = url
    def receiveConnection(self, slug_or_url):
        if slug_or_url in self.slugs_pre_loaded:
            target_url = self.slugs_pre_loaded[slug_or_url]
        else:
            target_url = slug_or_url
        try:
            response = requests.get(target_url, timeout=10)
            print(response.json())
        except Exception as e:
            print(f"fatal error {e}")
    def sendPayload(self, payload, slug_or_url, headers):
        if slug_or_url in self.slugs_pre_loaded:
            target_url = self.slugs_pre_loaded[slug_or_url]
        else:
            target_url = slug_or_url

        try:
            if headers:
                response = requests.post(
                    target_url,
                    json=payload,
                    headers=headers,
                    timeout=10
                )
            else:    
                response = requests.post(
                    target_url,
                    json=payload,
                    timeout=10
                )
            if response.status_code in [200, 201]:
                return response
            else:
                print(f"[Linka] Error in instance response: {response.status_code}")
                return None
                
        except requests.exceptions.RequestException as e:
            print(f"[Linka] Fatal error to connect: {e}")
            return None