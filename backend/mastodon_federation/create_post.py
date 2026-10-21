import requests
from flask import Blueprint, request, jsonify

post_bp = Blueprint('mastodon_posts', __name__)

def post_to_mastodon(instance_url, access_token, text):
    if not instance_url.startswith("http://") and not instance_url.startswith("https://"):
        instance_url = "https://" + instance_url

    endpoint = f"{instance_url.rstrip('/')}/api/v1/statuses"
    
    headers = {
        "Authorization": f"Bearer {access_token}",
        "Content-Type": "application/json"
    }
    
    payload = {
        "status": text,
        "visibility": "public"
    }
    
    response = requests.post(endpoint, json=payload, headers=headers, timeout=10)
    
    if response.status_code in (200, 201):
        return response.json()
    else:
        raise Exception(f"Erro no Mastodon ({response.status_code}): {response.text}")

@post_bp.route("/feed/mastodon/new", methods=["POST"])
def new_mastodon_post():
    data = request.get_json(silent=True) or {}
    
    access_token = data.get("access_token") or data.get("acess_token")
    text_post = data.get("text_post")
    instance = data.get("url") or "https://mastodon.social"

    print(f"DEBUG MASTODON - Token: {access_token} | Text: {text_post}")

    if access_token and text_post:
        try:
            res = post_to_mastodon(instance, access_token, text_post)
            return jsonify({"status": "post was created", "mastodon_id": res.get("id")}), 200
        except Exception as e:
            print(f"DEBUG MASTODON ERROR: {e}")
            return jsonify({"status": "error", "message": str(e)}), 500
    else:
        return jsonify({"status": "access_token or text_post empty"}), 400