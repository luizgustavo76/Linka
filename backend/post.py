from flask import Flask, Blueprint, request, jsonify
import sqlite3
import os
#pasta atual
base_dir = os.path.dirname(os.path.abspath(__file__))
#pasta dos bancos de dados
db_dir = (base_dir + "/DB")
#arquivo de banco de dado de post
post_dir = (db_dir + "/post.db")
post_bp = Blueprint("post", __name__)
#referencia ao banco de dados local
def get_db():
    conn = sqlite3.connect(post_dir)
    return conn
# cria a tabela de posts
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS posts(
                username TEXT,
                text_post TEXT,
                star INTEGER,
                datetime TEXT,
                id INTEGER PRIMARY KEY AUTOINCREMENT)""")
    conn.commit()
    conn.close()
#rota pra criar novo post
@post_bp.route("/new", methods=["POST"])
def new_post():
    dados = request.get_json()
    username = dados.get("username")
    text_post = dados.get("text_post")
    datetime = dados.get("datetime")
    if username is None:
        return jsonify({"status":"username não informado"}), 401
    if text_post is None:
        return jsonify({"status":"não é possivel criar um post sem conteúdo"}),400
    conn = get_db()
    cur = conn.cursor()
    cur.execute("INSERT INTO posts(username, text_post, datetime) VALUES (?, ?, ?)", (username, text_post, datetime))
    conn.commit()
    conn.close()
    return jsonify({"status":"post criado com sucesso"}),200
@post_bp.route("/view<int:post_id>")
def view_post(post_id):
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT text_post, username FROM posts WHERE id = ?", (post_id,))
    resultado_post = cur.fetchall()
    conn.close()
    if not resultado_post:
        return jsonify({"status":"post não encontrado"}), 404
    if resultado_post:
        text_post, username = resultado_post
        text_post = resultado_post[0]
        username = resultado_post[1]
        corpo_retorno = {
            "text_post":text_post,
            "username":username
        }
        return corpo_retorno
@post_bp.route("/feed")
def feed():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT id, username, text_post, datetime FROM posts ORDER BY id DESC")
    posts = cur.fetchall()
    conn.close()

    lista_posts = []

    for post in posts:
        lista_posts.append({
            "id": post[0],
            "username": post[1],
            "text_post": post[2],
            "datetime": post[3]
        })

    return jsonify(lista_posts)

if __name__ == "__main__":
    create_db()
    post_bp.run(debug=True)