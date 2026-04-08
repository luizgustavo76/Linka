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
    from PyQt6.QtGui import QPalette
    import Translator
    import configparser
    import subprocess
    import os
    import requests
    from datetime import datetime, date
    
    app = QApplication(sys.argv)
    window = QWidget()
    layout = QVBoxLayout(window)
    window.setWindowTitle("Menu Linka")
    window.setMinimumSize(900, 600)
    window.setMaximumSize(1200, 800)
    config = configparser.ConfigParser()
    config.read("config-login.cfg")
    username = config["FAST-LOGIN"]["username"]
    url = config["SERVER"]["url"]
    url_federations = config["FEDERATIONS"]["url"]
    lang = config["LANG"]["lang"]
    if lang == "pt-br":
        translator = Translator.Translator("strings/main-page/pt-br.json")
    else:
        translator = Translator.Translator("strings/main-page/en.json")
    
    app.setStyleSheet("""
        QWidget {
            background-color: palette(window);
            color: palette(window-text);
        }

        QPushButton {
            background-color: palette(button);
            color: palette(button-text);
            border-radius: 8px;
            padding: 6px;
        }

        QPushButton:hover {
            border: 1px solid #1e90ff;
        }

        QLineEdit {
            background-color: palette(base);
            color: palette(text);
            border: 1px solid gray;
            padding: 5px;
        }
        """)
    palette = app.palette()
    text_color = palette.color(QPalette.ColorRole.WindowText).name()
    bg_color = palette.color(QPalette.ColorRole.Window).name()
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
    edit_text = translator.translate("my account.edit")
    new_post_text = translator.translate("feed.new post")
    text_post = translator.translate("feed.text post")
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
            label_widget.setStyleSheet("font-weight: bold;")

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
    def scroll_area(parent_layout, widgets_list):
        scroll = QScrollArea()
        scroll.setWidgetResizable(True)
        scroll_content = QWidget()
        scroll_layout = QVBoxLayout(scroll_content)
        for w in widgets_list:
            scroll_layout.addWidget(w)
        
        scroll.setWidget(scroll_content)
        parent_layout.addWidget(scroll)
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
        def add_federations():
            clean_layout(layout)
            label(translator.translate("configurations.add-federations"), window)
            entry_url = user_entry("url")
            def send():
                url_text = get_text(entry_url)
                config["FEDERATIONS"]["url"] = url_text
                with open("config-login.cfg", "w", encoding="utf-8") as f:
                    config.write(f)
                configurations()
            button(send_text,send, window)
        button(translator.translate("configurations.add-federations"), add_federations, window)
        button(signout_text, sign_out, window)
        button(back_text, main, window)
    from functools import partial
    def new_post():
        clean_layout(layout)
        label(new_post_text, window)
        entry_post = user_entry(text_post)
        def send_post():
            now = datetime.now()
            formated_hour = now.strftime("%d/%m/%Y %H:%M:%S")
            post = get_text(entry_post)
            try:
                data_post = requests.post(
                    url + "/new",
                    json={
                        "username":username,
                        "text_post":post,
                        "datetime":formated_hour
                    },
                    timeout=5
                )
                if data_post.status_code in (200, 201):
                    main()
                else:
                   pass
            except Exception as e:
                print(e) 
        button(back_text, main, window)
        button(send_text, send_post, window)
    def feed():
        clean_layout(layout)
        button(back_text, main, window)
        button(new_post_text, new_post, window)
        data_feed = requests.get(url + "/feed")
        if data_feed.status_code in (200, 201):
            posts = data_feed.json()
            posts_dict = {post['id']: post for post in posts}

            labels = []

            for post in posts_dict.values():

                frame = QFrame()
                frame_layout = QVBoxLayout(frame)
                star_layout = QHBoxLayout()

                frame_layout.addWidget(QLabel(f"{post['username']}"))
                frame_layout.addWidget(QLabel(f"{post['text_post']}"))
                frame_layout.addWidget(QLabel(f"{post['datetime']}"))

                icon_button = QPushButton()
                icon_button.setIcon(QIcon("../assets/default_star.png"))
                icon_button.setIconSize(QSize(24, 24))
                icon_button.setFixedSize(30, 30)
                icon_button.setStyleSheet("border: none;")
                icon_button.clicked.connect(partial(toggle_star, icon_button))
                star_label = QLabel("0")
                star_label.setStyleSheet("color: white; font-size: 14px;")

                star_layout.addWidget(icon_button)
                star_layout.addWidget(star_label)
                star_layout.addStretch()

                frame_layout.addLayout(star_layout)

                labels.append(frame)

            scroll_area(layout, labels)


    def toggle_star(button, post_id):
        if button.property("clicked") == True:
            button.setProperty("clicked", False)
            button.setIcon(QIcon("../assets/default_star.png"))
        else:
            button.setProperty("clicked", True)
            try:
                data_star = requests.post(
                    url + "/star",
                    json={
                        "post_id":post_id,
                        "add-or-remove":"add"
                    },
                    timeout=5
                )
            except:
                pass
            button.setIcon(QIcon("../assets/star.png"))
    def my_account():
        clean_layout(layout)
        button(back_text, main, window)
        label(username, window)

        try:
            data_account = requests.get(url + f"/view_profile/{username}", timeout=5)

            if data_account.status_code in (200, 201):
                response = data_account.json()
                print(response)

                if response == []:
                    data_new_profile = requests.post(
                        url + "/create_profile",
                        json={"username": username},
                        timeout=5
                    )

                    if data_new_profile.status_code in (200, 201):
                        label("Perfil criado com sucesso!", window)
                    else:
                        label("CRITICAL ERROR!", window)

                else:
                    label("Perfil carregado!", window)

            else:
                label("Erro ao carregar perfil!", window)

        except Exception as e:
            print(e)
            label("Erro de conexão!", window)

        button(edit_text, "None", window)
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
        icon_buttons_row([{"dir": "../assets", "icon": "posts.png", "text": feed_text, "action": feed}, {"dir": "../assets", "icon": "my-account.png", "text": my_account_text, "action": my_account}])
        
    if __name__ != "__main__":
        try:
            main()
            window.show()
            sys.exit(app.exec())
        except Exception as e:
            print(e)
