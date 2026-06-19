// menu.cpp — adaptado para Qt 5.1 (arquivo completo)
#include <QFileDialog>
#include <QTextStream>
#include <QMessageBox>
#include <QAction>
#include <QPointer>
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
#include <QDesktopServices>
#include <QUrl>
#include <QVector>
#include <QSplashScreen>
#include <QStandardPaths>
#include <QJsonDocument>
#include <QMenu>
#include <QScroller>
#include <iostream>
#include <QJsonObject>
#include <QLabel>
#include <QtGlobal>
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
#include <QDateTime>
#include <QSize>
#include <nlohmann/json.hpp>
#include <QPainter>
#include <QFontMetrics>
#include <QTranslator>
#include <QGraphicsOpacityEffect>
#include <QPropertyAnimation>
#include <QDesktopWidget>

using json = nlohmann::json;

// Helper singleShot para Qt 5.1 (usa QTimer + lambda)
static void singleShot(int msec, QObject *parent, const std::function<void()> &fn)
{
    QTimer *t = new QTimer(parent);
    t->setSingleShot(true);
    QObject::connect(t, &QTimer::timeout, [t, fn]() {
        try { fn(); } catch (...) {}
        t->deleteLater();
    });
    t->start(msec);
}

void fadeTransition(QWidget *widget)
{
    QGraphicsOpacityEffect *effect = new QGraphicsOpacityEffect(widget);
    widget->setGraphicsEffect(effect);

    QPropertyAnimation *anim = new QPropertyAnimation(effect, "opacity");
    anim->setDuration(300);
    anim->setStartValue(0.0);
    anim->setEndValue(1.0);

    anim->start(QAbstractAnimation::DeleteWhenStopped);
}
class ChatBubble : public QWidget {
public:
    ChatBubble(QString text, bool isMe, QWidget *parent = nullptr)
        : QWidget(parent), message(text), mine(isMe)
    {
        setMaximumWidth(400);
        setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Fixed);
    }

