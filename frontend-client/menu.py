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
import configparser
app = QApplication(sys.argv)
window = QWidget()
layout = QVBoxLayout(window)
window.setWindowTitle("Menu Linka")
window.resize(400, 300)
config = configparser.ConfigParser()
config.read("config-login.cfg")
lang = config["LANG"]["lang"]

if lang == "pt-br":
    translator = Translator.Translator("strings/main-page/pt-br.json")
else:
    translator = Translator.Translator("strings/main-page/en.json")
welcome_text = translator.translate("initial-page.welcome back")
chat_text = translator.translate("initial-page.chat")
feed_text = translator.translate("initial-page.feed")
add_friends_text = translator.translate("initial-page.add friends")
friends_text = translator.translate("initial-page.friends")
inbox_text = translator.translate("initial-page.inbox")
my_account_text = translator.translate("initial-page.my account")
linka_rights_text = translator.translate("initial-page.Linka rights")
configurations_text = translator.translate("initial-page.configurations")
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
def icon_button(dir, icon, text):
    container = QWidget()
    linha = QHBoxLayout(container)
    linha.setContentsMargins(8, 4, 8, 4)
    linha.setSpacing(12)

    botao = QPushButton("")
    botao.setIcon(QIcon(dir + "/" + icon))
    botao.setIconSize(QSize(48, 48))
    botao.setFixedSize(60, 60)

    botao.setStyleSheet("""
    QPushButton {
        background-color: #1b1b1b;
        border-radius: 10px;
        border: 2px solid #2b2b2b;
    }
    QPushButton:hover {
        border: 2px solid #1e90ff;
    }
    """)

    label = QLabel(text)
    label.setStyleSheet("font-size: 16px; font-weight: bold; color: white;")

    linha.addWidget(botao)
    linha.addWidget(label)
    linha.addStretch()

    container.setFixedHeight(70)

    layout.addWidget(container)

    return botao

def user_entry(texto):
    entrada = QLineEdit()
    entrada.setPlaceholderText(texto)
    layout.addWidget(entrada)
    return entrada
def main():
    label("welcome back", window)
    icon_button("../assets", "chat.png", chat_text)
    icon_button("../assets", "add-friends.png", add_friends_text)
    icon_button("../assets", "configurations.png", configurations_text)
    window.show()
    sys.exit(app.exec())
if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"Erro ao executar a aplicação: {e}")
