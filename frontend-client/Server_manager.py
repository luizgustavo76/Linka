import sys
import os
import time
import requests
import Translator 
from PyQt6.QtGui import QPalette
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
app = QApplication(sys.argv)
window = QWidget()
layout = QVBoxLayout(window)
layout.setContentsMargins(30, 30, 30, 30)
layout.setSpacing(18)
window.setWindowTitle("Server manager")
window.setMinimumSize(900, 600)
window.setMaximumSize(1200, 800)

palette = app.palette()
text_color = palette.color(QPalette.ColorRole.WindowText).name()
bg_color = palette.color(QPalette.ColorRole.Window).name()
CONFIG_FILE = "config-server.cfg"
def create_config():
        

        config = configparser.ConfigParser()

        if not os.path.exists(CONFIG_FILE):
            config["SERVER"] = {"url": "http://127.0.0.1:5000"}
            config["LANG"] = {"lang": "en"}
            with open(CONFIG_FILE, "w", encoding="utf-8") as f:
                config.write(f)

        config.read(CONFIG_FILE, encoding="utf-8")

        if "SERVER" not in config:
            config["SERVER"] = {"url": "http://127.0.0.1:5000"}
        if "LANG" not in config:
            config["LANG"] = {"lang":"en"}
create_config()
config = configparser.ConfigParser()
config.read("config-login.cfg")
lang = config["LANG"]["lang"]
if lang == "pt-br":
    translator = Translator.Translator("strings/server-manager/pt-br.json")
else:
    translator = Translator.Translator("strings/server-manager/en.json")
def configuration():
    welcome_text = translator.translate("welcome")
    start_text = translator.translate("initial-page.start")
    server_name_text = translator.translate("initial-page.server name")
    next_text = translator.translate("initial-page.next")
    def title(text):
        t = QLabel(text)
        t.setStyleSheet("""
            font-size: 26px;
            font-weight: bold;
            padding: 12px;
        """)
        layout.addWidget(t)
    def scroll_area(parent_layout, widgets_list):
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll_content = QWidget()
        scroll_layout = QVBoxLayout(scroll_content)
        for w in widgets_list:
            scroll_layout.addWidget(w)
        
        scroll.setWidget(scroll_content)
        parent_layout.addWidget(scroll)
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
        QLabel.setStyleSheet(window, f"color: {text_color};")
        if window.layout() is None:
            layout = QVBoxLayout()
            window.setLayout(layout)
        else:
            layout = window.layout()
        
        layout.addWidget(label_text)
    def get_text(entrada):
        texto = entrada.text()
        return texto
    def icon_button(dir, icon, text, action):
        container = QWidget()
        linha = QHBoxLayout(container)
        linha.setContentsMargins(8, 4, 8, 4)
        linha.setSpacing(12)

        botao = QPushButton("")
        botao.setIcon(QIcon(dir + "/" + icon))
        botao.setIconSize(QSize(48, 48))
        botao.setFixedSize(60, 60)

        label = QLabel(text)

        linha.addWidget(botao)
        linha.addWidget(label)
        linha.addStretch()

        container.setFixedHeight(70)
        if action == "None":
            pass
        else:
            botao.clicked.connect(action)
        layout.addWidget(container)

        return botao

    def user_entry(texto):
        entrada = QLineEdit()
        entrada.setPlaceholderText(texto)
        layout.addWidget(entrada)
        return entrada
    def main():
        clean_layout(layout)
        title(welcome_text)
        def start():
            clean_layout(layout)
            with open("strings/server-manager/tutorial-plugin.txt", "r") as f:
                conteudo = f.read()
                label(conteudo, window)
                def set_server():
                    clean_layout(layout)
                    title(server_name_text)
                    server_entry = user_entry("Server")
                    button(next_text, "None", window)
                    def next():
                        config["SERVER"]["url"] = get_text(server_entry)
                        with open(CONFIG_FILE, "w", encoding="utf-8") as f:
                            config.write(f)
                        
                button("ok", set_server, window)
        button(start_text, start, window)
if __name__ == "__main__":
        try:
            main()
            window.show()
            sys.exit(app.exec())
        except Exception as e:
            print(e)