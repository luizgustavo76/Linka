from flask import Flask, request, redirect, jsonify
import sqlite3
import os
#pasta atual
base_dir = os.path.dirname(os.path.abspath(__file__))
#pasta dos bancos de dados
db_dir = (base_dir + "/DB")
#arquivo de banco de dado de post
post_dir = (db_dir + "/post.db")
app = Flask(__name__)
#referencia ao banco de dados local
def get_db():
    conn = sqlite3.connect(post_dir)
    return conn
# cria a tabela de posts
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE NOT EXISTS posts(
                username TEXT 
                text_post TEXT
                star INTEGER
                datetime TEXT
                id INTEGER PRIMARY KEY AUTOINCREMENT)""")
    conn.commit()
    conn.close()
#rota pra criar novo post
@app.route("/new", methods=["POST"])
def new_post():
    dados = request.get_json()
    username = dados.get("username")
    text_post = dados.get("text_post")
    datetime = dados.get("datetime")
    if username is None:
        return jsonify({"status":"username não informado"}), 401
    if text_post is None:
        return jsonify({"status":"não é possivel criar um post sem conteúdo"})
    conn = get_db()
    cur = conn.cursor()
    cur.execute("INSERT INTO posts(username, text_post, datetime) VALUES (?, ?, ?)", (username, text_post, datetime))
    conn.commit()
    conn.close()
    return jsonify({"status":"post criado com sucesso"}),200
@app.route("/view<post_id>")
def view_post(post_id):
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT text_post, username FROM posts WHERE id = ?", (post_id,))
    resultado_post = cur.fetchall()
    conn.close()
    if not resultado_post:
        return jsonify({"status":"post não encontrado"}), 404
    if resultado_post:
        text_post = resultado_post[0]
        username = resultado_post[1]
        corpo_retorno = {
            "text_post":text_post,
            "username":username
        }
        return corpo_retorno