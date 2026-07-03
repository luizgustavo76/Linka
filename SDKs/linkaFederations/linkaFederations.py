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

    def sendPayload(self, payload, slug_ou_url):
        # 1. Verifica se o que foi passado é um slug conhecido no cache
        if slug_ou_url in self.slugs_pre_loaded:
            target_url = self.slugs_pre_loaded[slug_ou_url]
        else:
            # Se não estiver no cache, assume que já é a URL direta da instância
            target_url = slug_ou_url

        try:
            # Faz a requisição P2P para a outra instância do Linka
            response = requests.post(
                target_url,
                json=payload,
                timeout=10
            )
            
            # No requests, status_code retorna um INT, e não string
            if response.status_code in [200, 201]:
                return response
            else:
                print(f"[Linka] Erro na resposta da instância: {response.status_code}")
                return None
                
        except requests.exceptions.RequestException as e:
            print(f"[Linka] Falha catastrófica ao conectar na instância: {e}")
            return None