import sys
import os
import requests
import configparser
import secrets
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


# ==========================
# CONFIG SYSTEM
# ==========================

def create_config(username="", token="", alternative_server=""):
    if not os.path.isfile("config-login.cfg"):
        config = configparser.ConfigParser()

        config["SERVER"] = {
            "url": alternative_server if alternative_server else "http://127.0.0.1:5000"
        }

        config["LANG"] = {
            "lang": "en"
        }

        config["FAST-LOGIN"] = {
            "username": username,
            "token": token
        }

        with open("config-login.cfg", "w", encoding="utf-8") as cfg:
            config.write(cfg)


create_config()

config = configparser.ConfigParser()
config.read("config-login.cfg")

lang = config["LANG"]["lang"]
url = config["SERVER"]["url"]
saved_token = config["FAST-LOGIN"]["token"]


# ==========================
# TRANSLATOR
# ==========================

if lang == "pt-br":
    translator = Translator.Translator("strings/login/pt-br.json")
else:
    translator = Translator.Translator("strings/login/en.json")

username_text = translator.translate("login.username")
password_text = translator.translate("login.password")
sign_in_text = translator.translate("initial-page.sign-in")
sign_up_text = translator.translate("initial-page.sign-up")
change_server_text = translator.translate("initial-page.change server")
configuration_text = translator.translate("initial-page.configurations")
back_text = translator.translate("global.back")


# ==========================
# QT APP
# ==========================

app = QApplication(sys.argv)

app.setStyleSheet("""
QWidget {
    background: qlineargradient(x1:0, y1:0, x2:0, y2:1,
        stop:0 #d8f5ff,
        stop:0.45 #7ac9ff,
        stop:1 #2562ff
    );
    font-family: "Segoe UI";
    font-size: 14px;
}

QLabel {
    color: black;
}

QLineEdit {
    background: rgba(255, 255, 255, 200);
    border: 2px solid rgba(255, 255, 255, 220);
    border-radius: 14px;
    padding: 10px;
    color: #003366;
    font-size: 14px;
}

QLineEdit:focus {
    border: 2px solid rgba(100, 200, 255, 240);
    background: rgba(255, 255, 255, 240);
}

QPushButton {
    background: qlineargradient(x1:0, y1:0, x2:0, y2:1,
        stop:0 #ffffff,
        stop:0.4 #bfefff,
        stop:1 #5cc9ff
    );
    border: 2px solid rgba(255,255,255,210);
    border-radius: 18px;
    padding: 12px;
    color: #003366;
    font-weight: bold;
}

QPushButton:hover {
    background: qlineargradient(x1:0, y1:0, x2:0, y2:1,
        stop:0 #ffffff,
        stop:0.4 #d9f7ff,
        stop:1 #3ab8ff
    );
}

QPushButton:pressed {
    background: qlineargradient(x1:0, y1:0, x2:0, y2:1,
        stop:0 #3ab8ff,
        stop:1 #ffffff
    );
}

QPushButton#primary {
    background: qlineargradient(x1:0, y1:0, x2:0, y2:1,
        stop:0 #ffffff,
        stop:0.3 #8be0ff,
        stop:1 #0099ff
    );
    color: #002244;
}

QPushButton#secondary {
    background: rgba(255,255,255,120);
    color: #003366;
}
""")


# ==========================
# MAIN WINDOW + GLASS CARD
# ==========================

window = QWidget()
window.setWindowTitle("Linka - Login")
window.resize(520, 420)

main_layout = QVBoxLayout(window)
main_layout.setContentsMargins(40, 40, 40, 40)

card = QFrame()
card.setStyleSheet("""
QFrame {
    background: rgba(255,255,255,140);
    border: 2px solid rgba(255,255,255,220);
    border-radius: 25px;
}
""")

shadow = QGraphicsDropShadowEffect()
shadow.setBlurRadius(35)
shadow.setXOffset(0)
shadow.setYOffset(12)
card.setGraphicsEffect(shadow)

layout = QVBoxLayout(card)
layout.setContentsMargins(30, 30, 30, 30)
layout.setSpacing(18)

main_layout.addWidget(card)


# ==========================
# UTILS
# ==========================

def animate_card():
    anim = QPropertyAnimation(card, b"geometry")
    anim.setDuration(250)
    anim.setStartValue(QRect(card.x(), card.y() + 25, card.width(), card.height()))
    anim.setEndValue(QRect(card.x(), card.y(), card.width(), card.height()))
    anim.start()
    card.anim = anim


