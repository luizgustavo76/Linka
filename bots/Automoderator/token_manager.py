import requests
def new_session(url, username, password):
    response = requests.post(
        url + "/new-session",
        json={
            "username":username,
            "password":password
        },
        timeout=10
    )
    response_json = response.json()
    if response.status_code == 200:
        return response_json.get("token")
def valide_token(url, token):
    response = requests.post(
        url + "/valide-session",
        json={
            "token":token
        },
        timeout=10
    )
    return response.status_code