import requests
import json
import unicodedata
from bs4 import BeautifulSoup
from datetime import datetime
def formate(posts):
    # Se os posts vierem como string JSON, decodifica para lista do Python
    if isinstance(posts, str):
        posts = json.loads(posts)
        
    posts_formatados = []
    
    for post in posts:
        if "error" in post:
            continue
            
        username = post["account"]["acct"]        
        
        # Formata a data
        dt = datetime.fromisoformat(post["created_at"].replace("Z", "+00:00"))
        datetime_formatado = dt.strftime("%Y-%m-%d %H:%M:%S")        
        
        # Limpa o HTML do post
        html_content = post["content"]
        html_content = html_content.replace("<br />", "\n").replace("<br>", "\n").replace("</p>", "\n")
        
        soup = BeautifulSoup(html_content, "html.parser")
        text_post = soup.get_text()
        
        # Normaliza o texto removendo caracteres invisíveis (\u00a0)
        text_post = unicodedata.normalize("NFKC", text_post)
        text_post = "\n".join(line.strip() for line in text_post.splitlines()).strip()
        
        # Monta o dicionário
        posts_formatados.append({
            "username": username,
            "text_post": text_post,
            "datetime": datetime_formatado
        })
        
    # RETORNO IMPORTANTE: Retornamos a lista do Python pura! 
    # NADA de usar json.dumps() aqui.
    return posts_formatados

def fetch_mastodon_posts(instance="mastodon.world", total_limit=100):
    api_url = f"https://{instance}/api/v1/timelines/public"
    collected_posts = []
    
    parameters = {'limit': 40, 'local': 'true'} 

    while len(collected_posts) < total_limit:
        response = requests.get(api_url, params=parameters)
        
        if response.status_code != 200:
            return [{"error": f"API request failed. Status code: {response.status_code}"}]
            
        page_posts = response.json()
        
        if not isinstance(page_posts, list):
            return [{"error": "API returned an unexpected format"}]
        
        if not page_posts:
            break
            
        collected_posts.extend(page_posts)
        parameters['max_id'] = page_posts[-1]['id']
        
    return collected_posts[:total_limit]