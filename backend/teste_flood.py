import requests

while True:
    token = "76079303e674d7e6ab4c35fb45fdfd1e"
    
    # ADICIONE O CONTENT-TYPE AQUI:
    headers = {
        "Authorization": f"Bearer {token}",
        "Content-Type": "application/json",  # <--- O Flask precisa disso!
        "Accept": "application/json"
    }
    
    json_post = {
        "username": "flooder",
        "text_post": "hduhdihdjdid",
    }
    
    response = requests.post(
        "http://linkaProject.pythonanywhere.com/new",
        json=json_post,
        headers=headers
    )
    
    print(response.status_code)
    print(response.text) # Use .text para ver o JSON de retorno do erro ou sucesso!