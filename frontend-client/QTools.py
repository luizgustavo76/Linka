import sys
from PyQt6.QtWidgets import (
    QApplication, QWidget, QLabel, QPushButton,
    QVBoxLayout, QLineEdit
)

class Tela:
    def __init__(self):
        self.window = QWidget()
        self.window.setWindowTitle("Minha Tela")

        self.layout = QVBoxLayout()
        self.window.setLayout(self.layout)

    def user_entry(self, texto):
        entrada = QLineEdit()
        entrada.setPlaceholderText(texto)
        self.layout.addWidget(entrada)
        return entrada

    def get_text(self, entrada):
        texto = entrada.text()
        return texto

    def label(self, text):
        label_text = QLabel(text)
        self.layout.addWidget(label_text)
        return label_text

    def button(self, texto, acao=None):
        botao = QPushButton(texto)

        if acao is not None:
            botao.clicked.connect(acao)

        self.layout.addWidget(botao)
        return botao

    def show(self):
        self.window.show()


"""how to use
define tela = Tela()
and use the functions in the class
"""
app = QApplication(sys.argv)

tela = Tela()

entrada_nome = tela.user_entry("Digite seu nome")

def clicar():
    nome = tela.get_text(entrada_nome)
    tela.label(f"Olá, {nome}!")

tela.button("Confirmar", clicar)

tela.show()

sys.exit(app.exec())