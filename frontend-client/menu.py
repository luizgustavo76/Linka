import sys
from PyQt6.QtWidgets import (
    QApplication, QWidget, QLabel, QPushButton,
    QVBoxLayout, QHBoxLayout, QFrame,
    QScrollArea, QLineEdit
)
from PyQt6.QtGui import QIcon
from PyQt6.QtCore import QSize
from PyQt6.QtCore import Qt, QTimer, pyqtSignal, QObject
from PyQt6.QtWidgets import QSizePolicy
import Translator
app = QApplication(sys.argv)
window = QWidget()
layout = QVBoxLayout(window)
window.setWindowTitle("Menu")
window.resize(400, 300)
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
def icon_button(dir, icon):
    botao = QPushButton("")
    botao.setIcon(QIcon(dir + "/" + icon))
    botao.setIconSize(QSize(80, 80))
    botao.setFixedSize(90, 90)
    layout.addWidget(botao)
    return botao
def user_entry(texto):
    entrada = QLineEdit()
    entrada.setPlaceholderText(texto)
    layout.addWidget(entrada)
    return entrada
def main():
    label("welcome back", window)
    icon_button("../assets", "chat.png")
    icon_button("../assets", "add-friends.png")
    window.show()
    sys.exit(app.exec())
if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"Erro ao executar a aplicação: {e}")
