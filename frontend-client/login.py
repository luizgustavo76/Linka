import sys
import requests
from PyQt6.QtWidgets import (
    QApplication, QWidget, QLabel, QPushButton,
    QVBoxLayout, QHBoxLayout, QFrame,
    QScrollArea, QLineEdit
)
from PyQt6.QtCore import Qt, QTimer, pyqtSignal, QObject
from PyQt6.QtWidgets import QSizePolicy
app = QApplication(sys.argv)
window = QWidget()
layout = QVBoxLayout(window)
def button(texto,  acao, janela):
    botao = QPushButton(texto, janela)
    botao.clicked.connect(acao)
    layout.addWidget(botao)
def pegar_texto(entrada):
    texto = entrada.text()
def entrar():
    entrada_usuario("Nome de usuário")
    entrada_usuario("senha")
    enviar = button("Pronto", lambda: pegar_texto(enviar), window)
def cadastro():
    pass
def entrada_usuario(texto):

    entrada = QLineEdit()
    entrada.setPlaceholderText(texto)
    layout.addWidget(entrada)
def main():

    
    window.setWindowTitle("Faça login no Linka!")
    window.resize(400, 300) 
    window.show()
    button("Entrar", entrar, window)
    button("cadastrar-se", cadastro, window)
    sys.exit(app.exec())

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"Erro ao executar a aplicação: {e}")
