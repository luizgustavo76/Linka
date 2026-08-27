import requests
import sqlite3
from federation_crypto import sign_payload

class LinkaFederations:
    def __init__(self, db_path="slug-cache.db"):
        self.db_path = db_path
        self.slugs_pre_loaded = {}
        self.actual_server = "http://127.0.0.1:5000"
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

    def resolve_target(self, slug_or_url):
        return self.slugs_pre_loaded.get(slug_or_url, slug_or_url)

    def receiveConnection(self, slug_or_url, route, headers=None):
        target_url = self.resolve_target(slug_or_url)
        try:
            fed_headers = sign_payload(self.actual_server, {})
            if headers:
                fed_headers.update(headers)

            response = requests.get(target_url + route, headers=fed_headers, timeout=10)
            return {
                "remote_response": response.json() if (response.status_code == 200 and "application/json" in response.headers.get('Content-Type', '') and response.text.strip()) else f"Raw response ({response.status_code}): {response.text}"
            }
        except Exception as e:
            print(f"[Linka] fatal error: {e}")
            return None

    def sendPayload(self, payload, slug_or_url, route, headers=None):
        target_url = self.resolve_target(slug_or_url)

        try:
            fed_headers = sign_payload(self.actual_server, payload)

            if headers:
                fed_headers.update(headers)

            response = requests.post(
                target_url + route,
                json=payload,
                headers=fed_headers,
                timeout=10
            )

            if response.status_code in [200, 201]:
                return response.json() if "application/json" in response.headers.get('Content-Type', '') else response.text
            else:
                print(f"[Linka] Error in instance response: {response.status_code} - {response.text}")
                return None

        except requests.exceptions.RequestException as e:
            print(f"[Linka] Fatal error to connect: {e}")
            return None