import json
import os
import secrets
from datetime import datetime, timedelta
import psycopg2
from psycopg2.extras import RealDictCursor
from werkzeug.security import check_password_hash
from flask import Flask, Blueprint, request, jsonify, g, Response
from flask_cors import CORS
import dotenv
import markdown
base_dir = os.path.dirname(os.path.abspath(__file__))

# Carrega backend.env localmente se existir
env_path = os.path.join(base_dir, "backend.env")
if os.path.exists(env_path):
    dotenv.load_dotenv(env_path)
else:
    dotenv.load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL")

json_path = os.path.join(base_dir, "backend.json")
with open(json_path, "r") as f:
    modules_flags = json.load(f)
root_flags = modules_flags["modules-flags"]

from mentions import mentions_bp
if root_flags["twitter"]:
    from twitter_federation.posts import twitter_bp
if root_flags["threads"]:
    from threads_federation.app import threads_bp
if root_flags["communities"]:
    from community import community_bp
if root_flags["reddit-federation"]:
    from reddit_federation.posts import post_bp as reddit_post_bp
if root_flags["mastodon-federation"]:
    from mastodon_federation.posts import post_bp as mastodon_post_bp
if root_flags["bluesky-federation"]:
    from bluesky_federation.posts import post_bp as bluesky_post_bp
if root_flags["post"]:
    from post import post_bp
if root_flags["chat"]:
    from chat import chat_bp
if root_flags["bookmarks"]:
    from bookmarks import bookmarks_bp

tos_content = ""
if root_flags.get("automoderator"):
    import sys
    import threading
    import time
    from pathlib import Path
    BASE_DIR = Path(__file__).resolve().parent.parent
    AUTOMOD_DIR = BASE_DIR / "bots" / "Automoderator"
    SDK_DIR = BASE_DIR / "SDKs" / "linkaBotsSdk"
    for path_to_add in [AUTOMOD_DIR, SDK_DIR, BASE_DIR]:
        if path_to_add.exists() and str(path_to_add) not in sys.path:
            sys.path.insert(0, str(path_to_add))
    TOS_PATH = AUTOMOD_DIR / "terms_of_service.txt"
    if TOS_PATH.exists():
        with open(TOS_PATH, "r", encoding="utf-8") as f:
            tos_content = f.read()
    import AutoModerator
    def start_automod_loop():
        time.sleep(5)
        print("[AutoMod] Daemon iniciado. Rodando varredura a cada 2 minutos...")
        try:
            AutoModerator.main()
        except Exception as e:
            print(f"[AutoMod Error] Falha na execução da thread: {e}")
    threading.Thread(target=start_automod_loop, daemon=True).start()

if root_flags["friends"]:
    from friends import friends_bp
from login import login_bp
if root_flags["profiles"]:
    from profiles import profile_bp
if root_flags["search"]:
    from search import search_bp
if root_flags["status"]:
    from status import status_bp
from meta import meta_bp
from invites import invites_bp
if root_flags["chat_global"]:
    from chat_global import chat_global_bp
if root_flags["federations"]:
    from federations import federations_bp
if root_flags["notifications"]:
    from notifications import notifications_blueprint
if root_flags["images"]:
    from images import image_bp
if root_flags["federation_index"]:
    from federation_index import federation_index_bp
if root_flags["sincronizer"]:
    from sincronizer import sincronizer_bp
if root_flags["themes"]:
    from themes import theme_bp
from chat_group import chat_group_bp

app = Flask(__name__)

# Conexão remota centralizada no PostgreSQL (Neon.tech)
def get_db():
    conn = psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)
    return conn