def clean_layout(layout):
    while layout.count():
        item = layout.takeAt(0)

        widget = item.widget()
        if widget is not None:
            widget.deleteLater()

        sub_layout = item.layout()
        if sub_layout is not None:
            clean_layout(sub_layout)


def label(text):
    lbl = QLabel(text)
    lbl.setAlignment(Qt.AlignmentFlag.AlignCenter)
    layout.addWidget(lbl)
    return lbl


def button(text, action=None, primary=False):
    btn = QPushButton(text)

    if primary:
        btn.setObjectName("primary")
    else:
        btn.setObjectName("secondary")

    if action:
        btn.clicked.connect(action)

    layout.addWidget(btn)
    return btn


def user_entry(placeholder, password=False):
    entrada = QLineEdit()
    entrada.setPlaceholderText(placeholder)

    if password:
        entrada.setEchoMode(QLineEdit.EchoMode.Password)

    layout.addWidget(entrada)
    return entrada


def show_message(text, ms=4000):
    msg = QLabel(text)
    msg.setAlignment(Qt.AlignmentFlag.AlignCenter)
    msg.setStyleSheet("""
        color: black;
        font-weight: bold;
        font-size: 13px;
        background: rgba(0,0,0,120);
        padding: 8px;
        border-radius: 12px;
    """)
    layout.addWidget(msg)
    QTimer.singleShot(ms, msg.deleteLater)


# ==========================
# PAGES
# ==========================

def main_page():
    clean_layout(layout)
    animate_card()

    title = QLabel("Linka")
    title.setAlignment(Qt.AlignmentFlag.AlignCenter)
    title.setStyleSheet("""
        font-size: 46px;
        font-weight: bold;
        color: black;
    """)

    subtitle = QLabel("Vista Aero vibes. Conecte-se com o mundo.")
    subtitle.setAlignment(Qt.AlignmentFlag.AlignCenter)
    subtitle.setStyleSheet("""
        font-size: 14px;
        color: rgba(255,255,255,230);
    """)

    layout.addWidget(title)
    layout.addWidget(subtitle)

    button(sign_in_text, login_page, primary=True)
    button(sign_up_text, signup_page, primary=False)
    button(configuration_text, None, primary=False)
    button(change_server_text, None, primary=False)


def login_page():
    clean_layout(layout)
    animate_card()

    label("Login")

    entrada_nome = user_entry(username_text)
    entrada_senha = user_entry(password_text, password=True)

    def send():
        nome = entrada_nome.text()
        senha = entrada_senha.text()

        if not nome or not senha:
            show_message("Preencha todos os campos!")
            return

        try:
            data_login = requests.post(
                url + "/login",
                json={"username": nome, "senha": senha},
                timeout=5
            )

            if data_login.status_code == 200:
                show_message("Login feito com sucesso!")

                # tenta fast login
                try:
                    fast = requests.post(
                        url + "/fast-login",
                        json={"username": nome, "token": saved_token},
                        timeout=5
                    )

                    if fast.status_code == 200:
                        response = fast.json()
                        token = response.get("token")

                        config["FAST-LOGIN"]["username"] = nome
                        config["FAST-LOGIN"]["token"] = token

                        with open("config-login.cfg", "w", encoding="utf-8") as cfg:
                            config.write(cfg)

                except:
                    pass

            else:
                show_message(f"Erro: {data_login.status_code}")

        except:
            show_message("Erro no servidor ou conexão falhou.")

    button("Entrar", send, primary=True)
    button(back_text, main_page, primary=False)


def signup_page():
    clean_layout(layout)
    animate_card()

    label("Criar conta")

    entry_user = user_entry(username_text)
    entry_email = user_entry("Email")
    entry_password = user_entry(password_text, password=True)
    entry_repeat_password = user_entry("Repeat password", password=True)

    def finish_signup():
        user = entry_user.text()
        email = entry_email.text()
        password = entry_password.text()
        confirm_password = entry_repeat_password.text()

        if not user or not email or not password:
            show_message("Preencha todos os campos!")
            return

        if password != confirm_password:
            show_message("Senhas não combinam!")
            return

        try:
            data = requests.post(
                url + "/register",
                json={
                    "username": user,
                    "senha": password,
                    "email": email
                },
                timeout=5
            )

            if data.status_code == 201:
                show_message("Conta criada com sucesso!")
                QTimer.singleShot(1500, main_page)
            else:
                show_message(f"Erro ao criar conta: {data.status_code}")

        except:
            show_message("Erro ao conectar com servidor.")

    button("Criar Conta", finish_signup, primary=True)
    button(back_text, main_page, primary=False)


# ==========================
# RUN
# ==========================

if __name__ == "__main__":
    main_page()
    window.show()
    sys.exit(app.exec())