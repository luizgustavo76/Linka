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
    def sendPayload(payload, instance):
        if slugs_pre_loaded[instance]:
            url_instance = slugs_pre_loaded[instance]["url"]
            response = requests.post(
                url_instance,
                json=payload,
                timeout=10
            )
            return response
        else:
            response = request.get(url=https://linkaApi.pythoanywhere.com/meta)
            json_response = json_load(response)
            url_gate = json_response["url"]["linkaGate"]
            response_slug = request.post(
                url_gate + "/consult",
                json={'name_server':instance},
                timeout=10
            )
            if response_slug.status_code == "200":
                url = response_slug["url"]
                response = requests.post(
                    url_instance,
                    json=payload,
                    timeout=10
                )
                return response

