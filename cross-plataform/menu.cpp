#include <QApplication>
#include <QWidget>
#include <QPushButton>
#include <QVBoxLayout>
#include <QLineEdit>
#include <QDebug>
#include <functional>
#include <QNetworkAccessManager>
#include <QNetworkRequest>
#include <QNetworkReply>
#include <QStyleFactory>
#include <QVector>
#include <QSplashScreen>
#include <QStandardPaths>
#include <QJsonDocument>
#include <iostream>
#include <QJsonObject>
#include <QLabel>
#include <QUrl>
#include <fstream>
#include <QEventLoop>
#include <string>
#include <QScrollBar>
#include <QStackedWidget>
#include <map>
#include <QFile>
#include <QDir>
#include <QFileInfo>
#include <QTabWidget>
#include <QTabBar>
#include <QMainWindow>
#include <QJsonArray>
#include <QScrollArea>
#include <QTimer>
#include <QFrame>
#include <QIcon>
#include <QSize>
#include <nlohmann/json.hpp>
#include <QPainter>
#include <QFontMetrics>

class ChatBubble : public QWidget {
public:
    ChatBubble(QString text, bool isMe, QWidget *parent = nullptr)
        : QWidget(parent), message(text), mine(isMe)
    {
        setMaximumWidth(400);
        setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Fixed);
    }

protected:
    void paintEvent(QPaintEvent *event) override {
        QPainter painter(this);
        painter.setRenderHint(QPainter::Antialiasing);

        QRect bubbleRect = rect().adjusted(10, 5, -10, -5);

        QColor bubbleColor;
        if (mine)
            bubbleColor = QColor(0, 200, 120); // verde tipo "mensagem enviada"
        else
            bubbleColor = QColor(60, 60, 60);  // cinza tipo "mensagem recebida"

        painter.setBrush(bubbleColor);
        painter.setPen(Qt::NoPen);

        painter.drawRoundedRect(bubbleRect, 18, 18);

        painter.setPen(Qt::white);
        painter.setFont(QFont("Arial", 11));

        painter.drawText(
            bubbleRect.adjusted(15, 10, -15, -10),
            Qt::TextWordWrap,
            message
        );
    }

    QSize sizeHint() const override {
        QFontMetrics fm(QFont("Arial", 11));
        QRect r = fm.boundingRect(0, 0, 300, 1000, Qt::TextWordWrap, message);
        return QSize(r.width() + 60, r.height() + 30);
    }

private:
    QString message;
    bool mine;
};
using json = nlohmann::json;
std::string trim(const std::string &s)
{
    size_t start = s.find_first_not_of(" \t\r\n");
    size_t end = s.find_last_not_of(" \t\r\n");

    if(start == std::string::npos)
        return "";

    return s.substr(start, end - start + 1);
}

QString requestHTTP(const QString &url,
                    const QString &method,
                    const QJsonObject &json,
                    int timeoutMs = 10000)
{
    QNetworkAccessManager manager;

    QNetworkRequest request;
    request.setUrl(QUrl(url));
    request.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");

    QNetworkReply *reply = nullptr;

    QByteArray jsonData = QJsonDocument(json).toJson();

    QString m = method.toUpper();

    if (m == "GET")
    {
        reply = manager.get(request);
    }
    else if (m == "POST")
    {
        reply = manager.post(request, jsonData);
    }
    else if (m == "PUT")
    {
        reply = manager.put(request, jsonData);
    }
    else if (m == "DELETE")
    {
        reply = manager.sendCustomRequest(request, "DELETE", jsonData);
    }
    else
    {
        return "ERRO: Método HTTP inválido (" + method + ")";
    }

    QEventLoop loop;

    QTimer timer;
    timer.setSingleShot(true);

    QObject::connect(&timer, &QTimer::timeout, &loop, &QEventLoop::quit);
    QObject::connect(reply, &QNetworkReply::finished, &loop, &QEventLoop::quit);

    timer.start(timeoutMs);
    loop.exec();

    if (!timer.isActive())
    {
        reply->abort();
        reply->deleteLater();
        return "ERRO: Timeout na requisição.";
    }

    if (reply->error() != QNetworkReply::NoError)
    {
        QString errorMsg = "ERRO: " + reply->errorString();
        reply->deleteLater();
        return errorMsg;
    }

    QString response = reply->readAll();
    reply->deleteLater();

    return response;
}
void loadStyle()
{
    QFile file(":/styles/theme.qss");

    if (file.open(QFile::ReadOnly)) {
        qDebug() << "QSS loaded";
        qApp->setStyleSheet(file.readAll());
    } else {
        qDebug() << "QSS FAILED";
    }
}

