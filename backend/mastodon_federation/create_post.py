import requests

def post_to_mastodon(instance_url, access_token, text):
    """
    Envia um post para o Mastodon.
    instance_url: ex: 'https://mastodon.social'
    """
    endpoint = f"{instance_url.rstrip('/')}/api/v1/statuses"
    
    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json"
    }
    
    payload = {
        "status": text,
        "visibility": "public"
    }
    
    response = requests.post(endpoint, json=payload, headers=headers)
    
    if response.status_code in (200, 201):
        return response.json()  # Retorna os dados do post criado
    else:
        raise Exception(f"Erro no Mastodon ({response.status_code}): {response.text}")