protected:
    void paintEvent(QPaintEvent * /*event*/) override {
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
std::string trim(const std::string &s)
{
    size_t start = s.find_first_not_of(" \t\r\n");
    size_t end = s.find_last_not_of(" \t\r\n");

    if(start == std::string::npos)
        return "";

    return s.substr(start, end - start + 1);
}
std::map<std::string, std::map<std::string, std::string>> config;
QString configPath() {
    // Qt 5.1: use QStandardPaths::DataLocation fallback to home
    QString dirPath = QStandardPaths::writableLocation(QStandardPaths::DataLocation);
    if (dirPath.isEmpty()) {
        dirPath = QDir::homePath() + "/.linka";
    }
    QDir().mkpath(dirPath);
    return dirPath + "/config-login.cfg";
}
void loadConfig()
{
    QString path = configPath();
    qDebug() << "Caminho config:" << path;

    // Se não existe, copia do resource
    if (!QFile::exists(path))
    {
        qDebug() << "Config não existe, copiando do resource...";

        QFile res(":/config-login.cfg");

        if (!res.open(QIODevice::ReadOnly | QIODevice::Text))
        {
            qDebug() << "ERRO: não conseguiu abrir o resource :/config-login.cfg";
            return;
        }

        QFile out(path);

        if (!out.open(QIODevice::WriteOnly | QIODevice::Text))
        {
            qDebug() << "ERRO: não conseguiu criar config em:" << path;
            res.close();
            return;
        }

        out.write(res.readAll());

        out.close();
        res.close();

        qDebug() << "Config copiada com sucesso!";
    }

    QFile file(path);

    if (!file.open(QIODevice::ReadOnly | QIODevice::Text))
    {
        qDebug() << "ERRO ao abrir config-login.cfg no caminho:" << path;
        return;
    }

    QTextStream in(&file);

    std::string section;
    QString line;

    while (!in.atEnd())
    {
        line = in.readLine().trimmed();

        if (line.isEmpty()) continue;

        if (line.startsWith("[") && line.contains("]"))
        {
            section = line.toStdString();
            section = section.substr(1, section.find(']') - 1);
        }
        else
        {
            int pos = line.indexOf('=');

            if (pos != -1)
            {
                QString key = line.left(pos).trimmed();
                QString value = line.mid(pos + 1).trimmed();

                config[section][key.toStdString()] = value.toStdString();
            }
        }
    }

    file.close();

    qDebug() << "Config carregada com sucesso!";
}
void saveConfig() {

    QString dirPath = QStandardPaths::writableLocation(QStandardPaths::DataLocation);
    if (dirPath.isEmpty()) {
        dirPath = QDir::homePath() + "/.linka";
    }
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
    file << "token_session = " << config["FAST-LOGIN"]["token_session"] << "\n\n";
    file << "[FEDERATIONS]\n";
    file << "url = " << config["FEDERATIONS"]["url"] << "\n\n";

    file << "[THEMES]\n";
    file << "theme = " << config["THEMES"]["theme"] << "\n";

    file.close();
}
QString requestHTTP(
    const QString &url,
    const QString &method,
    const QJsonObject &json,
    int timeoutMs = 5000,
    int *statusCode = nullptr
);
QString newSession(QString username, QString password)
{
    loadConfig();

    QString url =
        QString::fromStdString(
            config["SERVER"]["url"]
        );

    QNetworkAccessManager manager;

    QJsonObject j;
    j["username"] = username;
    j["password"] = password;

    QNetworkRequest request;
    request.setUrl(QUrl(url + "/new-session"));

    request.setHeader(
        QNetworkRequest::ContentTypeHeader,
        "application/json"
    );

    QByteArray data =
        QJsonDocument(j).toJson();

    QNetworkReply *reply =
        manager.post(request, data);

    QEventLoop loop;

    QObject::connect(
        reply,
        &QNetworkReply::finished,
        &loop,
        &QEventLoop::quit
    );

    loop.exec();

    QString response =
        reply->readAll();

    reply->deleteLater();

    QJsonDocument doc =
        QJsonDocument::fromJson(
            response.toUtf8()
        );

    if(!doc.isObject())
    {
        qDebug() << "Resposta inválida:" << response;
        return "";
    }

    QJsonObject obj = doc.object();

    QString token =
        obj.value("token").toString();

    return token;
}
QString renoveToken() {
    loadConfig();
    
    QString url = QString::fromStdString(config["SERVER"]["url"]);
    
    // Criamos um manager exclusivo e isolado para esta renovação
    QNetworkAccessManager isolatorManager;
    QNetworkRequest request;
    request.setUrl(QUrl(url + "/new-session"));
    request.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");

    // Montando o payload usando nlohmann::json (C++ antigo compatível)
    json newTokenJson;
    newTokenJson["username"] = config["FAST-LOGIN"]["username"];
    newTokenJson["password"] = config["FAST-LOGIN"]["password"];
    
    std::string s = newTokenJson.dump();
    QByteArray data(s.c_str(), static_cast<int>(s.size()));
    
    // Faz o POST direto pelas entranhas do Qt Network
    QNetworkReply *reply = isolatorManager.post(request, data);

    // EventLoop local para travar a execução até responder (Síncrono)
    QEventLoop loop;
    QObject::connect(reply, SIGNAL(finished()), &loop, SLOT(quit()));
    loop.exec();

    // Lê a resposta bruta do servidor
    QString response = reply->readAll();
    int code = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
    reply->deleteLater();

    // Se a renovação de token falhar ou der erro de credenciais (ex: 401 ou 400)
    // nós paramos aqui para evitar loops com o servidor
    if (code != 200 && code != 201) {
        qDebug() << "Falha critica ao renovar token. Status:" << code;
        return "";
    }

    try {
        json doc = json::parse(response.toStdString());
        std::string newToken = doc.value("token", "");
        
        if (!newToken.empty()) {
            config["FAST-LOGIN"]["token_session"] = newToken;
            saveConfig();
            qDebug() << "Token renovado com sucesso via bypass!";
            return QString::fromStdString(newToken);
        }
    } catch (...) {
        qDebug() << "Erro ao parsear JSON na renovacao de token.";
    }

    return "";
}
QString requestHTTP(const QString &url,
                    const QString &method,
                    const QJsonObject &json,
                    int timeoutMs,
                    int *statusCode)
{
    QNetworkAccessManager manager;
    QNetworkRequest request;
    
    request.setUrl(QUrl(url));

    QString m = method.toUpper();

    // Se não for GET, define o cabeçalho de JSON
    if (m != "GET") {
        request.setHeader(
            QNetworkRequest::ContentTypeHeader,
            "application/json"
        );
    }
    loadConfig();
    QString token = QString::fromStdString(config["FAST-LOGIN"]["token_session"]);
    if (!token.isEmpty()) {
        request.setRawHeader(
            "Authorization", 
            QString("Bearer %1").arg(token).toUtf8()
        );
    };
    QNetworkReply *reply = nullptr;
    QByteArray jsonData = QJsonDocument(json).toJson();

    // Dispara o método correto baseado na string
    if (m == "GET") {
        reply = manager.get(request);
    } else if (m == "POST") {
        reply = manager.post(request, jsonData);
    } else if (m == "PUT") {
        reply = manager.put(request, jsonData);
    } else if (m == "DELETE") {
        // Qt 5.1 sendCustomRequest supports only verb (and optional QIODevice*), body support is inconsistent.
        reply = manager.sendCustomRequest(request, QByteArray("DELETE"));
    } else {
        if (statusCode) *statusCode = -1;
        return "ERROR: invalid method";
    }
    
    // Loop de eventos local para aguardar o request de forma síncrona
    QEventLoop loop;
    QTimer timer;
    timer.setSingleShot(true);
    
    QObject::connect(&timer, &QTimer::timeout, &loop, &QEventLoop::quit);
    QObject::connect(reply, &QNetworkReply::finished, &loop, &QEventLoop::quit);
    
    timer.start(timeoutMs);
    loop.exec();
    
    // Tratamento de Timeout
    if (!timer.isActive()) {
        reply->abort();
        if (statusCode) *statusCode = 408;
        reply->deleteLater();
        return "ERRO: Timeout";
    }
    
    
    // Captura o Status Code real retornado pelo servidor (ex: 200, 404, 500)
    int code = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
    if (statusCode) *statusCode = code;
    if (code == 401){
        renoveToken();
    };
    // Lê a resposta bruta do servidor
    QString response = reply->readAll();
    reply->deleteLater();
    
    return response;
}

#if QT_VERSION >= QT_VERSION_CHECK(5, 0, 0)

#endif

void scroll_area(QVBoxLayout *layout, const QList<QWidget*> &widgets)
{
    QScrollArea *scroll = new QScrollArea();
    scroll->setWidgetResizable(true);
    scroll->setFrameShape(QFrame::NoFrame); 

#if QT_VERSION >= QT_VERSION_CHECK(5, 0, 0)
    // QScroller exists in Qt 5.x; in Qt 5.1 it may exist but be platform dependent — keep it guarded
    QScroller::grabGesture(scroll, QScroller::LeftMouseButtonGesture);
    scroll->setVerticalScrollBarPolicy(Qt::ScrollBarAlwaysOff);
#else
    scroll->setVerticalScrollBarPolicy(Qt::ScrollBarAsNeeded);
    scroll->setHorizontalScrollBarPolicy(Qt::ScrollBarAlwaysOff);
#endif
    QWidget *container = new QWidget();
    QVBoxLayout *containerLayout = new QVBoxLayout(container);
    containerLayout->setContentsMargins(0, 0, 0, 0); 
    containerLayout->setSpacing(10);
    for(int i = 0; i < widgets.size(); ++i)
    {
        containerLayout->addWidget(widgets.at(i));
    }
    containerLayout->addStretch();
    scroll->setWidget(container);
    layout->addWidget(scroll);
}

void loadStyle()
{
    loadConfig();

    QString themePath = QString::fromStdString(config["THEMES"]["theme"]);

    // fallback seguro
    if (themePath.isEmpty()) {
        themePath = ":/styles/theme.qss";
    }

    QFile file(themePath);

    if (!file.exists()) {
        qDebug() << "QSS not found:" << themePath;
        return;
    }

    if (!file.open(QFile::ReadOnly | QFile::Text)) {
        qDebug() << "QSS FAILED:" << file.errorString();
        return;
    }

    QString qss = file.readAll();

    if (qss.isEmpty()) {
        qDebug() << "QSS empty file!";
        return;
    }

    qApp->setStyleSheet(qss);
    qDebug() << "QSS loaded";
}

void clearLayout(QLayout *layout) {
    if (!layout) return; 

    QLayoutItem *item;
    while ((item = layout->takeAt(0))) {
        if (item->layout()) {
            clearLayout(item->layout());
        } else if (item->widget()) {
            QWidget *widget = item->widget();
            widget->setParent(NULL); 
            widget->deleteLater(); 
        }
        delete item; 
    }
}
int main(int argc, char *argv[])
{
    #if QT_VERSION < QT_VERSION_CHECK(6, 0, 0)
    #ifdef AA_EnableHighDpiScaling
    QCoreApplication::setAttribute(Qt::AA_EnableHighDpiScaling);
    #endif
    #ifdef AA_UseHighDpiPixmaps
    QCoreApplication::setAttribute(Qt::AA_UseHighDpiPixmaps);
    #endif
    #endif

    QApplication app(argc, argv);
    QTranslator *translator = new QTranslator(&app);
    loadConfig();
    QString current_version = "1.0";
    QString username = QString::fromStdString(config["FAST-LOGIN"]["username"]);
    QString token_session = QString::fromStdString(config["FAST-LOGIN"]["token_session"]);
    
    if (token_session.isEmpty()){
        QJsonObject json_token;
        QString response = requestHTTP(
            QString::fromStdString(config["SERVER"]["url"]) + "/new-session",
            "POST",
            json_token
        );
        QByteArray byteArray = response.toUtf8();
        QJsonDocument doc = QJsonDocument::fromJson(byteArray);
        QJsonObject jsonObject = doc.object();
        QString new_token = jsonObject.value("token").toString();
        config["FAST-LOGIN"]["token_session"] = new_token.toStdString();
        saveConfig();
    }
    
    if (config["LANG"]["lang"] == "pt-br"){
        if (translator->load(":/translations/pt-br-main-page.qm")) {
            app.installTranslator(translator);
        }
    };    
    
    QStyle *st = QStyleFactory::create("breeze");
    if (!st) st = QStyleFactory::create("Fusion");
    if (st) app.setStyle(st);
    loadConfig();
    loadStyle();
    
    for (auto &sec : config) {
        std::cout << "[" << sec.first << "]\n";
        for (auto &kv : sec.second) {
            std::cout << "  " << kv.first << " = " << kv.second << "\n";
        }
    }
    
    QString url = QString::fromStdString(config["SERVER"]["url"]);
    if (url.isEmpty())
    {
        config["SERVER"]["url"] = "http://linkaProject.pythonanywhere.com";
        url = QString::fromStdString(config["SERVER"]["url"]);
        saveConfig();
    }
    qDebug() << "url" << url;   
    QRect screenGeometry = QApplication::desktop()->screenGeometry();
    int larguraTela = screenGeometry.width();
    int alturaTela = screenGeometry.height();

    
    QFont fonteGlobal = app.font();
    if (larguraTela >= 1080) {
        fonteGlobal.setPointSize(16); 
    } else if (larguraTela >= 720) {
        fonteGlobal.setPointSize(13); 
    } else {
        fonteGlobal.setPointSize(11);
    }
    app.setFont(fonteGlobal);
    QMainWindow window;
    app.setWindowIcon(QIcon(":/assets/icon.png"));
    QPixmap pixmap(":/assets/icon.png");
    QSplashScreen splash(pixmap);
    splash.show();
    window.setWindowTitle("Linka Mobile");
    window.setGeometry(0, 0, larguraTela, alturaTela);
    QWidget *central = new QWidget();
    QVBoxLayout *layout = new QVBoxLayout(central);
    int margemCalculada = larguraTela * 0.05;
    layout->setContentsMargins(margemCalculada, margemCalculada, margemCalculada, margemCalculada);
    layout->setSpacing(alturaTela * 0.03);
    //strings traduzidas
    QString text_post = QCoreApplication::translate("feed", "text post");
    QString back_text = QCoreApplication::translate("global", "back");
    QString new_post_text = QCoreApplication::translate("feed", "new post");
    QString friends_text = QCoreApplication::translate("initial-page", "friends");
    QString search_text = QCoreApplication::translate("initial-page", "Search");
    QString add_friends_text = QCoreApplication::translate("initial-page", "add friends");
    QString username_text = QCoreApplication::translate("global", "username");
    QString message_text = QCoreApplication::translate("add friends", "message");
    QString send_text = QCoreApplication::translate("global", "send");
    QString inbox_text = QCoreApplication::translate("main-page", "inbox");
    QString accept_text = QCoreApplication::translate("inbox", "accept");
    QString denied_text = QCoreApplication::translate("inbox", "denied");
    QString type_text = QCoreApplication::translate("chat", "type here");
    QString add_theme_text = QCoreApplication::translate("configurations", "add theme");
    QString add_federations_text = QCoreApplication::translate("configurations", "add-federations");
    QString options_text = QCoreApplication::translate("initial-page", "configurations");
    QString signin_text = QCoreApplication::translate("initial-page", "sign-in");
    QString signup_text = QCoreApplication::translate("initial-page", "sign-up");
    QString password_text = QCoreApplication::translate("sign-up", "password");
    QString retry_password_text = QCoreApplication::translate("sign-up", "retry the password");
    QString email_text = QCoreApplication::translate("sign-up", "email");
    QString edit_text = QCoreApplication::translate("my account", "edit");
    QString upload_text = QCoreApplication::translate("my account", "upload");
    QString bio_text = QCoreApplication::translate("my account", "biography");
    QString profile_picture_text = QCoreApplication::translate("my account", "profile picture");
    QString comments_text = QCoreApplication::translate("feed", "comments");
    QString new_comment_text = QCoreApplication::translate("feed", "new comment");
    QString banned_text = QCoreApplication::translate("banned", "bannedText");
    QString exit_text = QCoreApplication::translate("banned", "exit");
    QString update_text = QCoreApplication::translate("update", "update_text");
    QString update_now_text = QCoreApplication::translate("update", "update_now");
    QString view_profile = QCoreApplication::translate("feed", "view_profile");
    QString un_friend_text = QCoreApplication::translate("view profile", "Unfriend");
    QString sent_friend_text = QCoreApplication::translate("view profile", "Sent a friend");
    QString change_lang_page = QCoreApplication::translate("configurations", "change lang");
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
    std::function<void()> chatGlobal;
    std::function<void(const QString&, const QString&)> addFriendsRequest;
    std::function<void(const QString&, const QString&)> sendMessage;
    std::function<void()> optionsPage;
    std::function<void()> addFederationsPage;
    std::function<void()> loginPage;
    std::function<void()> signinPage;
    std::function<void()> signupPage;
    std::function<int(const QString&, const QString&, const QString&)> signinRequest;
    std::function<int(const QString&, const QString&)> signupRequest;
    std::function<void()> changeServerPage;
    std::function<void()> addThemePage;
    std::function<void()> editAccount;
    std::function<void(QString)> sendEdit;
    std::function<void()> change_url;
    std::function<void(QString)> commentPage;
    std::function<QList<QString>(QString)> commentRequest;
    std::function<void(QString, QString)> newCommentRequest;
    std::function<void(QString)> newCommentPage;
    std::function<void()> fast_login;
    std::function<void()> changeLangPage;
    std::function<void(QString)> bannedPage;
    std::function<void(QString)> updatePage;
    std::function<void(QString)> sentUnFriendRequest;
    std::function<void(QString)> sentFriendRequest;
    std::function<void(QString)> otherProfilePage;
    loginPage = [&](){
        clearLayout(layout);
        fadeTransition(central);
        QPushButton *signinPage_button = new QPushButton(signup_text);
        QPushButton *signupPage_button = new QPushButton(signin_text);
        QPushButton *change_server_button = new QPushButton();
        layout->addWidget(signinPage_button);
        layout->addWidget(signupPage_button);
        layout->addWidget(change_server_button);
        QObject::connect(signinPage_button, &QPushButton::clicked, [&](){
            signinPage();
        });
        QObject::connect(signupPage_button, &QPushButton::clicked, [&](){
            signupPage();
        });
        QObject::connect(change_server_button, &QPushButton::clicked, [&](){
            changeServerPage();
        });

    };
    updatePage = [&](QString link){
        clearLayout(layout);
        QLabel *title = new QLabel(update_text);
        QPushButton *buttonUpdate = new QPushButton(update_now_text);
        layout->addWidget(title);
        layout->addWidget(buttonUpdate);
        QObject::connect(buttonUpdate, &QPushButton::clicked, [=](){
            QDesktopServices::openUrl(QUrl(link));
        });
    };
    QString response_version = requestHTTP(
        url + "/meta",
        "GET",
        QJsonObject()
    );
    QByteArray byteArray = response_version.toUtf8();
    QJsonDocument doc = QJsonDocument::fromJson(byteArray);
    QJsonObject jsonObject = doc.object();
    
    // 1. Pegue as strings e use .trimmed() para limpar qualquer espaço ou \n invisível
    QString min_ver_str = jsonObject.value("minim-version").toString().trimmed();
    QString linkUpdate = jsonObject.value("url").toString();
    QString cur_ver_str = current_version.trimmed();

    double min_version_num = min_ver_str.toDouble();
    double cur_version_num = cur_ver_str.toDouble();
    qDebug() << "--- CHECAGEM DE VERSÃO ---";
    qDebug() << "Do Servidor (string):" << min_ver_str << " -> (número):" << min_version_num;
    qDebug() << "No App Local (string):" << cur_ver_str << " -> (número):" << cur_ver_str;

    // 3. Faça a comparação numérica pura. Não tem como o C++ errar que 2.0 > 1.0!
    if (min_version_num > cur_version_num){
        qDebug() << "Bloqueando o app! Indo para a tela de atualização...";
        updatePage(linkUpdate);
        window.show();
        return app.exec(); 
    }
    // validation of token
    QString token = QString::fromStdString(config["FAST-LOGIN"]["token_session"]);
    QJsonObject json_valide;
    json_valide.insert("token", QJsonValue(token));
    int status_code;
    requestHTTP(
        url + "/valide",
        "POST",
        json_valide,
        5000,
        &status_code
    );
    if (token.isEmpty()){
        loginPage();
    };
    if (status_code == 200 || status_code == 201){
        qDebug() << "200";
    }else{
        loginPage();
    };
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
        post.insert("username", QJsonValue(username));
        post.insert("text_post", QJsonValue(text));
        QDateTime actualHour = QDateTime::currentDateTime();
        QString formatedHour = actualHour.toString("yyyy-MM-dd HH:mm:ss");
        post.insert("datetime", QJsonValue(formatedHour));
        QString response = requestHTTP(
            url + "/new",
            "POST",
            post
        );
        initialPage();
        
    };
    auto new_post = [&](){
        clearLayout(layout);
        fadeTransition(central);
        
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
        QLabel *created = new QLabel("post created with sucess!");
        layout->addWidget(created);
        
    };
    
    friendsPage = [&](){
        clearLayout(layout);
        fadeTransition(central);
        QPushButton *new_friend = new QPushButton(add_friends_text);
        layout->addWidget(new_friend);
        QPushButton *back_button = new QPushButton(back_text);
        layout->addWidget(back_button);
        QObject::connect(back_button, &QPushButton::clicked, [=](){
                initialPage();
        });
        QObject::connect(new_friend, &QPushButton::clicked, [=](){
                addFriendsPage();
        });
    };
    inboxPage = [&](){
        clearLayout(layout);
        fadeTransition(central);
        QList<QWidget*> notifications;
        QJsonObject inbox;
        inbox.insert("username", QJsonValue(username));
        QString response_inbox = requestHTTP(
            url + "/inbox",
            "POST",
            inbox
        );
        QJsonDocument doc = QJsonDocument::fromJson(response_inbox.toUtf8());
        QJsonObject obj = doc.object();
        QJsonArray inbox_json = obj.value("inbox").toArray();
        for(int i = 0; i < inbox_json.size(); i++){
            QJsonArray item = inbox_json[i].toArray();
            for(int b = 0; b < item.size(); b++){
                QLabel *notification_inbox = new QLabel(item[b].toString());
                notifications.append(notification_inbox);
            };
            QPushButton *accept_button = new QPushButton(accept_text);
            notifications.append(accept_button);
            QPushButton *denied_button = new QPushButton(denied_text);
            notifications.append(denied_button);
            QObject::connect(accept_button, &QPushButton::clicked, [=](){
                singleShot(0, central, [=]() {
                    QJsonObject accept_json;
                    accept_json.insert("receiver", QJsonValue(username));
                    QString remittee = item.size() > 1 ? item[1].toString() : QString();
                    accept_json.insert("remittee", QJsonValue(remittee));
                    requestHTTP(
                        url + "/accept",
                        "POST",
                        accept_json
                    );
                });
            });
            QObject::connect(denied_button, &QPushButton::clicked, [=](){
                singleShot(0, central, [=]() {
                    QJsonObject denied_json;
                    denied_json.insert("receiver", QJsonValue(username));
                    QString remittee = item.size() > 1 ? item[1].toString() : QString();
                    denied_json.insert("remittee", QJsonValue(remittee));
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
                initialPage();
        });
    };
    addFederationsPage = [&](){
        clearLayout(layout);
        fadeTransition(central);
        QLineEdit *urlEntry = entry("url:");
        QPushButton *buttonAdd = new QPushButton(send_text);
        QPushButton *button_back = new QPushButton(back_text);
        QObject::connect(button_back, &QPushButton::clicked, [=](){
                initialPage();
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

            QByteArray ba = newDoc.toJson(QJsonDocument::Compact);

            config["FEDERATIONS"]["url"] =
                std::string(ba.constData(), ba.size());

            saveConfig();

            qDebug() << "Depois de salvar:" << QString::fromStdString(config["FEDERATIONS"]["url"]);
        });
    };
    //menu de adicionar tema
    addThemePage = [&](){
        clearLayout(layout);
        fadeTransition(central);
        loadConfig();
        QLabel *labelTheme = new QLabel(QString::fromStdString(config["THEMES"]["theme"]));;
        layout->addWidget(labelTheme);
        QPushButton *button_add_theme = new QPushButton(add_theme_text);
        layout->addWidget(button_add_theme);
        QPushButton *back_button = new QPushButton(back_text);
        layout->addWidget(back_button);
        QObject::connect(back_button, &QPushButton::clicked, [=](){
                initialPage();
        });
        QObject::connect(button_add_theme, &QPushButton::clicked, [=](){
                singleShot(0, central, [&](){
                    QString filePath = QFileDialog::getOpenFileName(
                        nullptr,
                        "Select a theme",
                        QDir::homePath(),
                        "All the files (*.*);;Texto (*.txt);;Imagens (*.png *.jpg)"
                    );

                    if(filePath.isEmpty())
                    {
                        return; 
                    }

                    QFile file(filePath);

                    if(!file.open(QIODevice::ReadOnly | QIODevice::Text))
                    {
                        QMessageBox::critical(nullptr, "Error", "Cannot open the file!");
                        return;
                    }

                    QTextStream in(&file);
                    QString content = in.readAll();
                    file.close();

                    config["THEMES"]["theme"] = filePath.toStdString();
                    qApp->setStyleSheet(content);
                    saveConfig();
                });
        });
    };
    sentFriendRequest = [&](QString receiver){
        QJsonObject json_friends;
        json_friends.insert("receiver", QJsonValue(receiver));
        json_friends.insert("remittee", QJsonValue(username));
        int status_code = 0;
        requestHTTP(
            url + "/send-friend",
            "POST",
            json_friends,
            10000,
            &status_code
        );
    };
    sentUnFriendRequest = [&](QString receiver){
        QJsonObject json_friends;
        json_friends.insert("friend", QJsonValue(receiver));
        json_friends.insert("username", QJsonValue(username));
        int status_code = 0;
        requestHTTP(
            url + "/unfriend",
            "POST",
            json_friends,
            10000,
            &status_code
        );
    };
    
    otherProfilePage = [&](QString usernameProfile){
        clearLayout(layout);
        QLabel *titleUsername = new QLabel(usernameProfile);
        titleUsername->setStyleSheet("font-size: 16px; font-weight: bold; color: #333333;");
        QJsonObject isFriend;
        isFriend.insert("username", QJsonValue(username));
        QPushButton *sentAFriend = new QPushButton(sent_friend_text);
        QPushButton *unFriend = new QPushButton(un_friend_text);
        QString bio = "";
        QLabel *biography = new QLabel(bio);
        QString response_bio = requestHTTP(
            url + "view_profile/" + usernameProfile,
            "GET",
            QJsonObject()
        );
        QJsonDocument doc_bio =
            QJsonDocument::fromJson(response_bio.toUtf8());
        QJsonObject json_response_bio = doc_bio.object();
        bio = json_response_bio.value("bio").toString();
        QString response = requestHTTP(
            url + "/friends",
            "POST",
            isFriend
        );
        QJsonDocument doc =
            QJsonDocument::fromJson(response.toUtf8());
        QJsonObject json_response = doc.object();
        QJsonArray friendsArray = json_response.value("friends").toArray();
        for (int i = 0; i < friendsArray.size(); ++i) {
            QJsonArray subArray = friendsArray[i].toArray();
            
            
            if (subArray.size() >= 2) {
                QString friendCheck1 = subArray[0].toString();
                QString friendCheck2 = subArray[1].toString();
                if (friendCheck1 == usernameProfile || friendCheck2 == usernameProfile){
                    layout->addWidget(unFriend);
                }else{
                    layout->addWidget(sentAFriend);
                };
            };
        };
        QObject::connect(sentAFriend, &QPushButton::clicked, [=](){
            sentFriendRequest(usernameProfile);
        });
        QObject::connect(unFriend, &QPushButton::clicked, [=](){
            sentUnFriendRequest(usernameProfile);
        });
        QPushButton *back_button = new QPushButton(back_text);
        QObject::connect(back_button, &QPushButton::clicked, [=](){
            initialPage();
        });
        layout->addWidget(titleUsername);
        layout->addWidget(biography);
        layout->addWidget(back_button);    
    };
    changeLangPage = [&](){
        clearLayout(layout);
        QPushButton *pt_br_btn = new QPushButton("pt-br");
        QPushButton *en_btn = new QPushButton("en");
        QObject::connect(pt_br_btn, &QPushButton::clicked, [=](){
            loadConfig();
            config["LANG"]["lang"] = "pt-br";
            saveConfig();
            initialPage();
        });
        QObject::connect(en_btn, &QPushButton::clicked, [=](){
            loadConfig();
            config["LANG"]["lang"] = "en";
            saveConfig();
            initialPage();
        });
        QPushButton *backButton = new QPushButton(back_text);
        QObject::connect(backButton, &QPushButton::clicked, [=](){
            initialPage();
        });
        layout->addWidget(pt_br_btn);
        layout->addWidget(en_btn);
        layout->addWidget(backButton);
    };
    //banned page
    bannedPage = [&](QString reason){
        clearLayout(layout);
        QLabel *banned = new QLabel(banned_text);
        QFont font ("Arial", 32, QFont::Bold);
        banned->setFont(font);
        QLabel *reasonLabel = new QLabel(reason);
        reasonLabel->setFont(font);
        QPushButton *logoutButton = new QPushButton(exit_text);
        layout->addWidget(banned);
        layout->addWidget(reasonLabel);
        layout->addWidget(logoutButton);
    };
    //menu de opções extras
    optionsPage = [&, back_text](){
        QList<QWidget*> buttons;
        clearLayout(layout);
        QPushButton *button_back = new QPushButton(back_text);
        QPushButton *button_add_theme = new QPushButton(add_theme_text);
        QPushButton *button_add_federation = new QPushButton(add_federations_text);
        QPushButton *button_change_lang = new QPushButton(change_lang_page);
        QObject::connect(button_back, &QPushButton::clicked, [=](){
                initialPage();
        });
        QObject::connect(button_add_federation, &QPushButton::clicked, [=](){
                addFederationsPage();
        });
        QObject::connect(button_add_theme, &QPushButton::clicked, [=](){
                addThemePage();
        });
        QObject::connect(button_change_lang, &QPushButton::clicked, [=](){
                changeLangPage();
        });
        
        buttons.append(button_back);
        buttons.append(button_add_theme);
        buttons.append(button_add_federation);
        buttons.append(button_change_lang);
        scroll_area(layout, buttons);
    };
    change_url = [&](){
        clearLayout(layout);
        fadeTransition(central);
        QLineEdit *urlEntry = new QLineEdit();
        urlEntry->setPlaceholderText("url:");
        QPushButton *button_send = new QPushButton(send_text);
        QPushButton *button_back = new QPushButton(back_text);
        layout->addWidget(urlEntry);
        layout->addWidget(button_send);
        layout->addWidget(button_back);
        QObject::connect(button_back, &QPushButton::clicked, [=](){
                singleShot(0, central, [&](){
                    initialPage();
                });
        });
        QObject::connect(button_send, &QPushButton::clicked, [=](){
                config["SERVER"]["url"] = urlEntry->text().toStdString();
                saveConfig();
                initialPage();
        });
    };
    options = [&]()
    {
        clearLayout(layout);
        fadeTransition(central);
        QList<QWidget*> buttons;
        QPushButton *friends = new QPushButton(friends_text);
        QPushButton *back = new QPushButton(back_text);
        QPushButton *inbox = new QPushButton(inbox_text);
        QPushButton *button_options = new QPushButton(options_text);
        QPushButton *button_change_url = new QPushButton(url);
        buttons.append(button_options);
        buttons.append(inbox);
        buttons.append(friends);
        buttons.append(back);
        buttons.append(button_change_url);
        QObject::connect(button_options, &QPushButton::clicked, [=](){
                singleShot(0, central, [=](){ if (optionsPage) optionsPage(); });
        });
        QObject::connect(inbox, &QPushButton::clicked, [=](){
                singleShot(0, central, [=](){ if (inboxPage) inboxPage(); });
        });
        QObject::connect(back, &QPushButton::clicked, [=](){
                singleShot(0, central, [=](){ if (initialPage) initialPage(); });
        });
        QObject::connect(friends, &QPushButton::clicked, [=](){
                singleShot(0, central, [=](){ if (friendsPage) friendsPage(); });
        });
        QObject::connect(button_change_url, &QPushButton::clicked, [=](){
                singleShot(0, central, [=](){ if (change_url) change_url(); });
        });
        scroll_area(layout, buttons);
    };
    sendEdit = [&](QString content){
        QJsonObject edit;
        // Qt 5.1: use insert with QJsonValue(QString) to avoid QJsonValue(const void*) private constructor
        edit.insert("edit-mode", QJsonValue(QString("bio")));
        edit.insert("content", QJsonValue(content));
        edit.insert("username", QJsonValue(username));
        QString edit_response = requestHTTP(
            url + "/edit",
            "POST",
            edit
        );
        initialPage();
    };
    editAccount = [&](){
        clearLayout(layout);
        fadeTransition(central);
        QList<QWidget*> scroll_layout;
        QLineEdit *bioEntry = new QLineEdit();
        bioEntry->setPlaceholderText(bio_text);
        QPushButton *loadPhoto = new QPushButton(profile_picture_text);
        QPushButton *sendButton = new QPushButton(send_text);
        QPushButton *backButton = new QPushButton(back_text);
        scroll_layout.append(bioEntry);
        scroll_layout.append(loadPhoto);
        scroll_layout.append(sendButton);
        scroll_layout.append(backButton);
        QObject::connect(backButton, &QPushButton::clicked, [=](){
            initialPage();
        });
        QObject::connect(sendButton, &QPushButton::clicked, [=](){
            sendEdit(bioEntry->text());
        });
        scroll_area(layout, scroll_layout);

    };
    account = [&](){
        clearLayout(layout);
        fadeTransition(central);
        QLabel *label_username = new QLabel(username);
        layout->addWidget(label_username);
        QJsonObject empty;
        QString bio_response = requestHTTP(
            url + "/view-profile" + "/?username=" + username,
            "GET",
            empty
        );
        QJsonDocument doc =
            QJsonDocument::fromJson(bio_response.toUtf8());

        QJsonObject json_bio = doc.object();
        QString MyBio = json_bio.value("bio").toString();
        QLabel *label_bio = new QLabel(MyBio);
        layout->addWidget(label_bio);
        QPushButton *button_edit = new QPushButton(edit_text);
        layout->addWidget(button_edit);
        QPushButton *buttonBack = new QPushButton(back_text);
        layout->addWidget(buttonBack);
        QObject::connect(buttonBack, &QPushButton::clicked, [=](){
                initialPage();
        });
        QObject::connect(button_edit, &QPushButton::clicked, [=](){
                editAccount();
        });
        
    };
    commentRequest = [&](QString post_id){

        QList<QString> comments;
        QJsonObject comments_json;
        comments_json.insert("post_id", QJsonValue(post_id));
        QString response = requestHTTP(
            url + "/view-comments",
            "POST",
            comments_json
        );

        QJsonDocument doc = QJsonDocument::fromJson(response.toUtf8());

        if(doc.isArray()){
            QJsonArray arr = doc.array();

            for(int i = 0; i < arr.size(); i++){
                comments.append(
                    arr[i].toObject().value("comment").toString()
                );
            }
        }

        return comments;
    };
    newCommentRequest = [&](QString post_id, QString text_comment){
        QJsonObject json_new;
        json_new.insert("post_id", QJsonValue(post_id));
        json_new.insert("username", QJsonValue(QString::fromStdString(config["FAST-LOGIN"]["username"])));
        json_new.insert("text_comment", QJsonValue(text_comment));
        requestHTTP(
            url + "/comments",
            "POST",
            json_new
        );
    };
    newCommentPage = [&](QString post_id){
        clearLayout(layout);
        QLineEdit *commentInput = new QLineEdit();
        commentInput->setPlaceholderText(comments_text);
        QPushButton *sendButton = new QPushButton(send_text);
        QPushButton *backButton = new QPushButton(back_text);
        layout->addWidget(commentInput);
        layout->addWidget(sendButton);
        layout->addWidget(backButton);
        QObject::connect(backButton, &QPushButton::clicked, [=](){
            commentPage(post_id);
        });
        QObject::connect(sendButton, &QPushButton::clicked, [=](){
            newCommentRequest(post_id, commentInput->text());
        });
    };
    fast_login = [&]()
    {
        if (config["FAST-LOGIN"]["token_login"].empty()){
            loginPage();
            return;
        }else{
            int status_code = 0;
            QJsonObject json_fast;
            json_fast.insert("username", QJsonValue(QString::fromStdString(config["FAST-LOGIN"]["username"])));
            json_fast.insert("token", QJsonValue(QString::fromStdString(config["FAST-LOGIN"]["token_login"])));
            requestHTTP(
                QString::fromStdString(config["SERVER"]["url"]) + "/fast-login",
                "POST",
                json_fast,
                5000,
                &status_code
            );
            if (status_code == 200 || status_code == 201){
                initialPage();
                return;
            }else{
                loginPage();
                return;
            };
        };
    };
    showfeed = [&]()
    {
        clearLayout(layout);
        fadeTransition(central);
        QLabel *loading = new QLabel("loading...");
        layout->addWidget(loading);
        QString url_feed = url + "/feed";
        qDebug() << "url feed" << url_feed;
        QNetworkRequest request{QUrl(url_feed)};
        QNetworkReply *reply = manager->get(request);

        QObject::connect(reply, &QNetworkReply::finished, [=]() mutable {


            QByteArray responseData = reply->readAll();
            reply->deleteLater();

            QJsonDocument doc = QJsonDocument::fromJson(responseData);

            if(!doc.isArray())
            {
                QLabel *err = new QLabel("Invalid response from server!");
                layout->addWidget(err);
                return;
            }

            QJsonArray postsArray = doc.array();

            QList<QWidget*> labels;

            for(auto value : postsArray)
            {
                if(!value.isObject()) continue;
                QJsonObject post = value.toObject();

                // Qt5.1: QJsonValue::toInt() might not exist — use toVariant().toInt()
                int postId = post.value("id").toVariant().toInt();
                QString username = post.value("username").toString();
                QString textPost = post.value("text_post").toString();
                QString datetime = post.value("datetime").toString();

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
                QHBoxLayout *usernameLayout = new QHBoxLayout();
                QPushButton *viewProfile = new QPushButton(view_profile);
                QObject::connect(viewProfile, &QPushButton::clicked, [=](){
                    otherProfilePage(username);
                });
                QLabel *lblUser = new QLabel(username);
                QLabel *lblText = new QLabel(textPost);
                QLabel *lblDate = new QLabel(datetime);

                lblUser->setStyleSheet("color: white; font-size: 16px; font-weight: bold;");
                lblText->setStyleSheet("color: white; font-size: 14px;");
                lblDate->setStyleSheet("color: gray; font-size: 12px;");
                lblText->setWordWrap(true); 
                lblText->setSizePolicy(QSizePolicy::Preferred, QSizePolicy::Minimum);
                usernameLayout->addWidget(lblUser);
                usernameLayout->addWidget(viewProfile);
                usernameLayout->addStretch();
                frameLayout->addLayout(usernameLayout);
                frameLayout->addWidget(lblText);
                frameLayout->addWidget(lblDate);

                // ===== BOTÃO STAR =====
                QPushButton *iconButton = new QPushButton();
                QPushButton *commentButton = new QPushButton("comments");
                QLabel *starLabel = new QLabel("...");
                frameLayout->addWidget(commentButton);
                frameLayout->addWidget(iconButton);
                frameLayout->addWidget(starLabel);

                iconButton->setIcon(QIcon(":/assets/default_star.png"));
                iconButton->setIconSize(QSize(24, 24));
                iconButton->setFixedSize(30, 30);
                iconButton->setStyleSheet("border: none;");

                starLabel->setStyleSheet("color: white; font-size: 14px;");
                commentButton->setIconSize(QSize(26, 48));
                commentButton->setStyleSheet("border: none;");


                // ponteiro seguro
                QPointer<QLabel> safeStarLabel = starLabel;

                // buscar quantidade de estrelas
                QNetworkRequest starsReq(QUrl(url + "/return-stars/" + QString::number(postId)));
                QNetworkReply *starsReply = manager->get(starsReq);

                QObject::connect(starsReply, &QNetworkReply::finished, [=]() mutable {

                    if (!safeStarLabel) {
                        starsReply->deleteLater();
                        return;
                    }

                    if(starsReply->error() == QNetworkReply::NoError)
                    {
                        QString starsText = QString(starsReply->readAll()).trimmed();
                        if(starsText.isEmpty()) starsText = "0";
                        safeStarLabel->setText(starsText);
                    }
                    else
                    {
                        safeStarLabel->setText("0");
                    }

                    starsReply->deleteLater();
                });

                // clique da estrela (toggle)
                QObject::connect(iconButton, &QPushButton::clicked, [=]() mutable {
                    QJsonObject star_json;
                    star_json.insert("username", QJsonValue(username));
                    star_json.insert("post_id", QJsonValue(postId));
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
                    bool starred = obj.value("starred").toBool();
                    if (starred == true){
                        iconButton->setIcon(QIcon(":/assets/star.png"));
                    }else{
                        iconButton->setIcon(QIcon(":/assets/default_star.png"));
                    };

                });
                QObject::connect(commentButton,&QPushButton::clicked, [=](){
                    commentPage(QString::number(postId));
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
                initialPage();
            });
            layout->addWidget(btnBack);
            layout->addWidget(btnNewPost);

           

            QObject::connect(btnNewPost, &QPushButton::clicked, [=](){
                new_post();
            });

        });
    };
    auto searchRequest = [&](QString content){
        QJsonObject search;
        search.insert("content", QJsonValue(content));

        QString response = requestHTTP(
            url + "/search",
            "POST",
            search
        );

        return response;
    };
    sendMessage = [&](QString message, QString user){
        QJsonObject chatJson;
        chatJson.insert("receiver", QJsonValue(user));
        chatJson.insert("sender", QJsonValue(username));
        chatJson.insert("message", QJsonValue(message));
        requestHTTP(
            url + "/send-message",
            "POST",
            chatJson
        );
    };
    chatGlobal = [&](){

        clearLayout(layout);

        QScrollArea *scroll = new QScrollArea();
        scroll->setWidgetResizable(true);

        QWidget *containerScroll = new QWidget();
        QVBoxLayout *containerLayout = new QVBoxLayout(containerScroll);

        scroll->setWidget(containerScroll);

        layout->addWidget(scroll);

        int *lastId = new int(0);

        QTimer *timer = new QTimer();

        auto updateChat = [=]() mutable {

            QJsonObject view_chat;
            view_chat.insert("id", QJsonValue(*lastId));
            
            QString chat_message = requestHTTP(
                url + "/view-global-message",
                "POST",
                view_chat
            );

            QJsonDocument doc =
                QJsonDocument::fromJson(
                    chat_message.toUtf8()
                );

            if (!doc.isArray())
                return;

            QJsonArray msgs = doc.array();

            for (int i = 0; i < msgs.size(); i++)
            {
                QJsonObject msg =
                    msgs[i].toObject();

                QString sender =
                    msg.value("sender").toString();

                QString text =
                    msg.value("message").toString();

                int id =
                    msg.value("id").toVariant().toInt();

                if (id > *lastId)
                    *lastId = id;

                bool isMe =
                    (sender == username);

                ChatBubble *bubble =
                    new ChatBubble(
                        sender + ":" + text,
                        isMe
                    );

                QHBoxLayout *line =
                    new QHBoxLayout();

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

                QWidget *lineWidget =
                    new QWidget();

                lineWidget->setLayout(line);

                containerLayout->addWidget(
                    lineWidget
                );
            }

            if (!msgs.isEmpty())
            {
                singleShot(50, central, [=](){

                        scroll->verticalScrollBar()->setValue(
                            scroll->verticalScrollBar()->maximum()
                        );

                    });
            }
        };

        QObject::connect(
            timer,
            &QTimer::timeout,
            updateChat
        );

        updateChat();

        timer->start(2000);

        QLineEdit *message_box = new QLineEdit();
        message_box->setPlaceholderText(type_text);

        QPushButton *send_button =
            new QPushButton(send_text);

        QHBoxLayout *entryBox =
            new QHBoxLayout();

        entryBox->addWidget(message_box);
        entryBox->addWidget(send_button);

        QWidget *container =
            new QWidget();

        container->setLayout(entryBox);

        QPushButton *back_button =
            new QPushButton(back_text);

        QObject::connect(
            back_button,
            &QPushButton::clicked,
            [=]() mutable {

                timer->stop();
                timer->deleteLater();

                delete lastId;

                initialPage();

            }
        );

        QObject::connect(
            send_button,
            &QPushButton::clicked,
            [=]() mutable {

                QString text =
                    message_box->text();

                if (text.isEmpty())
                    return;

                QJsonObject json;

                json.insert("sender", QJsonValue(username));
                json.insert("message", QJsonValue(text));

                requestHTTP(
                    url + "/send-global-message",
                    "POST",
                    json
                );

                message_box->clear();
            }
        );

        QObject::connect(
            message_box,
            &QLineEdit::returnPressed,
            [=]() mutable {

                send_button->click();

            }
        );

        layout->addWidget(container);
        layout->addWidget(back_button);
    };
    //tela quando você esta conversando com o usuario
    chat = [&](QString user){
        clearLayout(layout);
        QList<QWidget*> message;
        // QHBoxLayout *lineMessage;


        QScrollArea *scroll = new QScrollArea();
        scroll->setWidgetResizable(true);

        QWidget *containerScroll = new QWidget();
        QVBoxLayout *containerLayout = new QVBoxLayout(containerScroll);

        scroll->setWidget(containerScroll);
        layout->addWidget(scroll);


        QTimer *timer = new QTimer();

        
        int *currentMessageCount = new int(0); 

        QObject::connect(timer, &QTimer::timeout, [=]() mutable {
            QJsonObject view_chat;
            view_chat.insert("user1", QJsonValue(username));
            view_chat.insert("user2", QJsonValue(user));

            QString chat_message = requestHTTP(
                url + "/view",
                "POST",
                view_chat
            );

            QJsonDocument doc = QJsonDocument::fromJson(chat_message.toUtf8());
            if (!doc.isObject()) return;

            QJsonObject obj = doc.object();
            if (!obj.contains("messages")) return;

            QJsonArray msgs = obj.value("messages").toArray();

            
            if (msgs.size() != *currentMessageCount)
            {
                int oldSize = *currentMessageCount;
                *currentMessageCount = msgs.size(); 

                
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

                    QString sender = msg.value("sender").toString();
                    QString text = msg.value("message").toString();

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

                
                bool lastMsgIsMe = false;
                if (msgs.size() > 0) {
                    lastMsgIsMe = (msgs[msgs.size() - 1].toObject().value("sender").toString() == username);
                }

                if (oldSize == 0 || lastMsgIsMe)
                {
                    singleShot(50, central, [=](){
                        scroll->verticalScrollBar()->setValue(scroll->verticalScrollBar()->maximum());
                    });
                }
            }
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
            singleShot(0, central, [=](){
                timer->stop();
                delete currentMessageCount; // Deleta o ponteiro para não dar Memory Leak
                initialPage();
            });
        });

        QObject::connect(send_button, &QPushButton::clicked, [=]() mutable{
            singleShot(0, central, [=](){
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
        fadeTransition(central);
        QList<QWidget*> widgets;
        QPushButton *ChatGlobalButton = new QPushButton("Chat Global");
        QObject::connect(ChatGlobalButton, &QPushButton::clicked, [=]() mutable{
            chatGlobal();
        });
        
        QJsonObject friends_json;
        friends_json.insert("username", QJsonValue(username));
        QString response_friends = requestHTTP(
            url + "/friends",
            "POST",
            friends_json
        );
        QJsonDocument doc = QJsonDocument::fromJson(response_friends.toUtf8());
        QJsonObject obj = doc.object();
        QJsonArray friends = obj.value("friends").toArray();
        if (friends.isEmpty())
        {
            QLabel *label_error = new QLabel("No Friends....");
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
                    singleShot(0, central, [=](){
                        chat(friendName);
                    });
                });
                widgets.append(user);
            };
        };
        QPushButton *back_button = new QPushButton(back_text);
        QObject::connect(back_button, &QPushButton::clicked, [=]() mutable{
            singleShot(0, central, [=](){
                initialPage();
            });
        });
        widgets.append(ChatGlobalButton);
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
            singleShot(0, central, [=](){
                initialPage();
            });
        });

        QObject::connect(buttonSearch, &QPushButton::clicked, [=]() mutable {
            singleShot(0, central, [=]() mutable {

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
                if (obj.contains("usernames") && obj.value("usernames").isArray())
                {
                    QJsonArray arr = obj.value("usernames").toArray();

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
                if (obj.contains("posts") && obj.value("posts").isArray())
                {
                    QJsonArray arr = obj.value("posts").toArray();

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
        if (config["FAST-LOGIN"]["token_session"].empty()){
            loginPage();
        };
        if (config["FAST-LOGIN"]["username"].empty() || config["FAST-LOGIN"]["password"].empty()){
            loginPage();
            return;
        };
        QString token = QString::fromStdString(config["FAST-LOGIN"]["token_session"]);
        QJsonObject valideToken;
        valideToken.insert("username", QJsonValue(QString::fromStdString(config["FAST-LOGIN"]["username"])));
        int status_code = 0;
        requestHTTP(
            url + "/valide-session",
            "POST",
            valideToken,
            10000,
            &status_code
        );
        if (status_code == 200 || status_code == 201){}else{
            QString token = newSession(QString::fromStdString(config["FAST-LOGIN"]["username"]), QString::fromStdString(config["FAST-LOGIN"]["password"]));
            config["FAST-LOGIN"]["token_session"] = token.toStdString();
            saveConfig();
        };
        clearLayout(layout);
        fadeTransition(central);
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
        stack->addWidget(pageChat);    // index 0
        stack->addWidget(pageOptions); // index 1
        stack->addWidget(pageHome);    // index 2
        stack->addWidget(pageSearch);  // index 3
        stack->addWidget(pageProfile); // index 4
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
        QObject::connect(btnOptions, &QPushButton::clicked, [options]() {
            if (options) options();
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
            stack->setCurrentIndex(2);
        });
        QObject::connect(btnSearch, &QPushButton::clicked, [=](){
            stack->setCurrentIndex(3);
        });
        QObject::connect(btnChat, &QPushButton::clicked, [=](){
            stack->setCurrentIndex(0);
        });

        QObject::connect(btnProfile, &QPushButton::clicked, [=](){
            stack->setCurrentIndex(4);
        });
        QObject::connect(btnOptions, &QPushButton::clicked, [=](){
            stack->setCurrentIndex(1);
        });
        
        // ======= ESTILO =======
        bottomBar->setStyleSheet("background: #111;");
        btnOptions->setStyleSheet("font-size: 32px; border: none; color: white; background: transparent;");
        btnHome->setStyleSheet("font-size: 32px; border: none; color: #00ffea; background: transparent;");
        btnChat->setStyleSheet("font-size: 32px; border: none; color: white; background: transparent;");
        btnProfile->setStyleSheet("font-size: 32px; border: none; color: white; background: transparent;");
        btnSearch->setStyleSheet("font-size: 32px; border: none; color: white; background: transparent;");

        // ======= MONTAGEM =======
        if (!layout) {
            qDebug() << "ERRO FATAL: O layout principal veio nulo!";
            return;
        }
        layout->addWidget(stack, 1);
        layout->addWidget(bottomBar, 0);
    };
    signinRequest = [&](QString username, QString password, QString email){
        QJsonObject json_signin;
        json_signin.insert("username", QJsonValue(username));
        json_signin.insert("password", QJsonValue(password));
        json_signin.insert("email", QJsonValue(email));
        int status_code = 0;
        QString request_signin = requestHTTP(
            url + "/register",
            "POST",
            json_signin,
            10000,
            &status_code
        );
        QString token = newSession(username, password);
        config["FAST-LOGIN"]["token_session"] = token.toStdString();
        saveConfig();
        return status_code;
    };  
    signinPage = [&](){
        clearLayout(layout);
        fadeTransition(central);
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
            if (passwordEntry->text() == retryPasswordEntry->text()) {
                int status_code = signinRequest(usernameEntry->text(), passwordEntry->text(), emailEntry->text());
                if (status_code == 200 || status_code == 201){
                    loadConfig();
                    config["FAST-LOGIN"]["username"] = usernameEntry->text().toStdString();
                    config["FAST-LOGIN"]["password"] = passwordEntry->text().toStdString();
                    saveConfig();
                    initialPage();
                }else{
                    QString msgErro = QString("ERROR! (Status: %1)").arg(status_code);
                    QLabel *error_label = new QLabel(msgErro);
                    layout->addWidget(error_label);
                };
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
        int status_code = 0;
        QJsonObject json_signup;
        json_signup.insert("username", QJsonValue(username));
        json_signup.insert("password", QJsonValue(password));
        QString response_signup = requestHTTP(
            url + "/login",
            "POST",
            json_signup,
            10000,
            &status_code
        );
        QString token = newSession(username, password);
        loadConfig();
        config["FAST-LOGIN"]["token_session"] = token.toStdString();
        saveConfig();
        return status_code;
    };
    signupPage = [&](){
        clearLayout(layout);
        fadeTransition(central);
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
        QObject::connect(send_button, &QPushButton::clicked, [=]() mutable {
            QString userTxt = usernameEntry->text();
            QString passTxt = passwordEntry->text();

            QString token_gerado = newSession(userTxt, passTxt);
            if (!token_gerado.isEmpty()){
                loadConfig();
                config["FAST-LOGIN"]["username"] = userTxt.toStdString();
                config["FAST-LOGIN"]["password"] = passTxt.toStdString();
                config["FAST-LOGIN"]["token_session"] = token_gerado.toStdString();
                saveConfig();
                initialPage();
            } else {
                QLabel *error_label = new QLabel("Usuário ou senha incorretos!");
                layout->addWidget(error_label);
            };
        });
    };
    changeServerPage = [&](){
        clearLayout(layout);
        fadeTransition(central);
        QLineEdit *urlEntry = new QLineEdit();
        urlEntry->setPlaceholderText("url");
        QPushButton *ok_button = new QPushButton("ok");
        QPushButton *back_button = new QPushButton(back_text);
        QObject::connect(back_button, &QPushButton::clicked, [=](){
            loginPage();
        });
        layout->addWidget(urlEntry);
        layout->addWidget(ok_button);
        layout->addWidget(back_button);
    };
    
    addFriendsRequest = [&](QString receiver, QString message){
        QJsonObject friend_json;
        friend_json.insert("receiver", QJsonValue(receiver));
        friend_json.insert("remittee", QJsonValue(username));
        friend_json.insert("message", QJsonValue(message));
        requestHTTP(
            url + "/send-friend",
            "POST",
            friend_json
        );
    };
    addFriendsPage = [=, &initialPage, &addFriendsPage]{
        clearLayout(layout);
        fadeTransition(central);
        QLineEdit *usernameEntry = entry(username_text);
        layout->addWidget(usernameEntry);
        QLineEdit *messageEntry = entry(message_text);
        layout->addWidget(messageEntry);
        QPushButton *back_button = new QPushButton(back_text);
        layout->addWidget(back_button);
        QPushButton *send_button = new QPushButton(send_text);
        layout->addWidget(send_button);
        QObject::connect(send_button, &QPushButton::clicked, [=](){
                addFriendsRequest(usernameEntry->text(), messageEntry->text());
                initialPage();
            
        });
        QObject::connect(back_button, &QPushButton::clicked, [=](){
                initialPage();
        });
    };
    //chamada da função
    initialPage();
    //função para exibir o feed
    window.show();
    return app.exec();
}