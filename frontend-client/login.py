import sys
import os
import requests
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
        config["FAST-LOGIN"] = {
            "username":username,
            "token": token
        }
        with open("config-login.cfg", "w", encoding="utf-8") as cfg:
            config.write(cfg)
        
app = QApplication(sys.argv)
window = QWidget()
layout = QVBoxLayout(window)
def limpar_layout(layout):
    while layout.count():
        item = layout.takeAt(0)

        widget = item.widget()
        if widget is not None:
            widget.deleteLater()

        sub_layout = item.layout()
        if sub_layout is not None:
            limpar_layout(sub_layout)
        
def button(texto,  acao, janela):
    if acao == "None":
        botao = QPushButton(texto, janela)
        layout.addWidget(botao)
        return button
    else:
        botao = QPushButton(texto, janela)
        botao.clicked.connect(acao)
        layout.addWidget(botao)
        return button
def label(text, window):
    label_text = QLabel(text, window)
    window.addWidget(label_text)
def pegar_texto(entrada):
    texto = entrada.text()
    return texto
def entrar():
    limpar_layout(layout)

    entrada_nome = entrada_usuario("Nome de usuário")
    entrada_senha = entrada_usuario("Senha")

    def enviar():
        nome = pegar_texto(entrada_nome)
        senha = pegar_texto(entrada_senha)
        try:
            dados_login = requests.post(
                url + "/login",
                json={"username":nome, "senha":senha},
                timeout=5
            )
        except:
            limpar_layout(layout)
            

    button("Pronto", enviar, window)
def cadastro():
    pass
def entrada_usuario(texto):
    entrada = QLineEdit()
    entrada.setPlaceholderText(texto)
    layout.addWidget(entrada)
    return entrada
def main():
    window.setWindowTitle("Faça login no Linka!")
    window.resize(400, 300) 
    window.show()
    button("Entrar", entrar, window)
    button("cadastrar-se", cadastro, window)
    button("Login com Github", "None", window)
    button("Alterar servidor", "None", window)
    sys.exit(app.exec())

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"Erro ao executar a aplicação: {e}")
