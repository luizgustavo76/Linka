from flask import Flask, Blueprint, redirect, request, jsonify
import requests

github_bp = Blueprint("github-auth", __name__)

CLIENT_ID = "Iv23li2XFR6qvOuBgDKm"
CLIENT_SECRET = "1552e2c7d23c9ed4ac2b279d06cd1b78fb219ce8"

@github_bp.route("/auth/github/login")
def github_login():
    github_url = (
        "https://github.com/login/oauth/authorize"
        f"?client_id={CLIENT_ID}"
        "&scope=read:user user:email"
    )
    return redirect(github_url)


@github_bp.route("/github/callback")
def github_callback():
    code = request.args.get("code")

    token_url = "https://github.com/login/oauth/access_token"
    token_data = {
        "client_id": CLIENT_ID,
        "client_secret": CLIENT_SECRET,
        "code": code
    }

    headers = {"Accept": "github_bplication/json"}

    token_response = requests.post(token_url, data=token_data, headers=headers)
    token_json = token_response.json()

    access_token = token_json.get("access_token")

    if not access_token:
        return jsonify({"error": "Não foi possível obter access_token"}), 400

    user_url = "https://api.github.com/user"
    user_headers = {"Authorization": f"Bearer {access_token}"}

    user_data = requests.get(user_url, headers=user_headers).json()

    return jsonify({
        "username": user_data.get("login"),
        "id": user_data.get("id"),
        "avatar_url": user_data.get("avatar_url"),
        "profile_url": user_data.get("html_url")
    })


if __name__ == "__main__":
    github_bp.run(debug=True, port=5000)