std::map<std::string, std::map<std::string, std::string>> config;
void scroll_area(QVBoxLayout *layout, const QList<QWidget*> &widgets)
{
    QScrollArea *scroll = new QScrollArea();
    scroll->setWidgetResizable(true);

    QWidget *container = new QWidget();
    QVBoxLayout *containerLayout = new QVBoxLayout(container);

    for(QWidget *w : widgets)
    {
        containerLayout->addWidget(w);
    }

    containerLayout->addStretch();
    scroll->setWidget(container);

    layout->addWidget(scroll);
};
QString configPath() {
    QString dirPath = QStandardPaths::writableLocation(QStandardPaths::AppDataLocation);
    QDir().mkpath(dirPath);
    return dirPath + "/config-login.cfg";
};
void loadConfig() {

    QString path = configPath();
    qDebug() << "Caminho config:" << path;

    if (!QFile::exists(path))
    {
        QFile res(":/config-login.cfg");
        if (res.open(QIODevice::ReadOnly))
        {
            QFile out(path);
            if (out.open(QIODevice::WriteOnly))
            {
                out.write(res.readAll());
                out.close();
            }
            res.close();
        }
    }

    QFile file(path);

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text)) {
        qDebug() << "Erro ao abrir config-login.cfg";
        return;
    }

    QTextStream in(&file);

    std::string section;
    QString line;

    while (!in.atEnd()) {
        line = in.readLine().trimmed();

        if (line.isEmpty()) continue;

        if (line.startsWith("[")) {
            section = line.toStdString();
            section = section.substr(1, section.find(']') - 1);
        }
        else {
            int pos = line.indexOf('=');
            if (pos != -1) {
                QString key = line.left(pos).trimmed();
                QString value = line.mid(pos + 1).trimmed();

                config[section][key.toStdString()] = value.toStdString();
            }
        }
    }
};
void saveConfig() {

    QString dirPath = QStandardPaths::writableLocation(QStandardPaths::AppDataLocation);
    QDir().mkpath(dirPath);

    QString filePath = dirPath + "/config-login.cfg";

    qDebug() << "Salvando config em:" << filePath;

    std::ofstream file(filePath.toStdString());

    file << "[SERVER]\n";
    file << "url = " << config["SERVER"]["url"] << "\n\n";

    file << "[LANG]\n";
    file << "lang = " << config["LANG"]["lang"] << "\n\n";

    file << "[FAST-LOGIN]\n";
    file << "username = " << config["FAST-LOGIN"]["username"] << "\n";
    file << "token = " << config["FAST-LOGIN"]["token"] << "\n";
    file << "password = " << config["FAST-LOGIN"]["password"] << "\n\n";

    file << "[FEDERATIONS]\n";
    file << "url = " << config["FEDERATIONS"]["url"] << "\n\n";

    file << "[THEMES]\n";
    file << "theme = " << config["THEMES"]["theme"] << "\n";

    file.close();
}
void clearLayout(QLayout *layout) {
    if (!layout) return;

    QLayoutItem *item;
    while ((item = layout->takeAt(0)) != nullptr) {
        if (item->layout()) {
            clearLayout(item->layout());
            delete item->layout();
        }
        if (item->widget()) {
            item->widget()->deleteLater();
        }
        delete item;
    }
}
int main(int argc, char *argv[])
{
    QApplication app(argc, argv);
    app.setStyle(QStyleFactory::create("breeze"));
    loadConfig();
    loadStyle();
    //url do servidor
    for (auto &sec : config) {
        std::cout << "[" << sec.first << "]\n";
        for (auto &kv : sec.second) {
            std::cout << "  " << kv.first << " = " << kv.second << "\n";
        }
    }
    QString url = QString::fromStdString(config["SERVER"]["url"]);
    qDebug() << "url" << url;   
    //janela principal
    QMainWindow window;
    app.setWindowIcon(QIcon(":/assets/icon.png"));
    QPixmap pixmap(":/assets/icon.png");

    QSplashScreen splash(pixmap);
    splash.show();
    window.setWindowTitle("Linka Mobile");
    window.resize(400, 600);

    // CENTRAL WIDGET
    QWidget *central = new QWidget();
    QVBoxLayout *layout = new QVBoxLayout(central);
    //username no config-login.cfg
    QString username = QString::fromStdString(config["FAST-LOGIN"]["username"]);
    //strings traduzidas
    QString text_post = QCoreApplication::translate("feed", "text post");
    QString back_text = QCoreApplication::translate("global", "back");
    QString new_post_text = QCoreApplication::translate("feed", "new post");
    QString friends_text = QCoreApplication::translate("add friends", "friends");
    QString search_text = QCoreApplication::translate("main-page", "search");
    QString add_friends_text = QCoreApplication::translate("main-page", "add friends");
    QString username_text = QCoreApplication::translate("global", "username");
    QString message_text = QCoreApplication::translate("add friends", "message");
    QString send_text = QCoreApplication::translate("global", "send");
    QString inbox_text = QCoreApplication::translate("main-page", "inbox");
    QString accept_text = QCoreApplication::translate("inbox", "accept");
    QString denied_text = QCoreApplication::translate("inbox", "denied");
    QString type_text = QCoreApplication::translate("chat", "type here");
    QString add_theme_text = QCoreApplication::translate("configurations", "add theme");
    QString add_federations_text = QCoreApplication::translate("configurations", "add federations");
    QString options_text = QCoreApplication::translate("main-page", "configurations");
    QString signin_text = QCoreApplication::translate("main-page", "sign-in");
    QString signup_text = QCoreApplication::translate("main-page", "sign-up");
    QString password_text = QCoreApplication::translate("sign-up", "password");
    QString retry_password_text = QCoreApplication::translate("sign-up", "retry the password");
    QString email_text = QCoreApplication::translate("sign-up", "email");
    layout->setContentsMargins(12, 12, 12, 12);
    layout->setSpacing(10);

    window.setCentralWidget(central);

    QNetworkAccessManager *manager = new QNetworkAccessManager(&window);
    
    auto entry = [&](QString text) -> QLineEdit* {
        QLineEdit *input = new QLineEdit();
        input->setPlaceholderText(text);
        layout->addWidget(input);
        return input;
    };

    // Declara antes
    std::function<void()> showfeed;
    std::function<void()> initialPage;
    std::function<void()> showInitialPage;
    std::function<void()> options;
    std::function<void()> account;
    std::function<void()> searchPage;
    std::function<void()> chatPage;
    std::function<void()> friendsPage;
    std::function<void()> addFriendsPage;
    std::function<void()> inboxPage;
    std::function<void(const QString&)> chat;
    std::function<void(const QString&, const QString&)> addFriendsRequest;
    std::function<void(const QString&, const QString&)> sendMessage;
    std::function<void()> optionsPage;
    std::function<void()> addFederationsPage;
    std::function<void()> loginPage;
    std::function<void()> signinPage;
    std::function<void()> signupPage;
    std::function<void(const QString&, const QString&, const QString&)> signinRequest;
    std::function<void(const QString&, const QString&)> signupRequest;
    auto button = [&](QString text, std::function<void()> func)
    {
        QPushButton *btn = new QPushButton(text);
        layout->addWidget(btn);

        QObject::connect(btn, &QPushButton::clicked, [func]() {
            func();
        });
    };
    //novo post
    auto new_post_request = [&](QString text, QString username){
        QJsonObject post;
        post["username"] = username;
        post["text_post"] = text;
        post["datetime"] = "11/09/2001";
        QString response = requestHTTP(
            url + "/new",
            "POST",
            post
        );
        
    };
    auto new_post = [&](){
        clearLayout(layout);
        QLineEdit *text_input = entry(text_post);
        button(back_text, initialPage);
        button(
            new_post_text,
            [=]() {
                new_post_request(
                    text_input->text(),
                    QString::fromStdString(config["FAST-LOGIN"]["username"])
                );
            }
        );
        QLabel("post created with sucess!");
    };
    signinRequest = [&](QString username, QString password, QString email){
        QJsonObject json_signin;
        json_signin["username"] = username;
        json_signin["password"] = password;
        json_signin["email"] = email;
        QString request_signin = requestHTTP(
            url + "/register",
            "POST",
            json_signin
        );
    }
    signinPage = [&](){
        clearLayout(layout);
        QLineEdit *usernameEntry = new QLineEdit();
        QLineEdit *passwordEntry = new QLineEdit();
        QLineEdit *retryPasswordEntry = new QLineEdit();
        QLineEdit *emailEntry = new QLineEdit();
        usernameEntry->setPlaceholderText(username_text);
        passwordEntry->setPlaceholderText(password_text);
        retryPasswordEntry->setPlaceholderText(retry_password_text);
        emailEntry->setPlaceholderText(email_text);
        QPushButton *send_button = new QPushButton(send_text);
        QPushButton *back_button = new QPushButton(back_text);
        QObject::connect(back_button, &QPushButton::clicked, [=](){
            loginPage();
        });
        QObject::connect(send_button, &QPushButton::clicked, [=](){
            if (passwordEntry == retryPasswordEntry){
                signinRequest(usernameEntry->text(), passwordEntry->text(), emailEntry->text());
            };
        });
        layout->addWidget(usernameEntry);
        layout->addWidget(passwordEntry);
        layout->addWidget(retryPasswordEntry);
        layout->addWidget(emailEntry);
        layout->addWidget(send_button);
        layout->addWidget(back_button);

    };
    signupRequest = [&](QString username, QString password){
        QJsonObject json_signup;
        json_signup["username"] = username;
        json_signup["password"] = password;
        QString response_signup = requestHTTP(
            url + "/login",
            "POST",
            json_signup
        );
    };
    signupPage = [&](){
        clearLayout(layout);
        QLineEdit *usernameEntry = new QLineEdit();
        QLineEdit *passwordEntry = new QLineEdit();
        usernameEntry->setPlaceholderText(username_text);
        passwordEntry->setPlaceholderText(password_text);
        QPushButton *send_button = new QPushButton(send_text);
        QPushButton *back_button = new QPushButton(back_text);
        layout->addWidget(usernameEntry);
        layout->addWidget(passwordEntry);
        layout->addWidget(send_button);
        layout->addWidget(back_button);
        QObject::connect(back_button, &QPushButton::clicked, [=](){
            loginPage();
        });
        QObject::connect(send_button, &QPushButton::clicked, [=](){
            signupRequest(usernameEntry->text(), passwordEntry->text());
        });
    };
    loginPage = [&](){
        clearLayout(layout);
        QPushButton *signinPage_button = new QPushButton(signin_text);
        QPushButton *signupPage_button = new QPushButton(signup_text);
        layout->addWidget(signinPage_button);
        layout->addWidget(signupPage_button);
        QObject::connect(signinPage_button, &QPushButton::clicked, [&](){
            signinPage();
        });
        QObject::connect(signupPage_button, &QPushButton::clicked, [&](){
            signupPage();
        });
    };
    addFriendsRequest = [&](QString receiver, QString message){
        QJsonObject friend_json;
        friend_json["receiver"] = receiver;
        friend_json["remittee"] = username;
        friend_json["message"] = message;
        requestHTTP(
            url + "/send-friend",
            "POST",
            friend_json
        );
    };
    addFriendsPage = [&](){
        clearLayout(layout);
        QLineEdit *usernameEntry = entry(username_text);
        layout->addWidget(usernameEntry);
        QLineEdit *messageEntry = entry(message_text);
        layout->addWidget(messageEntry);
        QPushButton *back_button = new QPushButton(back_text);
        layout->addWidget(back_button);
        QPushButton *send_button = new QPushButton(send_text);
        layout->addWidget(send_button);
        QObject::connect(send_button, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [&](){
                    addFriendsRequest(usernameEntry->text(), messageEntry->text());
                    initialPage();
                });
        });
        QObject::connect(back_button, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [&](){
                    initialPage();
                });
        });
    };
    friendsPage = [&](){
        clearLayout(layout);
        QPushButton *new_friend = new QPushButton(add_friends_text);
        layout->addWidget(new_friend);
        QPushButton *back_button = new QPushButton(back_text);
        layout->addWidget(back_button);
        QObject::connect(back_button, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [&](){
                    initialPage();
                });
        });
        QObject::connect(new_friend, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [&](){
                    addFriendsPage();
                });
        });
    };
    inboxPage = [&](){
        clearLayout(layout);
        QList<QWidget*> notifications;
        QJsonObject inbox;
        inbox["username"] = username;
        QString response_inbox = requestHTTP(
            url + "/inbox",
            "POST",
            inbox
        );
        QJsonDocument doc = QJsonDocument::fromJson(response_inbox.toUtf8());
        QJsonObject obj = doc.object();
        QJsonArray inbox_json = obj["inbox"].toArray();
        for(int i = 0; i < inbox_json.size(); i++){
            for(int b = 0; b < inbox_json[i].toArray().size(); b++){
                QLabel *notification_inbox = new QLabel(inbox_json[i][b].toString());
                notifications.append(notification_inbox);
            };
            QPushButton *accept_button = new QPushButton(accept_text);
            notifications.append(accept_button);
            QPushButton *denied_button = new QPushButton(denied_text);
            notifications.append(denied_button);
            QObject::connect(accept_button, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [&](){
                    QJsonObject accept_json;
                    accept_json["receiver"] = username;
                    accept_json["remittee"] = inbox_json[i][1].toString();
                    requestHTTP(
                        url + "/accept",
                        "POST",
                        accept_json
                    );
                });
            });
            QObject::connect(denied_button, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [&](){
                    QJsonObject denied_json;
                    denied_json["receiver"] = username;
                    denied_json["remittee"] = inbox_json[i][1].toString();
                    requestHTTP(
                        url + "/denied",
                        "POST",
                        denied_json
                    );
                });
            });

        };
        scroll_area(layout, notifications);
        QPushButton *back_button = new QPushButton(back_text);
        layout->addWidget(back_button);
        QObject::connect(back_button, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [&](){
                    initialPage();
                });
        });
    };
    addFederationsPage = [&](){
        clearLayout(layout);
        QLineEdit *urlEntry = entry("url:");
        QPushButton *buttonAdd = new QPushButton(send_text);
        QPushButton *button_back = new QPushButton(back_text);
        QObject::connect(button_back, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [&](){
                    initialPage();
                });
        });
        layout->addWidget(urlEntry);
        layout->addWidget(buttonAdd);
        layout->addWidget(button_back);
        QObject::connect(buttonAdd, &QPushButton::clicked, [=]() mutable {

            qDebug() << "Caminho config:" << QFileInfo("config-login.cfg").absoluteFilePath();
            qDebug() << "Digitado:" << urlEntry->text();

            QString raw = QString::fromStdString(config["FEDERATIONS"]["url"]);

            if(raw.trimmed().isEmpty())
                raw = "[]";

            QJsonDocument doc = QJsonDocument::fromJson(raw.toUtf8());
            QJsonArray arr;

            if(doc.isArray())
                arr = doc.array();

            arr.append(urlEntry->text());

            QJsonDocument newDoc(arr);

            config["FEDERATIONS"]["url"] =
                newDoc.toJson(QJsonDocument::Compact).toStdString();

            saveConfig();

            qDebug() << "Depois de salvar:" << QString::fromStdString(config["FEDERATIONS"]["url"]);
        });
    };
    //menu de opções extras
    optionsPage = [&](){
        QList<QWidget*> buttons;
        clearLayout(layout);
        QPushButton *button_back = new QPushButton(back_text);
        QPushButton *button_add_theme = new QPushButton(add_theme_text);
        QPushButton *button_add_federation = new QPushButton(add_federations_text);
        QObject::connect(button_back, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [&](){
                    initialPage();
                });
        });
        QObject::connect(button_add_federation, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [&](){
                    addFederationsPage();
                });
        });
        buttons.append(button_back);
        buttons.append(button_add_theme);
        buttons.append(button_add_federation);
        scroll_area(layout, buttons);
    };
    options = [&]()
    {
        clearLayout(layout);
        QList<QWidget*> buttons;
        QPushButton *friends = new QPushButton(friends_text);
        QPushButton *back = new QPushButton(back_text);
        QPushButton *inbox = new QPushButton(inbox_text);
        QPushButton *button_options = new QPushButton(options_text);
        buttons.append(button_options);
        buttons.append(inbox);
        buttons.append(friends);
        buttons.append(back);
        QObject::connect(button_options, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [&](){
                    optionsPage();
                });
        });
        QObject::connect(inbox, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [&](){
                    inboxPage();
                });
        });
        QObject::connect(back, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [&](){
                    initialPage();
                });
        });
        QObject::connect(friends, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [&](){
                    friendsPage();
                });
        });
        scroll_area(layout, buttons);
    };
    account = [&](){
        clearLayout(layout);
        QLabel *label_username = new QLabel(username);
        layout->addWidget(label_username);
        QPushButton *buttonBack = new QPushButton(back_text);
        layout->addWidget(buttonBack);
        QObject::connect(buttonBack, &QPushButton::clicked, [=](){
            QTimer::singleShot(0, [&](){
                    initialPage();
                });
        });
    };
    showfeed = [&]()
    {
        clearLayout(layout);
        QString url_feed = url + "/feed";
        qDebug() << "url feed" << url_feed;
        QNetworkRequest request{QUrl(url_feed)};
        QNetworkReply *reply = manager->get(request);

        QObject::connect(reply, &QNetworkReply::finished, [=]() mutable {

            if(reply->error() != QNetworkReply::NoError)
            {
                QLabel *err = new QLabel("Erro ao carregar feed!");
                layout->addWidget(err);
                reply->deleteLater();
                return;
            }

            QByteArray responseData = reply->readAll();
            reply->deleteLater();

            QJsonDocument doc = QJsonDocument::fromJson(responseData);

            if(!doc.isArray())
            {
                QLabel *err = new QLabel("Resposta inválida do servidor!");
                layout->addWidget(err);
                return;
            }

            QJsonArray postsArray = doc.array();

            QList<QWidget*> labels;

            for(auto value : postsArray)
            {
                if(!value.isObject()) continue;

                QJsonObject post = value.toObject();

                int postId = post["id"].toInt();
                QString username = post["username"].toString();
                QString textPost = post["text_post"].toString();
                QString datetime = post["datetime"].toString();

                // ===== FRAME =====
                QFrame *frame = new QFrame();
                frame->setStyleSheet(R"(
                    QFrame {
                        background-color: #1A1A1A;
                        border: 1px solid #2F2F2F;
                        border-radius: 14px;
                        padding: 10px;
                    }
                )");

                QVBoxLayout *frameLayout = new QVBoxLayout(frame);
                QHBoxLayout *starLayout = new QHBoxLayout();

                QLabel *lblUser = new QLabel(username);
                QLabel *lblText = new QLabel(textPost);
                QLabel *lblDate = new QLabel(datetime);

                lblUser->setStyleSheet("color: white; font-size: 16px; font-weight: bold;");
                lblText->setStyleSheet("color: white; font-size: 14px;");
                lblDate->setStyleSheet("color: gray; font-size: 12px;");

                frameLayout->addWidget(lblUser);
                frameLayout->addWidget(lblText);
                frameLayout->addWidget(lblDate);

                // ===== BOTÃO STAR =====
                QPushButton *iconButton = new QPushButton();
                iconButton->setIcon(QIcon(":/assets/default_star.png"));
                iconButton->setIconSize(QSize(24, 24));
                iconButton->setFixedSize(30, 30);
                iconButton->setStyleSheet("border: none;");

                QLabel *starLabel = new QLabel("...");
                starLabel->setStyleSheet("color: white; font-size: 14px;");

                // buscar quantidade de estrelas
                QNetworkRequest starsReq(QUrl(url + "/return-stars/" + QString::number(postId)));
                QNetworkReply *starsReply = manager->get(starsReq);

                QObject::connect(starsReply, &QNetworkReply::finished, [=]() mutable {
                    if(starsReply->error() == QNetworkReply::NoError)
                    {
                        QString starsText = starsReply->readAll();
                        starLabel->setText(starsText);
                    }
                    else
                    {
                        starLabel->setText("0");
                    }

                    starsReply->deleteLater();
                });

                // clique da estrela (toggle)
                QObject::connect(iconButton, &QPushButton::clicked, [=]() mutable {
                    QJsonObject star_json;
                    star_json["username"] = username;
                    star_json["post_id"] = postId;
                    requestHTTP(
                        url + "/star",
                        "POST",
                        star_json
                    );
                    QString has_starred = requestHTTP(
                        url + "/has-star",
                        "POST",
                        star_json
                    );
                    QJsonDocument doc = QJsonDocument::fromJson(has_starred.toUtf8());
                    QJsonObject obj = doc.object();
                    bool starred = obj["starred"].toBool();
                    if (starred == true){
                        iconButton->setIcon(QIcon(":/assets/star.png"));
                    }else{
                        iconButton->setIcon(QIcon(":/assets/default_star.png"));
                    };

                });

                starLayout->addWidget(iconButton);
                starLayout->addWidget(starLabel);
                starLayout->addStretch();

                frameLayout->addLayout(starLayout);

                labels.append(frame);
            }

            scroll_area(layout, labels);

            // botões de baixo
            QPushButton *btnBack = new QPushButton(back_text);
            QPushButton *btnNewPost = new QPushButton(new_post_text);
            QObject::connect(btnBack, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [&](){
                    initialPage();
                });
            });
            layout->addWidget(btnBack);
            layout->addWidget(btnNewPost);

           

            QObject::connect(btnNewPost, &QPushButton::clicked, [=](){
                new_post();
                // new_post();
            });

        });
    };
    auto searchRequest = [&](QString content){
        QJsonObject search;
        search["content"] = content;

        QString response = requestHTTP(
            url + "/search",
            "POST",
            search
        );

        return response;
    };
    sendMessage = [&](QString message, QString user){
        QJsonObject chatJson;
        chatJson["receiver"] = user;
        chatJson["sender"] = username;
        chatJson["message"] = message;
        requestHTTP(
            url + "/send-message",
            "POST",
            chatJson
        );
    };
    //tela quando você esta conversando com o usuario
    chat = [&](QString user){
        clearLayout(layout);
        QList<QWidget*> message;
        QHBoxLayout *lineMessage;

        //scroll area pra mensagens
        QScrollArea *scroll = new QScrollArea();
        scroll->setWidgetResizable(true);

        QWidget *containerScroll = new QWidget();
        QVBoxLayout *containerLayout = new QVBoxLayout(containerScroll);

        scroll->setWidget(containerScroll);
        layout->addWidget(scroll);

        //parte grafica
        QTimer *timer = new QTimer();

        QObject::connect(timer, &QTimer::timeout, [=]() mutable{
            QJsonObject view_chat;
            view_chat["user1"] = username;
            view_chat["user2"] = user;

            QString chat_message = requestHTTP(
                url + "/view",
                "POST",
                view_chat
            );

            QJsonDocument doc = QJsonDocument::fromJson(chat_message.toUtf8());
            if (!doc.isObject()) return;

            QJsonObject obj = doc.object();
            if (!obj.contains("messages")) return;

            QJsonArray msgs = obj["messages"].toArray();
            QLayoutItem *child;
            while ((child = containerLayout->takeAt(0)) != nullptr)
            {
                if (child->widget())
                {
                    delete child->widget();
                }
                delete child;
            }

            for (int i = 0; i < msgs.size(); i++)
            {
                QJsonObject msg = msgs[i].toObject();

                QString sender = msg["sender"].toString();
                QString receiver = msg["receiver"].toString();
                QString text = msg["message"].toString();

                bool isMe = (sender == username);

                ChatBubble *bubble = new ChatBubble(text, isMe);

                QHBoxLayout *line = new QHBoxLayout();

                if (isMe)
                {
                    line->addStretch();
                    line->addWidget(bubble);
                }
                else
                {
                    line->addWidget(bubble);
                    line->addStretch();
                }

                QWidget *lineWidget = new QWidget();
                lineWidget->setLayout(line);

                containerLayout->addWidget(lineWidget);
            }

            

            //auto scroll pra baixo
            QTimer::singleShot(50, [=](){
                scroll->verticalScrollBar()->setValue(scroll->verticalScrollBar()->maximum());
            });
        });

        timer->start(2000);

        QLineEdit *message_box = new QLineEdit();
        message_box->setPlaceholderText(type_text);

        QHBoxLayout *entryBox = new QHBoxLayout();
        QPushButton *send_button = new QPushButton(send_text);

        entryBox->addWidget(message_box);
        entryBox->addWidget(send_button);

        QWidget *container = new QWidget();
        container->setLayout(entryBox);

        QPushButton *back_button = new QPushButton(back_text);

        QObject::connect(back_button, &QPushButton::clicked, [=]() mutable{
            QTimer::singleShot(0, [=](){
                timer->stop();
                initialPage();
            });
        });

        QObject::connect(send_button, &QPushButton::clicked, [=]() mutable{
            QTimer::singleShot(0, [=](){
                sendMessage(message_box->text(), user);
                message_box->clear();
            });
        });

        QObject::connect(message_box, &QLineEdit::returnPressed, [=]() mutable{
            send_button->click();
        });

        layout->addWidget(container);
        layout->addWidget(back_button);

    };
    //pagina inicial de chat
    chatPage = [&](){
        clearLayout(layout);
        QList<QWidget*> widgets;
        QJsonObject friends_json;
        friends_json["username"] = username;
        QString response_friends = requestHTTP(
            url + "/friends",
            "POST",
            friends_json
        );
        QJsonDocument doc = QJsonDocument::fromJson(response_friends.toUtf8());
        QJsonObject obj = doc.object();
        QJsonArray friends = obj["friends"].toArray();
        if (friends.isEmpty())
        {
            QLabel *label_error = new QLabel("sem amigos ;)");
            widgets.append(label_error);
        }
        else
        {
            for(int i = 0; i < friends.size(); i++){
                QJsonArray row = friends[i].toArray();
                QString receiver = row[0].toString();
                QString remittee = row[1].toString();
                QString friendName;
                if(receiver == username)
                    friendName = remittee;
                else
                    friendName = receiver;
                QPushButton *user = new QPushButton(friendName);
                QObject::connect(user, &QPushButton::clicked, [=]() mutable{
                    QTimer::singleShot(0, [=](){
                        chat(friendName);
                    });
                });
                widgets.append(user);
            };
        };
        QPushButton *back_button = new QPushButton(back_text);
        QObject::connect(back_button, &QPushButton::clicked, [=]() mutable{
            QTimer::singleShot(0, [=](){
                initialPage();
            });
        });
        widgets.append(back_button);
        scroll_area(layout, widgets);
    };
    searchPage = [&](){
        clearLayout(layout);

        QList<QWidget*> content;

        QLineEdit *searchEntry = new QLineEdit();
        searchEntry->setPlaceholderText(search_text);
        content.append(searchEntry);

        QPushButton *buttonSearch = new QPushButton(search_text + "!");
        content.append(buttonSearch);

        QLabel *resultLabel = new QLabel("");
        resultLabel->setWordWrap(true);
        content.append(resultLabel);

        // Área onde os resultados vão aparecer
        QWidget *resultsContainer = new QWidget();
        QVBoxLayout *resultsLayout = new QVBoxLayout(resultsContainer);
        resultsLayout->setContentsMargins(0,0,0,0);
        resultsLayout->setSpacing(10);

        content.append(resultsContainer);

        QPushButton *button_back = new QPushButton(back_text);

        QObject::connect(button_back, &QPushButton::clicked, [=](){
            QTimer::singleShot(0, [=](){
                initialPage();
            });
        });

        QObject::connect(buttonSearch, &QPushButton::clicked, [=]() mutable {
            QTimer::singleShot(0, [=]() mutable {

                // limpar resultados anteriores
                QLayoutItem *child;
                while ((child = resultsLayout->takeAt(0)) != nullptr) {
                    if (child->widget()) {
                        child->widget()->deleteLater();
                    }
                    delete child;
                }

                QString source_response = searchRequest(searchEntry->text());

                if (source_response.isEmpty())
                {
                    resultLabel->setText("");
                    return;
                }

                QJsonDocument doc = QJsonDocument::fromJson(source_response.toUtf8());

                if (!doc.isObject())
                {
                    resultLabel->setText(source_response);
                    return;
                }

                QJsonObject obj = doc.object();

                // ===== usernames =====
                if (obj.contains("usernames") && obj["usernames"].isArray())
                {
                    QJsonArray arr = obj["usernames"].toArray();

                    for (auto v : arr)
                    {
                        QString user = v.toString();

                        QFrame *frame = new QFrame();
                        frame->setStyleSheet(R"(
                            QFrame {
                                background-color: #1A1A1A;
                                border: 1px solid #2F2F2F;
                                border-radius: 14px;
                                padding: 10px;
                            }
                        )");

                        QVBoxLayout *frameLayout = new QVBoxLayout(frame);

                        QLabel *lbl = new QLabel(user);
                        lbl->setStyleSheet("color: white; font-size: 16px; font-weight: bold;");

                        frameLayout->addWidget(lbl);

                        resultsLayout->addWidget(frame);
                    }
                }

                // ===== posts =====
                if (obj.contains("posts") && obj["posts"].isArray())
                {
                    QJsonArray arr = obj["posts"].toArray();

                    for (auto v : arr)
                    {
                        QString post = v.toString();

                        QFrame *frame = new QFrame();
                        frame->setStyleSheet(R"(
                            QFrame {
                                background-color: #1A1A1A;
                                border: 1px solid #2F2F2F;
                                border-radius: 14px;
                                padding: 10px;
                            }
                        )");

                        QVBoxLayout *frameLayout = new QVBoxLayout(frame);

                        QLabel *lbl = new QLabel(post);
                        lbl->setWordWrap(true);
                        lbl->setStyleSheet("color: white; font-size: 14px;");

                        frameLayout->addWidget(lbl);

                        resultsLayout->addWidget(frame);
                    }
                }

                resultsLayout->addStretch();

            });
        });

        content.append(button_back);

        scroll_area(layout, content);
    };
    //pagina inicial para renderizar
    initialPage = [&]()
    {
        
        if (config["FAST-LOGIN"]["username"].empty() || config["FAST-LOGIN"]["password"].empty()){
            loginPage();
            return;
        }
        
        clearLayout(layout);
        splash.finish(&window);
        QStackedWidget *stack = new QStackedWidget(central);
        // ======= PÁGINAS =======
        QWidget *pageHome = new QWidget();
        QVBoxLayout *homeLayout = new QVBoxLayout(pageHome);
        QLabel *homeLabel = new QLabel("🏠 HOME");
        homeLabel->setAlignment(Qt::AlignCenter);
        homeLayout->addWidget(homeLabel);
        QWidget *pageSearch = new QWidget();
        QVBoxLayout *searchLayout = new QVBoxLayout(pageSearch);
        QLabel *searchLabel = new QLabel("🔍 BUSCA");
        searchLabel->setAlignment(Qt::AlignCenter);
        searchLayout->addWidget(searchLabel);
        QWidget *pageProfile = new QWidget();
        QVBoxLayout *profileLayout = new QVBoxLayout(pageProfile);
        QLabel *profileLabel = new QLabel("👤 PERFIL");
        profileLabel->setAlignment(Qt::AlignCenter);
        profileLayout->addWidget(profileLabel);
        QWidget *pageChat = new QWidget();
        QWidget *pageOptions = new QWidget();
        stack->addWidget(pageChat);    // index 3
        stack->addWidget(pageOptions); // index 4
        stack->addWidget(pageHome);
        stack->addWidget(pageSearch);
        stack->addWidget(pageProfile);
        // ======= BARRA INFERIOR =======
        QWidget *bottomBar = new QWidget(central);
        bottomBar->setFixedHeight(90);
        QPushButton *btnHome = new QPushButton(bottomBar);
        QPushButton *btnSearch = new QPushButton(bottomBar);
        QPushButton *btnChat = new QPushButton(bottomBar);
        QPushButton *btnProfile = new QPushButton(bottomBar);
        QPushButton *btnOptions = new QPushButton(bottomBar);
        QIcon options_icon(":/assets/options.png");
        btnOptions->setIcon(options_icon);
        btnOptions->setIconSize(QSize(64, 64));
        QIcon search_icon(":/assets/search.png");
        btnSearch->setIcon(search_icon);
        btnSearch->setIconSize(QSize(64, 64));
        QIcon icon_home(":/assets/home.png");
        btnHome->setIcon(icon_home);
        btnHome->setIconSize(QSize(64, 64));
        QIcon icon_chat(":/assets/chat.png");
        btnChat->setIcon(icon_chat);
        btnChat->setIconSize(QSize(64, 64));
        QIcon icon_account(":/assets/account.png");
        btnProfile->setIcon(icon_account);
        btnProfile->setIconSize(QSize(64, 64));
        btnHome->setFixedSize(64, 64);
        btnChat->setFixedSize(64, 64);
        btnProfile->setFixedSize(64, 64);
        btnSearch->setFixedSize(64, 64);
        btnOptions->setFixedSize(64, 64);
        QObject::connect(btnHome, &QPushButton::clicked, [=]() {
            showfeed();
        });
        QObject::connect(btnOptions, &QPushButton::clicked, [=]() {
            options();
        });
        QObject::connect(btnSearch, &QPushButton::clicked, [=]() {
            searchPage();
        });
        QObject::connect(btnProfile, &QPushButton::clicked, [=]() {
            account();
        });
        QObject::connect(btnChat, &QPushButton::clicked, [=]() {
            chatPage();
        });
        btnHome->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
        btnChat->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
        btnProfile->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
        btnOptions->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
        btnSearch->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
        QHBoxLayout *barLayout = new QHBoxLayout(bottomBar);
        barLayout->setContentsMargins(10, 10, 10, 10);
        barLayout->setSpacing(10);
        barLayout->addWidget(btnHome);
        barLayout->addWidget(btnChat);
        barLayout->addWidget(btnProfile);
        barLayout->addWidget(btnOptions);
        barLayout->addWidget(btnSearch);
        QObject::connect(btnHome, &QPushButton::clicked, [=](){
            stack->setCurrentIndex(0);
        });
        QObject::connect(btnSearch, &QPushButton::clicked, [=](){
            stack->setCurrentIndex(1);
        });
        QObject::connect(btnChat, &QPushButton::clicked, [=](){
            stack->setCurrentIndex(2);
        });

        QObject::connect(btnProfile, &QPushButton::clicked, [=](){
            stack->setCurrentIndex(3);
        });
        QObject::connect(btnOptions, &QPushButton::clicked, [=](){
            stack->setCurrentIndex(4);
        });
        
        // ======= ESTILO =======
        bottomBar->setStyleSheet("background: #111;");
        btnOptions->setStyleSheet("font-size: 32px; border: none; color: white; background: transparent;");
        btnHome->setStyleSheet("font-size: 32px; border: none; color: #00ffea; background: transparent;");
        btnChat->setStyleSheet("font-size: 32px; border: none; color: white; background: transparent;");
        btnProfile->setStyleSheet("font-size: 32px; border: none; color: white; background: transparent;");
        btnSearch->setStyleSheet("font-size: 32px; border: none; color: white; background: transparent;");

        // ======= MONTAGEM =======
        layout->addWidget(stack, 1);
        layout->addWidget(bottomBar, 0);
    };
    //chamada da função
    initialPage();
    //função para exibir o feed
    window.show();
    return app.exec();
}