def init_tables():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS tokens (
            username TEXT,
            token TEXT,
            emission_date TEXT,
            expire_date TEXT
        );
    """)
    # CRIA ÍNDICE DE PERFORMANCE NA COLUNA TOKEN
    cur.execute("""
        CREATE INDEX IF NOT EXISTS idx_tokens_token ON tokens(token);
    """)
    cur.execute("""
        CREATE TABLE IF NOT EXISTS banned (
            username TEXT,
            time TEXT,
            reason TEXT
        );
    """)
    conn.commit()
    cur.close()
    conn.close()
# Garante que as tabelas essenciais de tokens/bans existam no Postgres
init_tables()

def verificar_hash(senha, hash_salvo):
    return check_password_hash(hash_salvo, senha)

def gerar_token():
    return secrets.token_hex(16)
@app.route("/privacy")
def privacy():
    privacy_path = os.path.join(base_dir, "privacy.md")
    try:
        with open(privacy_path, "r", encoding="utf-8") as f:
            md_text = f.read()
    except FileNotFoundError:
        return Response("Privacy policy not found.", status=404, mimetype="text/plain")

    html_body = markdown.markdown(md_text)

    html_page = f"""<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Linka Privacy Policy</title>
    <style>
        body {{
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
            max-width: 720px;
            margin: 40px auto;
            padding: 0 20px;
            line-height: 1.6;
            color: #222;
            background: #fafafa;
        }}
        h1, h2 {{ color: #111; border-bottom: 1px solid #ddd; padding-bottom: 6px; }}
        code {{ background: #eee; padding: 2px 6px; border-radius: 4px; }}
    </style>
</head>
<body>
    {html_body}
</body>
</html>"""

    return Response(html_page, mimetype="text/html; charset=utf-8")
@app.route("/tos")
def tos_route():
    html = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <meta charset="utf-8">
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <title>Linka - Terms of Service</title>
        <style>
            body {{
                background-color: #121212;
                color: #e0e0e0;
                font-family: monospace;
                padding: 20px;
            }}
            pre {{
                white-space: pre-wrap;
                word-wrap: break-word;
            }}
        </style>
    </head>
    <body>
        <pre>{tos_content}</pre>
    </body>
    </html>
    """
    return html

@app.route("/new-session", methods=["POST"])
def new_session():
    data = request.get_json()
    username = data.get("username")
    password = data.get("password")
    if None in (username, password):
        return jsonify({"status": "the json is empty or is missing data"}), 401
    
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT password FROM login WHERE username = %s", (username,))
    result = cur.fetchone()
    
    if not result:
        conn.close()
        return jsonify({"status": "user not found"}), 401
    
    if verificar_hash(password, result["password"]):
        emission_date = datetime.now().isoformat()
        expire_date = (datetime.now() + timedelta(hours=2)).isoformat()
        token = gerar_token()
        
        cur.execute("INSERT INTO tokens(username, token, emission_date, expire_date) VALUES(%s, %s, %s, %s)", 
                    (username, token, emission_date, expire_date))
        conn.commit()
        conn.close()
        return jsonify({"status": "the session has created", "token": token}), 200
    
    conn.close()
    return jsonify({"status": "wrong password"}), 401

public_routes = [
    "post.feed",
    "meta.return_version",
    "new_session",
    "search.search",
    "post.view_comments",
    "login.register",
    "login.login",
    "post.return_stars",
    "None",
    "profile.create",
    "images.upload_image",
    "images.lite_render",
    "profile.get_profile_pic",
    "post.view_post"
]

@app.before_request
def valide():
    # Rotas ignoradas totalmente pelo middleware
    if request.path in ["/receiveToken", "/sendToken", "/upload-image", "/view-post", "/lite-render", "/tos"]:
        return None

    token = request.headers.get("Authorization")

    if request.method == "GET":
        return None
    if request.endpoint in public_routes:
        if token:
            token = token.replace("Bearer ", "").strip()
            conn = get_db()
            cur = conn.cursor()
            cur.execute("SELECT username FROM tokens WHERE token = %s", (token,))
            result = cur.fetchone()
            conn.close()
            g.username = result["username"] if result else None
        else:
            g.username = None
        # Rota pública SEMPRE passa, tendo token válido, inválido ou sem token!
        return None

    # SE A ROTA É PRIVADA:
    if not token:
        return jsonify({"status": "the token is empty"}), 403
        
    token = token.replace("Bearer ", "").strip()
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT username, expire_date, token FROM tokens WHERE token = %s", (token,))
    result = cur.fetchone()
    
    if not result:
        conn.close()
        return jsonify({"status": "invalid token"}), 403
        
    g.username = result["username"]
    
    # Validações de Ban e Expiração...
    cur.execute("SELECT * FROM banned WHERE username = %s", (g.username,))
    result_user_banned = cur.fetchone()
    conn.close()
    
    if result_user_banned:
        return jsonify({
            "status": "BANNED",
            "reason": result_user_banned["reason"],
            "time": result_user_banned["time"]
        }), 403
        
    if result["expire_date"]:
        expire_date = datetime.fromisoformat(result["expire_date"])
        if datetime.now() > expire_date:
            return jsonify({"status": "the token has been expired"}), 403

    return None
@app.route("/valide-session", methods=["POST"])
def valideManual():
    public_paths = ["/login", "/register", "/new-session", "/create-profile", "/tos"]
    if request.path in public_paths:
        return None  
    token = request.headers.get("Authorization")
    if token is None:
        return jsonify({"status": "the token is empty"}), 403
    else:
        token = token.replace("Bearer ", "").strip()
        
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT username, expire_date, token FROM tokens WHERE token = %s", (token,))
    result = cur.fetchone()
    conn.close()
    
    if not result:
        return jsonify({"status": "invalid token"}), 403
        
    token_db = result["token"]
    g.username = result["username"]
    expire_date = datetime.fromisoformat(result["expire_date"])
    
    if token_db == token:
        if datetime.now() > expire_date:
            return jsonify({"status": "the token has been expired"}), 403
        else:
            return jsonify({"status": "the token is valid"}), 200
    else:
        return jsonify({"status": "the token is invalid"}), 403

CORS(app, resources={r"/*": {"origins": "*"}})

if root_flags["status"]:
    app.register_blueprint(status_bp)
if root_flags["search"]:
    app.register_blueprint(search_bp)
if root_flags["profiles"]:
    app.register_blueprint(profile_bp)
app.register_blueprint(login_bp)
if root_flags["post"]:
    app.register_blueprint(post_bp)
if root_flags["chat"]:
    app.register_blueprint(chat_bp)
if root_flags["sincronizer"]:
    app.register_blueprint(sincronizer_bp)
if root_flags["friends"]:
    app.register_blueprint(friends_bp)
app.register_blueprint(meta_bp)
app.register_blueprint(invites_bp)
if root_flags["reddit-federation"]:
    app.register_blueprint(reddit_post_bp)
if root_flags["mastodon-federation"]:
    app.register_blueprint(mastodon_post_bp)
if root_flags["bluesky-federation"]:
    app.register_blueprint(bluesky_post_bp)
if root_flags["chat_global"]:
    app.register_blueprint(chat_global_bp)
if root_flags["notifications"]:
    app.register_blueprint(notifications_blueprint)
if root_flags["chat_group"]:
    app.register_blueprint(chat_group_bp)
if root_flags["images"]:
    app.register_blueprint(image_bp)
if root_flags["themes"]:
    app.register_blueprint(theme_bp)
if root_flags["federations"]:
    app.register_blueprint(federations_bp)
if root_flags["threads"]:
    app.register_blueprint(threads_bp)
if root_flags["federation_index"]:
    app.register_blueprint(federation_index_bp)
if root_flags["communities"]:
    app.register_blueprint(community_bp)
if root_flags["bookmarks"]:
    app.register_blueprint(bookmarks_bp)
if root_flags["twitter"]:
    app.register_blueprint(twitter_bp)
app.register_blueprint(mentions_bp)

if __name__ == "__main__":
    app.run(host='0.0.0.0', port=5000)