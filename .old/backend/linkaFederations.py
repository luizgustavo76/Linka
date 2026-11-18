import os
import requests
import psycopg2
from psycopg2.extras import RealDictCursor
from federation_crypto import sign_payload

DATABASE_URL = os.getenv("DATABASE_URL")
ACTUAL_SERVER_URL = os.getenv("ACTUAL_SERVER_URL", "http://127.0.0.1:5000")

class LinkaFederations:
    def __init__(self):
        self.slugs_pre_loaded = {}
        self.actual_server = ACTUAL_SERVER_URL.rstrip("/")
        self.create_db()
        self.load_slugs()

    def get_db(self):
        return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

    def create_db(self):
        """ Cria a tabela de cache de slugs no PostgreSQL se não existir """
        if not DATABASE_URL:
            print("[LinkaFederations] AVISO: DATABASE_URL não definida.")
            return

        conn = self.get_db()
        cur = conn.cursor()
        try:
            cur.execute("""
                CREATE TABLE IF NOT EXISTS slug_cache (
                    id SERIAL PRIMARY KEY,
                    slug TEXT UNIQUE NOT NULL,
                    url TEXT NOT NULL
                )
            """)
            conn.commit()
        except Exception as e:
            conn.rollback()
            print(f"[LinkaFederations] Erro ao criar tabela slug_cache: {e}")
        finally:
            cur.close()
            conn.close()

    def load_slugs(self):
        """ Carrega os mapeamentos de slugs/instâncias em memória """
        if not DATABASE_URL:
            return

        conn = self.get_db()
        cur = conn.cursor()
        try:
            cur.execute("SELECT slug, url FROM slug_cache")
            result = cur.fetchall()
            for row in result:
                self.slugs_pre_loaded[row["slug"]] = row["url"]
        except Exception as e:
            print(f"[LinkaFederations] Erro ao carregar slugs do cache: {e}")
        finally:
            cur.close()
            conn.close()

    def resolve_target(self, slug_or_url):
        return self.slugs_pre_loaded.get(slug_or_url, slug_or_url)

    def receiveConnection(self, slug_or_url, route, headers=None):
        target_url = self.resolve_target(slug_or_url).rstrip("/")
        endpoint = route if route.startswith("/") else f"/{route}"
        full_target = f"{target_url}{endpoint}"

        try:
            fed_headers = sign_payload(self.actual_server, {})
            if headers:
                fed_headers.update(headers)

            response = requests.get(full_target, headers=fed_headers, timeout=10)
            content_type = response.headers.get('Content-Type', '')

            if response.status_code == 200 and "application/json" in content_type and response.text.strip():
                return {"remote_response": response.json()}
            else:
                return {"remote_response": f"Raw response ({response.status_code}): {response.text}"}

        except Exception as e:
            print(f"[LinkaFederations] Erro fatal em receiveConnection: {e}")
            return None

    def sendPayload(self, payload, slug_or_url, route, headers=None):
        target_url = self.resolve_target(slug_or_url).rstrip("/")
        endpoint = route if route.startswith("/") else f"/{route}"
        full_target = f"{target_url}{endpoint}"

        try:
            fed_headers = sign_payload(self.actual_server, payload)
            if headers:
                fed_headers.update(headers)

            response = requests.post(
                full_target,
                json=payload,
                headers=fed_headers,
                timeout=10
            )

            content_type = response.headers.get('Content-Type', '')

            if response.status_code in [200, 201]:
                if "application/json" in content_type and response.text.strip():
                    return response.json()
                return response.text
            else:
                print(f"[LinkaFederations] Erro na resposta da instância ({response.status_code}): {response.text}")
                return None

        except requests.exceptions.RequestException as e:
            print(f"[LinkaFederations] Erro fatal ao conectar com a instância: {e}")
            return None