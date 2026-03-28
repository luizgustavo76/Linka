from flask import Flask, request, jsonify, redirect, send_from_directory, abort, render_template
import sqlite3
import os
from werkzeug.security import generate_password_hash, check_password_hash
import secrets
app = Flask(__name__)
class Login:
    def __init__(self):
        #pasta atual
        self.base_dir = os.path.dirname(os.path.abspath(__file__))
        #pasta onde está os banco de dados
        self.db_dir = (self.base_dir + "/DB")
        #banco de dados de login
        self.login_dir = os.path.join(self.db_dir, "login.db")
        self.fast_login = os.path.join(self.db_dir, "FastLogin.db")
        #senha em hash
        self.senha_hash = None
        
        if not os.path.exists(self.db_dir):
            os.makedirs(self.db_dir)
        #criar banco se não existir
        def criar_db_login():
            conn = self.get_db_login()
            cur = conn.cursor()
            cur.execute("""
                CREATE TABLE IF NOT EXISTS login(
                        username TEXT UNIQUE,
                        senha TEXT,
                        email TEXT UNIQUE
                    )
            """)
            conn.commit()
            conn.close()
            conn = self.get_db_fastlogin()
            cur = conn.cursor()
            cur.execute("""
                CREATE TABLE IF NOT EXISTS FastLogin(
                    username TEXT UNIQUE,
                    token TEXT
                )
                """)
            conn.commit()
            conn.close()
        criar_db_login()
    def get_db_login(self):
        conn = sqlite3.connect(self.login_dir)
        return conn
    def get_db_fastlogin(self):
        conn = sqlite3.connect(self.fast_login)
        return conn    
    #transformar senha em texto para hash
    def gerar_hash(self, senha):
        self.senha_hash = generate_password_hash(senha)
        return self.senha_hash
    #verificar se a hash com a senha
    def verificar_hash(self, senha, hash):
        return check_password_hash(hash, senha)
login_system = Login()
#rota pra registrar usuarios
@app.route("/register", methods=["POST"])
def register():
    dados = request.get_json()
    username = dados.get("username")
    senha = dados.get("senha")
    email = dados.get("email")
    conn = login_system.get_db_login()
    cur = conn.cursor()
    cur.execute("SELECT username FROM login WHERE username = ?", (username,))
    resultado = cur.fetchone()
    conn.close()
    if resultado:
        return jsonify({"status":"username já existe"}),400
    else:
        senha_com_hash = login_system.gerar_hash(senha)
        conn = login_system.get_db_login()
        cur = conn.cursor()
        cur.execute("""
            INSERT INTO login (username, senha, email) VALUES (?, ?, ?)""",(username, senha_com_hash, email))
        conn.commit()
        conn.close()
        return jsonify({"status":"conta criada com sucesso!"}), 201

#route of main page
@app.route("/")
def main():
    return render_template("index.html") 
#route of the fast login
@app.route("/fast-login", methods=["POST"])
def FastLogin():
    data = request.get_json()
    username = data.get("username")
    token = data.get("token")
    def generate_token(length=32):
        """Generate a secure random token"""
        return secrets.token_hex(length)
#login route
@app.route("/login", methods=["POST"])
def login():
    dados = request.get_json()
    username = dados.get("username")
    senha = dados.get("senha")
    conn = login_system.get_db_login()
    cur = conn.cursor()
    cur.execute("SELECT username FROM login WHERE username = ?", (username,))
    #resultado de usernames, se existir aquele username ele retorna o nome se não retorna None
    resultado_username = cur.fetchone()
    conn.close()
    if not resultado_username:
        return jsonify({"status":"usuário não existe, verifique o campo de usuário"}), 401
    if resultado_username:
        conn = login_system.get_db_login()
        cur = conn.cursor()
        cur.execute("SELECT senha FROM login WHERE username =?", (username,))
        resultado_senha = cur.fetchone()
        hash_salvo = resultado_senha[0]
        senha_descodificada = login_system.verificar_hash(senha, hash_salvo)
        conn.close()
        if senha_descodificada:
            
            return jsonify({"status":"login efetuado com sucesso"}), 200
        else:
            return jsonify({"status": "senha errada, verifique se a senha está correta"}), 400
app.run(debug=True)