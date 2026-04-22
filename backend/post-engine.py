import requests
import json
while True:
    endpoint = input("url e endpoint:")
    dados = input("json:")
    dados = json.loads(dados)
    r = requests.post(
        endpoint,
        json=dados,
        timeout=5
    )
    print(r.status_code)
    print(r.text)