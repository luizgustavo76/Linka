import requests
import json
from bs4 import BeautifulSoup
from datetime import datetime

def formate(posts):
    # Se os posts vierem como string JSON, decodifica
    if isinstance(posts, str):
        posts = json.loads(posts)
        
    posts_formatados = []
    
    for post in posts:
        # Pula itens de erro que possam ter sido retornados da API
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
        text_post = soup.get_text().strip()
        
        # Monta o novo dicionário apenas com os campos desejados
        posts_formatados.append({
            "username": username,
            "text_post": text_post,
            "datetime": datetime_formatado
        })
        
    # Retorna a nova lista convertida em uma string JSON limpa e formatada
    return json.dumps(posts_formatados, indent=4, ensure_ascii=False)

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

if __name__ == "__main__":
    resultado = fetch_mastodon_posts()
    
    # Agora sim: 'resultado_formatado' recebe a string JSON filtrada
    resultado_formatado = formate(resultado)
    
    # Exibe o JSON final no terminal
    print(resultado_formatado)