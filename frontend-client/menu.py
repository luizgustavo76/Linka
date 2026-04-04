def main_app():
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
    import subprocess
    import os
    import requests


    app = QApplication(sys.argv)
    window = QWidget()
    layout = QVBoxLayout(window)
    window.setWindowTitle("Menu Linka")
    window.resize(400, 300)
    config = configparser.ConfigParser()
    config.read("config-login.cfg")
    username = config["FAST-LOGIN"]["username"]
    url = config["SERVER"]["url"]
    lang = config["LANG"]["lang"]
    if lang == "pt-br":
        translator = Translator.Translator("strings/main-page/pt-br.json")
    else:
        translator = Translator.Translator("strings/main-page/en.json")

    #translated texts
    welcome_text = translator.translate("initial-page.welcome back")
    chat_text = translator.translate("initial-page.chat")
    feed_text = translator.translate("initial-page.feed")
    add_friends_text = translator.translate("initial-page.add friends")
    friends_text = translator.translate("initial-page.friends")
    inbox_text = translator.translate("initial-page.inbox")
    my_account_text = translator.translate("initial-page.my account")
    linka_rights_text = translator.translate("initial-page.Linka rights")
    configurations_text = translator.translate("initial-page.configurations")
    type_here_text = translator.translate("chat.type here")
    username_text = translator.translate("global.username")
    back_text = translator.translate("global.back")
    message_text = translator.translate("add friends.message")
    send_text = translator.translate("global.send")
    accept_text = translator.translate("inbox.accept")
    denied_text = translator.translate("inbox.denied")
    friends_requests_text = translator.translate("inbox.friends requests")
    news_text = translator.translate("inbox.news")
    signout_text = translator.translate("configurations.sign-out")
    exit_label_text = translator.translate("configurations.exit label")
    error_feed_text = translator.translate("feed.error")
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
    def icon_button(dir, icon, text, action):
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
    def icon_buttons_row(buttons_info):
        """
        Cria uma linha de botões lado a lado.
        buttons_info = [
            {"dir": "../assets", "icon": "chat.png", "text": "Chat", "action": "None"},
            {"dir": "../assets", "icon": "add-friends.png", "text": "Add Friends", "action": add_friends}
        ]
        """
        row_container = QWidget()
        row_layout = QHBoxLayout(row_container)
        row_layout.setContentsMargins(0, 0, 0, 0)
        row_layout.setSpacing(10)

        for info in buttons_info:
            botao_container = QWidget()
            botao_layout = QVBoxLayout(botao_container)
            botao_layout.setContentsMargins(0,0,0,0)
            botao_layout.setSpacing(4)

            botao = QPushButton("")
            botao.setIcon(QIcon(info["dir"] + "/" + info["icon"]))
            botao.setIconSize(QSize(48,48))
            botao.setFixedSize(60,60)
            if info["action"] != "None":
                botao.clicked.connect(info["action"])

            label_widget = QLabel(info["text"])
            label_widget.setAlignment(Qt.AlignmentFlag.AlignCenter)
            label_widget.setStyleSheet("color: white; font-weight: bold;")

            botao_layout.addWidget(botao, alignment=Qt.AlignmentFlag.AlignCenter)
            botao_layout.addWidget(label_widget)
            row_layout.addWidget(botao_container)

        layout.addWidget(row_container)
    def add_friends():
        clean_layout(layout)
        username_entry = user_entry(username_text)
        
        message_entry = user_entry(message_text)
        def send():
            get_user = get_text(username_entry)
            get_message = get_text(message_entry)
            data_inbox = requests.post(
                url + "/send-friend",
                json={
                    "receiver":get_user,
                    "remittee":username
                },
                timeout=5
            )
            if data_inbox.status_code == 200 or data_inbox.status_code == 201:
                clean_layout(layout)
                main()

        button(send_text, send, window)
        button(back_text, main, window)
    def scroll_area():
        scroll = QScrollArea()          
        scroll.setWidgetResizable(True) 
        layout.addWidget(scroll)
    def inbox():
        clean_layout(layout)
        label(friends_requests_text, window)
        data_inbox = requests.post(
            url + "/inbox",
            json={
                "username":username
            },
            timeout=5
        )
        if data_inbox.status_code in (200, 201):
            response_inbox = data_inbox.json()
            print(response_inbox)
        back_button = button(back_text, main, window)
    def configurations():
        clean_layout(layout)
        def sign_out():
            os.remove("config-login.cfg")
            clean_layout(layout)
            label(exit_label_text, window)
        button(signout_text, sign_out, window)
        button(back_text, main, window)
    def feed():
        clean_layout(layout)
        scroll_area()
        button(back_text, main, window)
        try:
            data_feed = requests.get(url + "/feed")
            if data_feed.status_code in (200, 201):
                response_feed = data_feed.json()
                print(response_feed)
        except:
            label(error_feed_text, window)
    def main():
        clean_layout(layout)
        label(welcome_text, window)
        icon_buttons_row([
            {"dir": "../assets", "icon": "chat.png", "text": chat_text, "action": add_friends},
            {"dir": "../assets", "icon": "add-friends.png", "text": add_friends_text, "action": add_friends}
        ])
        icon_buttons_row([
            {"dir": "../assets", "icon": "configurations.png", "text": configurations_text, "action": configurations},
            {"dir": "../assets", "icon": "inbox.png", "text": inbox_text, "action": inbox}
        ])
        icon_buttons_row([{"dir": "../assets", "icon": "posts.png", "text": feed_text, "action": feed}])
        
    if __name__ != "__main__":
        main()
        window.show()
        sys.exit(app.exec())
       
