from flask import Blueprint, jsonify
import requests
import time

post_bp = Blueprint("post_bp", __name__)

@post_bp.route("/")
@post_bp.route("/feed")
@post_bp.route("/feed/")
@post_bp.route("/feed/<subreddit>")  # Removido o 'path:' para evitar capturar /valide-session
def subreddit_posts(subreddit="comentariosMelhores"):
    clear_sub = subreddit.strip("/") if subreddit else "comentariosMelhores"
    
    # Se o Android enviou algo como "comentariosMelhores/feed", remove o "/feed" do final
    if clear_sub.endswith("/feed"):
        clear_sub = clear_sub[:-5]
    
    # Se a requisição cair em algo genérico, garante o subreddit padrão
    if not clear_sub or clear_sub.lower() in ["feed", "valide-session"]:
        clear_sub = "comentariosMelhores"

    url = f"https://api.pullpush.io/reddit/search/submission/?subreddit={clear_sub}&size=30&sort=desc&sort_type=created_utc"
    
    try:
        start = time.time()
        response = requests.get(url, timeout=5)
        print(f"Tempo API ({clear_sub}): {time.time() - start:.2f}s")
        
        data = response.json()        
        posts = []
        
        for post in data.get("data", []):
            author = post.get("author", "")
            title = post.get("title", "").strip()
            selftext = post.get("selftext", "").strip()
            image_url = post.get("url", "")
            
            if not author or author in ["[deleted]", "AutoModerator"]:
                continue
            if selftext in ["[removed]", "[deleted]"]:
                continue

            is_valid_image = image_url and any(image_url.endswith(ext) for ext in ['.jpg', '.jpeg', '.png', '.gif'])
            
            if is_valid_image:
                text_content = f"{title}\n[IMAGE]{image_url}\n{selftext}".strip()
            else:
                text_content = f"{title}\n{selftext}".strip()
                
            posts.append({
                "id": len(posts) + 1,
                "text_post": text_content,
                "username": author,
            })
            
            if len(posts) >= 15:
                break
            
        return jsonify(posts), 200
        
    except Exception as e:
        print(f"Erro no endpoint: {e}")
        return jsonify({"error": str(e)}), 500