import sys
import os
import time
import requests
import Translator 
import QTools
from PyQt6.QtWidgets import (
    QApplication, QWidget, QLabel, QPushButton,
    QVBoxLayout, QHBoxLayout, QFrame,
    QScrollArea, QLineEdit
)
from PyQt6.QtCore import Qt, QTimer, pyqtSignal, QObject
from PyQt6.QtWidgets import QSizePolicy
import configparser

url = "http://127.0.0.1:5000"
def create_config(username, token, AlternativeServer):
    if not os.path.isfile("config-login.cfg"):
        if AlternativeServer:
            config["SERVER"] = {
            "json":AlternativeServer
        }
        config = configparser.ConfigParser()
        config["LANG"] = {
            "lang":"en"
        }
        config["FAST-LOGIN"] = {
            "username":username,
            "token": token
        }
        with open("config-login.cfg", "w", encoding="utf-8") as cfg:
            config.write(cfg)
config = configparser.ConfigParser()
config.read("config-login.cfg")
lang = config["LANG"]["lang"]

if lang == "pt-br":
    translator = Translator.Translator("strings/login/pt-br.json")
else:
    translator = Translator.Translator("strings/login/en.json")

username_text = translator.translate("login.username")
password_text = translator.translate("login.password")
sign_in_text = translator.translate("initial-page.sign-in")
sign_up_text = translator.translate("initial-page.sign-up")
email_text = translator.translate("sign_in.email")
change_server_text = translator.translate("initial-page.change server")
login_with_github_text = translator.translate("initial-page.login with github")         
app = QApplication(sys.argv)
window = QWidget()
layout = QVBoxLayout(window)
def clean_layout(layout):
    while layout.count():
        item = layout.takeAt(0)

        widget = item.widget()
        if widget is not None:
            widget.deleteLater()

        sub_layout = item.layout()
        if sub_layout is not None:
            clean_layout(sub_layout)
        
def button(texto,  acao, janela):
    if acao == "None":
        botao = QPushButton(texto, janela)
        layout.addWidget(botao)
        return botao
    else:
        botao = QPushButton(texto, janela)
        botao.clicked.connect(acao)
        layout.addWidget(botao)
        return botao
def label(text, window):
    label_text = QLabel(text)

    if window.layout() is None:
        layout = QVBoxLayout()
        window.setLayout(layout)
    else:
        layout = window.layout()
    
    layout.addWidget(label_text)
def get_text(entrada):
    texto = entrada.text()
    return texto
def sigin():
    clean_layout(layout)

    entrada_nome = user_entry("Nome de usuário")
    entrada_senha = user_entry("Senha")

    def send():
        nome = get_text(entrada_nome)
        senha = get_text(entrada_senha)
        try:
            dados_login = requests.post(
                url + "/login",
                json={"username":nome, "senha":senha},
                timeout=5
            )
            if dados_login.status_code == 200:
                label("login feito com sucesso", window)
            else:
                label(f"erro ao efetuar login {dados_login.status_code}")
        except:
            clean_layout(layout)
            label("Um erro aconteceu no servidor, verfique a conexão",window)
            

    button("Pronto", send, window)
def signup():
    clean_layout(layout)
    top_text = translator.translate("sign-up.top text")
    label(top_text, window)
    entry_user = user_entry(username_text)
    repeat_password_text = translator.translate("sign-up.repeat the password")
    entry_password = user_entry(password_text)
    entry_repeat_password = user_entry(repeat_password_text)
    entry_email = user_entry("Email")
    finish_text = translator.translate("sign-up.finish")
    def finish_signup():
        email = get_text(entry_email)
        user = get_text(entry_user)
        password = get_text(entry_password)
        confirm_password = get_text(entry_repeat_password)
        if password == confirm_password:
            try:
                data = requests.post(
                    url + "/register",
                    json={
                        "username":user,
                        "senha":password,
                        "email":email
                    },
                    timeout=5
                )
                if data.status_code == 200:
                    welcome_text = translator.translate("sign-up.welcome")
                    label(welcome_text + " " + user, window)
                else:
                    clean_layout(layout)
                    error_text = translator.translate(f"errors.{data.status_code}")
                    label(error_text, window)
                    QTimer.singleShot(7000, main)
                    

            except:
                pass
        else:
            top_text = translator.translate("sign-up.top text password fail")
    confirm = button(finish_text, finish_signup, window)

def user_entry(texto):
    entrada = QLineEdit()
    entrada.setPlaceholderText(texto)
    layout.addWidget(entrada)
    return entrada
def main():
    clean_layout(layout)
    label("Linka", window)
    window.setWindowTitle("Faça login no Linka!")
    window.resize(400, 300) 
    window.show()
    button(sign_in_text, sigin, window)
    button(sign_up_text, signup, window)
    button(login_with_github_text, "None", window)
    button(change_server_text, "None", window)
    sys.exit(app.exec())

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"Erro ao executar a aplicação: {e}")
