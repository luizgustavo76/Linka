import sys
import os
import time
import requests
import Translator 
from PyQt6.QtWidgets import (
    QApplication, QWidget, QLabel, QPushButton,
    QVBoxLayout, QHBoxLayout, QFrame,
    QScrollArea, QLineEdit
)
from PyQt6.QtCore import Qt, QTimer, pyqtSignal, QObject
from PyQt6.QtWidgets import QSizePolicy
from PyQt6.QtGui import QFont
from PyQt6.QtWidgets import QGraphicsDropShadowEffect
from PyQt6.QtCore import QPropertyAnimation, QRect
import configparser
import webbrowser

def create_config(username, token, AlternativeServer):
    CONFIG_FILE = "config-login.cfg"

    config = configparser.ConfigParser()

    if not os.path.exists(CONFIG_FILE):
        config["SERVER"] = {"url": "http://127.0.0.1:5000"}
        config["LANG"] = {"lang": "en"}
        config["FAST-LOGIN"] = {"username": "", "token": ""}

        with open(CONFIG_FILE, "w", encoding="utf-8") as f:
            config.write(f)

    config.read(CONFIG_FILE, encoding="utf-8")

    if "SERVER" not in config:
        config["SERVER"] = {"url": "http://127.0.0.1:5000"}

    if "LANG" not in config:
        config["LANG"] = {"lang": "en"}

    if "FAST-LOGIN" not in config:
        config["FAST-LOGIN"] = {"username": "", "token": ""}

create_config(None, None, None)

config = configparser.ConfigParser()
config.read("config-login.cfg")
lang = config["LANG"]["lang"]

if lang == "pt-br":
    translator = Translator.Translator("strings/login/pt-br.json")
else:
    translator = Translator.Translator("strings/login/en.json")
url = config["SERVER"]["url"]
token = config["FAST-LOGIN"]["token"]
username_text = translator.translate("login.username")
password_text = translator.translate("login.password")
sign_in_text = translator.translate("initial-page.sign-in")
sign_up_text = translator.translate("initial-page.sign-up")
email_text = translator.translate("sign_in.email")
change_server_text = translator.translate("initial-page.change server")
login_with_github_text = translator.translate("initial-page.login with github")
configuration_text = translator.translate("initial-page.configurations")
back_text = translator.translate("global.back")      
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
    global token
    clean_layout(layout)

    entrada_nome = user_entry(username_text)
    entrada_senha = user_entry(password_text)
    button(back_text, main, window)
    def send():
        global token
        nome = get_text(entrada_nome)
        senha = get_text(entrada_senha)
        try:
            dados_login = requests.post(
                url + "/login",
                json={"username":nome, "senha":senha},
                timeout=5
            )
            fast_login = requests.post(
                url + "/fast-login",
                json = {
                    "username":nome,
                    "token":token
                },
                timeout=5
            )
            if fast_login.status_code == 200:
                resp_fast = fast_login.json()
                new_token = resp_fast.get("token")
                if new_token:
                    token = new_token
                    config["FAST-LOGIN"]["username"] = nome
                    config["FAST-LOGIN"]["token"] = new_token
                    with open("config-login.cfg", "w", encoding="utf-8") as f:
                        config.write(f)

            if dados_login.status_code == 200:
                label("login feito com sucesso", window)
            else:
                label(f"erro ao efetuar login {dados_login.status_code}")
        except:
            clean_layout(layout)
            label("Um erro aconteceu no servidor, verfique a conexão",window)
            

    button("Pronto", send, window)
def send_fast_login():
    nome =config["FAST-LOGIN"]["username"]
    token_config = config["FAST-LOGIN"]["token"]
    if token_config == None:
        main()
    else:
        fast_login = requests.post(
            url + "/fast-login",
            json = {
                "username":nome,
                "token":token
            },
            timeout=5
        )
        if fast_login.status_code == 200:
            resp_fast = fast_login.json()
            new_token = resp_fast.get("token")
            if new_token:
                token = new_token
                config["FAST-LOGIN"]["username"] = nome
                config["FAST-LOGIN"]["token"] = new_token
                with open("config-login.cfg", "w", encoding="utf-8") as f:
                    config.write(f)
def signup():
    global token
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
        global token
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
                if data.status_code == 200 or data.status_code == 201:
                    
                    welcome_text = translator.translate("sign-up.welcome")
                    label(welcome_text + " " + user, window)
                    data_token = requests.post(
                        url + "/fast-login",
                        json={
                            "username":user,
                            "token":None
                        },
                        timeout=5
                    )
                    new_token = data_token.json().get("token")
                    config["FAST-LOGIN"]["username"] = user
                    config["FAST-LOGIN"]["token"] = new_token
                    with open("config-login.cfg", "w", encoding="utf-8") as f:
                        config.write(f)
                    token = new_token

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
    back_button = button(back_text, main, window)

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
    button(configuration_text, "None", window)
    button(change_server_text, "None", window)
    

if __name__ == "__main__":
    try:
        main()
        sys.exit(app.exec())
    except Exception as e:
        print(f"Erro ao executar a aplicação: {e}")