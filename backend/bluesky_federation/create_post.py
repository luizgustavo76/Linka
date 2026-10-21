import requests
from datetime import datetime, timezone

def post_to_bluesky(handle, app_password, text):
    pds_url = "https://bsky.social"
    
    session_res = requests.post(
        f"{pds_url}/xrpc/com.atproto.server.createSession",
        json={"identifier": handle, "password": app_password}
    )
    
    if session_res.status_code != 200:
        raise Exception(f"Erro ao autenticar no Bluesky: {session_res.text}")
        
    session_data = session_res.json()
    access_jwt = session_data["accessJwt"]
    user_did = session_data["did"]
    
    now_iso = datetime.now(timezone.utc).isoformat().replace("+00:00", "Z")
    headers = {
        "Authorization": f"Bearer {access_jwt}",
        "Content-Type": "application/json"
    }
    
    record_payload = {
        "repo": user_did,
        "collection": "app.bsky.feed.post",
        "record": {
            "$type": "app.bsky.feed.post",
            "text": text,
            "createdAt": now_iso
        }
    }
    
    post_res = requests.post(
        f"{pds_url}/xrpc/com.atproto.repo.createRecord",
        json=record_payload,
        headers=headers
    )
    
    if post_res.status_code in (200, 201):
        return post_res.json()
    else:
        raise Exception(f"Erro ao postar no Bluesky: {post_res.text}")