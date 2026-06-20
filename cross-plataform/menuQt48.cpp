// menu.cpp — adaptado para Qt 4.8 (arquivo completo)
#include <QFileDialog>
#include <QTextStream>
#include <QMessageBox>
#include <QAction>
#include <QPointer>
#include <QApplication>
#include <QWidget>
#include <QPushButton>
#include <QVBoxLayout>
#include <QHBoxLayout>
#include <QLineEdit>
#include <QDebug>
#include <QNetworkAccessManager>
#include <QNetworkRequest>
#include <QNetworkReply>
#include <QStyleFactory>
#include <QDesktopServices>
#include <QUrl>
#include <QVector>
#include <QSplashScreen>
#include <QMenu>
#include <iostream>
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
#include <QScrollArea>
#include <QTimer>
#include <QFrame>
#include <QIcon>
#include <QDateTime>
#include <QSize>
#include <QPainter>
#include <QFontMetrics>
#include <QTranslator>
#include <QDesktopWidget>
#include <QSizePolicy>
#include <QCoreApplication>
#include <QScriptEngine>
#include <QScriptValue>
#include <QScriptValueIterator>
#include <QVariant>
#include <QScriptEngine>
#include <QScriptValue>
#include <QVariant>
#include <QMap>
#include <QList>
#include <QString>
#include <QByteArray>

typedef QMap<QString, QVariant> QJsonObject;
typedef QList<QVariant> QJsonArray;
typedef QVariant QJsonValue;

class QJsonDocument {
public:
    enum JsonFormat { Indented, Compact };
    QJsonDocument() {}
    QJsonDocument(const QJsonObject &map) : m_variant(map) {}
    QJsonDocument(const QJsonArray &list) : m_variant(list) {}

    static QJsonDocument fromJson(const QByteArray &json) {
        QJsonDocument doc;
        QScriptEngine engine;
        QScriptValue val = engine.evaluate("(" + QString::fromUtf8(json) + ")");
        doc.m_variant = val.toVariant();
        return doc;
    }

    bool isObject() const { return m_variant.type() == QVariant::Map; }
    bool isArray() const { return m_variant.type() == QVariant::List; }
    QJsonObject object() const { return m_variant.toMap(); }
    QJsonArray array() const { return m_variant.toList(); }
    QByteArray toJson(JsonFormat format = Compact) const { Q_UNUSED(format); return m_variant.toString().toUtf8(); }
private:
    QVariant m_variant;
};

// Apenas estas duas macros são necessárias e seguras para o resto do código:
#define toObject() toMap()
#define toArray() toList()
// REMOVEMOS a macro "toVariant" daqui para sumir com o erro de redefinição!
// ===== HELPER PARA CALLBACKS EM QT 4.8 =====
class TimerCallback : public QObject
{
    Q_OBJECT
public:
    typedef void (*CallbackFunc)();
    
    TimerCallback(int msec, QObject *parent, CallbackFunc func)
        : QObject(parent), m_func(func)
    {
        QTimer::singleShot(msec, this, SLOT(execute()));
    }
    
private slots:
    void execute() {
        if (m_func) {
            try { m_func(); } catch (...) {}
        }
        deleteLater();
    }
    
private:
    CallbackFunc m_func;
};

// Helper singleShot com suporte a functores simples
void singleShotCallback(int msec, QObject *parent, TimerCallback::CallbackFunc func)
{
    new TimerCallback(msec, parent, func);
}

class ChatBubble : public QWidget {
public:
    ChatBubble(QString text, bool isMe, QWidget *parent = 0)
        : QWidget(parent), message(text), mine(isMe)
    {
        setMaximumWidth(400);
        setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Fixed);
    }

protected:
    void paintEvent(QPaintEvent * /*event*/) {
        QPainter painter(this);
        painter.setRenderHint(QPainter::Antialiasing);

        QRect bubbleRect = rect().adjusted(10, 5, -10, -5);

        QColor bubbleColor;
        if (mine)
            bubbleColor = QColor(0, 200, 120);
        else
            bubbleColor = QColor(60, 60, 60);

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

    QSize sizeHint() const {
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

std::map<std::string, std::map<std::string, std::string> > config;

QString configPath() {
    QString dirPath = QDir::homePath() + "/.linka";
    QDir().mkpath(dirPath);
    return dirPath + "/config-login.cfg";
}

void loadConfig()
{
    QString path = configPath();
    qDebug() << "Caminho config:" << path;

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

    QString dirPath = QDir::homePath() + "/.linka";
    QDir().mkpath(dirPath);

    QString filePath = dirPath + "/config-login.cfg";

    qDebug() << "Salvando config em:" << filePath;

    std::ofstream file(filePath.toStdString().c_str());

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
    int *statusCode = 0
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

    request.setRawHeader(
        "Content-Type",
        "application/json"
    );

    QByteArray data =
        QJsonDocument(j).toJson();

    QNetworkReply *reply =
        manager.post(request, data);

    QEventLoop loop;

    QObject::connect(
        reply,
        SIGNAL(finished()),
        &loop,
        SLOT(quit())
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
    
    QNetworkAccessManager isolatorManager;
    QNetworkRequest request;
    request.setUrl(QUrl(url + "/new-session"));
    request.setRawHeader("Content-Type", "application/json");

    QJsonObject newTokenJson;
    newTokenJson["username"] = QString::fromStdString(config["FAST-LOGIN"]["username"]);
    newTokenJson["password"] = QString::fromStdString(config["FAST-LOGIN"]["password"]);
    
    QJsonDocument doc(newTokenJson);
    QString s = doc.toJson(QJsonDocument::Compact);
    std::string s_std = s.toStdString();
    QByteArray data = s.toUtf8();
    
    QNetworkReply *reply = isolatorManager.post(request, data);

    QEventLoop loop;
    QObject::connect(reply, SIGNAL(finished()), &loop, SLOT(quit()));
    loop.exec();

    QString response = reply->readAll();
    int code = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
    reply->deleteLater();

    if (code != 200 && code != 201) {
        qDebug() << "Falha critica ao renovar token. Status:" << code;
        return "";
    }

    try {
        QJsonDocument doc = QJsonDocument::fromJson(response.toUtf8());
        QJsonObject jsonObj = doc.object();
        QString newToken = jsonObj.value("token").toString();
        
        if (!newToken.isEmpty()) {
            config["FAST-LOGIN"]["token_session"] = newToken.toStdString();
            saveConfig();
            qDebug() << "Token renovado com sucesso via bypass!";
            return newToken;
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

    if (m != "GET") {
        request.setRawHeader(
            "Content-Type",
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
    }
    
    QNetworkReply *reply = 0;
    QByteArray jsonData = QJsonDocument(json).toJson();

    if (m == "GET") {
        reply = manager.get(request);
    } else if (m == "POST") {
        reply = manager.post(request, jsonData);
    } else if (m == "PUT") {
        reply = manager.put(request, jsonData);
    } else if (m == "DELETE") {
        reply = manager.deleteResource(request);
    } else {
        if (statusCode) *statusCode = -1;
        return "ERROR: invalid method";
    }
    
    QEventLoop loop;
    QTimer timer;
    timer.setSingleShot(true);
    
    QObject::connect(&timer, SIGNAL(timeout()), &loop, SLOT(quit()));
    QObject::connect(reply, SIGNAL(finished()), &loop, SLOT(quit()));
    
    timer.start(timeoutMs);
    loop.exec();
    
    if (!timer.isActive()) {
        reply->abort();
        if (statusCode) *statusCode = 408;
        reply->deleteLater();
        return "ERRO: Timeout";
    }
    
    int code = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
    if (statusCode) *statusCode = code;
    
    if (code == 401){
        renoveToken();
    }
    
    QString response = reply->readAll();
    reply->deleteLater();
    
    return response;
}

void scroll_area(QVBoxLayout *layout, const QList<QWidget*> &widgets)
{
    QScrollArea *scroll = new QScrollArea();
    scroll->setWidgetResizable(true);
    scroll->setFrameShape(QFrame::NoFrame); 
    scroll->setVerticalScrollBarPolicy(Qt::ScrollBarAsNeeded);
    scroll->setHorizontalScrollBarPolicy(Qt::ScrollBarAsNeeded);
    
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

// ===== CLASSE PRINCIPAL PARA GERENCIAR PÁGINAS EM QT 4.8 =====
class AppManager : public QObject
{
    Q_OBJECT
    
public:
    AppManager(QMainWindow *w, QWidget *c, QVBoxLayout *l, QNetworkAccessManager *m)
        : window(w), central(c), layout(l), manager(m), 
          currentPage(0), lastId(0), currentMessageCount(0), timerChat(0)
    {
        loadConfig();
        url = QString::fromStdString(config["SERVER"]["url"]);
        username = QString::fromStdString(config["FAST-LOGIN"]["username"]);
    }
    
    QMainWindow *window;
    QWidget *central;
    QVBoxLayout *layout;
    QNetworkAccessManager *manager;
    QString url;
    QString username;
    int currentPage;
    int lastId;
    int currentMessageCount;
    QTimer *timerChat;
    QString currentChatUser;
    QScrollArea *chatScroll;
    QWidget *chatContainer;
    QVBoxLayout *chatLayout;
    
public slots:
    void showLoginPage() {
        clearLayout(layout);
        
        QPushButton *signinBtn = new QPushButton("Sign In");
        QPushButton *signupBtn = new QPushButton("Sign Up");
        QPushButton *changeServerBtn = new QPushButton("Change Server");
        
        layout->addWidget(signinBtn);
        layout->addWidget(signupBtn);
        layout->addWidget(changeServerBtn);
        
        QObject::connect(signinBtn, SIGNAL(clicked()), this, SLOT(showSigninPage()));
        QObject::connect(signupBtn, SIGNAL(clicked()), this, SLOT(showSignupPage()));
        QObject::connect(changeServerBtn, SIGNAL(clicked()), this, SLOT(showChangeServerPage()));
    }
    
    void showSigninPage() {
        clearLayout(layout);
        
        QLineEdit *usernameEntry = new QLineEdit();
        QLineEdit *passwordEntry = new QLineEdit();
        QLineEdit *emailEntry = new QLineEdit();
        
        usernameEntry->setPlaceholderText("Username");
        passwordEntry->setPlaceholderText("Password");
        passwordEntry->setEchoMode(QLineEdit::Password);
        emailEntry->setPlaceholderText("Email");
        
        QPushButton *sendBtn = new QPushButton("Sign In");
        QPushButton *backBtn = new QPushButton("Back");
        
        layout->addWidget(usernameEntry);
        layout->addWidget(passwordEntry);
        layout->addWidget(emailEntry);
        layout->addWidget(sendBtn);
        layout->addWidget(backBtn);
        
        QObject::connect(backBtn, SIGNAL(clicked()), this, SLOT(showLoginPage()));
        QObject::connect(sendBtn, SIGNAL(clicked()), this, SLOT(onSigninClicked()));
    }
    
    void showSignupPage() {
        clearLayout(layout);
        
        QLineEdit *usernameEntry = new QLineEdit();
        QLineEdit *passwordEntry = new QLineEdit();
        
        usernameEntry->setPlaceholderText("Username");
        passwordEntry->setPlaceholderText("Password");
        passwordEntry->setEchoMode(QLineEdit::Password);
        
        QPushButton *sendBtn = new QPushButton("Sign Up");
        QPushButton *backBtn = new QPushButton("Back");
        
        layout->addWidget(usernameEntry);
        layout->addWidget(passwordEntry);
        layout->addWidget(sendBtn);
        layout->addWidget(backBtn);
        
        QObject::connect(backBtn, SIGNAL(clicked()), this, SLOT(showLoginPage()));
        QObject::connect(sendBtn, SIGNAL(clicked()), this, SLOT(onSignupClicked()));
    }
    
    void showChangeServerPage() {
        clearLayout(layout);
        
        QLineEdit *urlEntry = new QLineEdit();
        urlEntry->setPlaceholderText("Server URL");
        
        QPushButton *okBtn = new QPushButton("OK");
        QPushButton *backBtn = new QPushButton("Back");
        
        layout->addWidget(urlEntry);
        layout->addWidget(okBtn);
        layout->addWidget(backBtn);
        
        QObject::connect(backBtn, SIGNAL(clicked()), this, SLOT(showLoginPage()));
        QObject::connect(okBtn, SIGNAL(clicked()), this, SLOT(onServerUrlChanged()));
    }
    
    void showInitialPage() {
        clearLayout(layout);
        
        QLabel *homeLabel = new QLabel("HOME - Initial Page");
        QPushButton *feedBtn = new QPushButton("Feed");
        QPushButton *friendsBtn = new QPushButton("Friends");
        QPushButton *inboxBtn = new QPushButton("Inbox");
        QPushButton *chatBtn = new QPushButton("Chat");
        QPushButton *optionsBtn = new QPushButton("Options");
        QPushButton *profileBtn = new QPushButton("Profile");
        QPushButton *logoutBtn = new QPushButton("Logout");
        
        layout->addWidget(homeLabel);
        layout->addWidget(feedBtn);
        layout->addWidget(friendsBtn);
        layout->addWidget(inboxBtn);
        layout->addWidget(chatBtn);
        layout->addWidget(optionsBtn);
        layout->addWidget(profileBtn);
        layout->addWidget(logoutBtn);
        
        QObject::connect(feedBtn, SIGNAL(clicked()), this, SLOT(showFeed()));
        QObject::connect(friendsBtn, SIGNAL(clicked()), this, SLOT(showFriendsPage()));
        QObject::connect(inboxBtn, SIGNAL(clicked()), this, SLOT(showInboxPage()));
        QObject::connect(chatBtn, SIGNAL(clicked()), this, SLOT(showChatPage()));
        QObject::connect(optionsBtn, SIGNAL(clicked()), this, SLOT(showOptionsPage()));
        QObject::connect(profileBtn, SIGNAL(clicked()), this, SLOT(showProfilePage()));
        QObject::connect(logoutBtn, SIGNAL(clicked()), this, SLOT(showLoginPage()));
    }
    
    void showFeed() {
        clearLayout(layout);
        
        QLabel *loadingLabel = new QLabel("Loading feed...");
        layout->addWidget(loadingLabel);
        
        QString response = requestHTTP(url + "/feed", "GET", QJsonObject());
        QJsonDocument doc = QJsonDocument::fromJson(response.toUtf8());
        
        if (doc.isArray()) {
            QJsonArray postsArray = doc.array();
            QList<QWidget*> postWidgets;
            
            for (int i = 0; i < postsArray.size(); i++) {
                QJsonObject post = postsArray[i].toObject();
                
                QString postUsername = post.value("username").toString();
                QString postText = post.value("text_post").toString();
                QString postDatetime = post.value("datetime").toString();
                
                QFrame *frame = new QFrame();
                frame->setStyleSheet("background-color: #1A1A1A; border: 1px solid #2F2F2F; border-radius: 14px; padding: 10px;");
                
                QVBoxLayout *frameLayout = new QVBoxLayout(frame);
                QLabel *userLabel = new QLabel(postUsername);
                QLabel *textLabel = new QLabel(postText);
                QLabel *dateLabel = new QLabel(postDatetime);
                
                userLabel->setStyleSheet("color: white; font-weight: bold;");
                textLabel->setStyleSheet("color: white;");
                textLabel->setWordWrap(true);
                dateLabel->setStyleSheet("color: gray;");
                
                frameLayout->addWidget(userLabel);
                frameLayout->addWidget(textLabel);
                frameLayout->addWidget(dateLabel);
                
                postWidgets.append(frame);
            }
            
            clearLayout(layout);
            scroll_area(layout, postWidgets);
        }
        
        QPushButton *backBtn = new QPushButton("Back");
        layout->addWidget(backBtn);
        QObject::connect(backBtn, SIGNAL(clicked()), this, SLOT(showInitialPage()));
    }
    
    void showFriendsPage() {
        clearLayout(layout);
        
        QLabel *titleLabel = new QLabel("Friends");
        layout->addWidget(titleLabel);
        
        QPushButton *addFriendsBtn = new QPushButton("Add Friend");
        QPushButton *backBtn = new QPushButton("Back");
        
        layout->addWidget(addFriendsBtn);
        layout->addWidget(backBtn);
        
        QObject::connect(addFriendsBtn, SIGNAL(clicked()), this, SLOT(showAddFriendsPage()));
        QObject::connect(backBtn, SIGNAL(clicked()), this, SLOT(showInitialPage()));
    }
    
    void showAddFriendsPage() {
        clearLayout(layout);
        
        QLineEdit *usernameEntry = new QLineEdit();
        QLineEdit *messageEntry = new QLineEdit();
        
        usernameEntry->setPlaceholderText("Username");
        messageEntry->setPlaceholderText("Message");
        
        QPushButton *sendBtn = new QPushButton("Send");
        QPushButton *backBtn = new QPushButton("Back");
        
        layout->addWidget(usernameEntry);
        layout->addWidget(messageEntry);
        layout->addWidget(sendBtn);
        layout->addWidget(backBtn);
        
        QObject::connect(backBtn, SIGNAL(clicked()), this, SLOT(showFriendsPage()));
        QObject::connect(sendBtn, SIGNAL(clicked()), this, SLOT(onAddFriendClicked()));
    }
    
    void showInboxPage() {
        clearLayout(layout);
        
        QLabel *titleLabel = new QLabel("Inbox");
        layout->addWidget(titleLabel);
        
        QJsonObject inboxJson;
        inboxJson.insert("username", QJsonValue(username));
        
        QString response = requestHTTP(url + "/inbox", "POST", inboxJson);
        QJsonDocument doc = QJsonDocument::fromJson(response.toUtf8());
        QJsonObject obj = doc.object();
        QJsonArray inboxArray = obj.value("inbox").toArray();
        
        QList<QWidget*> widgets;
        
        for (int i = 0; i < inboxArray.size(); i++) {
            QJsonArray item = inboxArray[i].toArray();
            
            for (int j = 0; j < item.size(); j++) {
                QLabel *notifLabel = new QLabel(item[j].toString());
                widgets.append(notifLabel);
            }
            
            QPushButton *acceptBtn = new QPushButton("Accept");
            QPushButton *denyBtn = new QPushButton("Deny");
            
            widgets.append(acceptBtn);
            widgets.append(denyBtn);
        }
        
        scroll_area(layout, widgets);
        
        QPushButton *backBtn = new QPushButton("Back");
        layout->addWidget(backBtn);
        QObject::connect(backBtn, SIGNAL(clicked()), this, SLOT(showInitialPage()));
    }
    
    void showChatPage() {
        clearLayout(layout);
        
        QLabel *titleLabel = new QLabel("Chats");
        layout->addWidget(titleLabel);
        
        QJsonObject friendsJson;
        friendsJson.insert("username", QJsonValue(username));
        
        QString response = requestHTTP(url + "/friends", "POST", friendsJson);
        QJsonDocument doc = QJsonDocument::fromJson(response.toUtf8());
        QJsonObject obj = doc.object();
        QJsonArray friendsArray = obj.value("friends").toArray();
        
        QList<QWidget*> widgets;
        
        for (int i = 0; i < friendsArray.size(); i++) {
            QJsonArray row = friendsArray[i].toArray();
            QString friendName = (row[0].toString() == username) ? row[1].toString() : row[0].toString();
            
            QPushButton *friendBtn = new QPushButton(friendName);
            widgets.append(friendBtn);
        }
        
        scroll_area(layout, widgets);
        
        QPushButton *globalChatBtn = new QPushButton("Global Chat");
        QPushButton *backBtn = new QPushButton("Back");
        
        layout->addWidget(globalChatBtn);
        layout->addWidget(backBtn);
        
        QObject::connect(backBtn, SIGNAL(clicked()), this, SLOT(showInitialPage()));
        QObject::connect(globalChatBtn, SIGNAL(clicked()), this, SLOT(showGlobalChat()));
    }
    
    void showGlobalChat() {
        clearLayout(layout);
        
        chatScroll = new QScrollArea();
        chatScroll->setWidgetResizable(true);
        
        chatContainer = new QWidget();
        chatLayout = new QVBoxLayout(chatContainer);
        chatScroll->setWidget(chatContainer);
        
        layout->addWidget(chatScroll);
        
        QLineEdit *messageBox = new QLineEdit();
        messageBox->setPlaceholderText("Type message...");
        
        QPushButton *sendBtn = new QPushButton("Send");
        QPushButton *backBtn = new QPushButton("Back");
        
        QHBoxLayout *inputLayout = new QHBoxLayout();
        inputLayout->addWidget(messageBox);
        inputLayout->addWidget(sendBtn);
        
        QWidget *inputContainer = new QWidget();
        inputContainer->setLayout(inputLayout);
        
        layout->addWidget(inputContainer);
        layout->addWidget(backBtn);
        
        timerChat = new QTimer();
        QObject::connect(timerChat, SIGNAL(timeout()), this, SLOT(updateGlobalChat()));
        timerChat->start(2000);
        
        updateGlobalChat();
        
        QObject::connect(backBtn, SIGNAL(clicked()), this, SLOT(onChatBackClicked()));
        QObject::connect(sendBtn, SIGNAL(clicked()), this, SLOT(onGlobalChatSend()));
    }
    
    void AppManager::updateGlobalChat() {
        QJsonObject viewChat;
        viewChat.insert("id", QJsonValue(lastId));
        
        QString response = requestHTTP(url + "/view-global-message", "POST", viewChat);
        QJsonDocument doc = QJsonDocument::fromJson(response.toUtf8());
        
        if (doc.isArray()) {
            QJsonArray messages = doc.array();
            
            // Exemplo de como rodar o laço com segurança no Qt 4:
            for (int i = 0; i < messages.size(); ++i) {
                // Em vez de usar .toVariant(), pegamos o item direto como QVariant
                QVariant msgItem = messages.at(i);
            };
            for (int i = 0; i < messages.size(); i++) {
                QJsonObject msg = messages[i].toObject();
                QString sender = msg.value("sender").toString();
                QString text = msg.value("message").toString();
                int id = msg.value("id").toInt();
                
                if (id > lastId) lastId = id;
                
                bool isMe = (sender == username);
                ChatBubble *bubble = new ChatBubble(sender + ": " + text, isMe);
                
                QHBoxLayout *line = new QHBoxLayout();
                if (isMe) {
                    line->addStretch();
                    line->addWidget(bubble);
                } else {
                    line->addWidget(bubble);
                    line->addStretch();
                }
                
                QWidget *lineWidget = new QWidget();
                lineWidget->setLayout(line);
                chatLayout->addWidget(lineWidget);
            }
            
            if (!messages.isEmpty()) {
                chatScroll->verticalScrollBar()->setValue(chatScroll->verticalScrollBar()->maximum());
            }
        }
    }
    
    void onGlobalChatSend() {
        // Enviar mensagem global
    }
    
    void onChatBackClicked() {
        if (timerChat) {
            timerChat->stop();
            timerChat->deleteLater();
            timerChat = 0;
        }
        lastId = 0;
        showInitialPage();
    }
    
    void showProfilePage() {
        clearLayout(layout);
        
        QLabel *usernameLabel = new QLabel("Username: " + username);
        layout->addWidget(usernameLabel);
        
        QString response = requestHTTP(url + "/view-profile/?username=" + username, "GET", QJsonObject());
        QJsonDocument doc = QJsonDocument::fromJson(response.toUtf8());
        QJsonObject obj = doc.object();
        QString bio = obj.value("bio").toString();
        
        QLabel *bioLabel = new QLabel("Bio: " + bio);
        layout->addWidget(bioLabel);
        
        QPushButton *editBtn = new QPushButton("Edit");
        QPushButton *backBtn = new QPushButton("Back");
        
        layout->addWidget(editBtn);
        layout->addWidget(backBtn);
        
        QObject::connect(backBtn, SIGNAL(clicked()), this, SLOT(showInitialPage()));
        QObject::connect(editBtn, SIGNAL(clicked()), this, SLOT(showEditProfilePage()));
    }
    
    void showEditProfilePage() {
        clearLayout(layout);
        
        QLineEdit *bioEntry = new QLineEdit();
        bioEntry->setPlaceholderText("Biography");
        
        QPushButton *saveBtn = new QPushButton("Save");
        QPushButton *backBtn = new QPushButton("Back");
        
        layout->addWidget(bioEntry);
        layout->addWidget(saveBtn);
        layout->addWidget(backBtn);
        
        QObject::connect(backBtn, SIGNAL(clicked()), this, SLOT(showProfilePage()));
        QObject::connect(saveBtn, SIGNAL(clicked()), this, SLOT(onProfileSaveClicked()));
    }
    
    void showOptionsPage() {
        clearLayout(layout);
        
        QLabel *titleLabel = new QLabel("Options");
        layout->addWidget(titleLabel);
        
        QPushButton *themeBtn = new QPushButton("Change Theme");
        QPushButton *langBtn = new QPushButton("Change Language");
        QPushButton *federationBtn = new QPushButton("Add Federation");
        QPushButton *backBtn = new QPushButton("Back");
        
        layout->addWidget(themeBtn);
        layout->addWidget(langBtn);
        layout->addWidget(federationBtn);
        layout->addWidget(backBtn);
        
        QObject::connect(backBtn, SIGNAL(clicked()), this, SLOT(showInitialPage()));
        QObject::connect(themeBtn, SIGNAL(clicked()), this, SLOT(showThemeDialog()));
        QObject::connect(langBtn, SIGNAL(clicked()), this, SLOT(showLanguagePage()));
        QObject::connect(federationBtn, SIGNAL(clicked()), this, SLOT(showFederationPage()));
    }
    
    void showThemeDialog() {
        QString filePath = QFileDialog::getOpenFileName(window, "Select Theme", QDir::homePath());
        if (!filePath.isEmpty()) {
            config["THEMES"]["theme"] = filePath.toStdString();
            saveConfig();
            loadStyle();
        }
    }
    
    void showLanguagePage() {
        clearLayout(layout);
        
        QPushButton *ptBtn = new QPushButton("Portuguese (pt-br)");
        QPushButton *enBtn = new QPushButton("English (en)");
        QPushButton *backBtn = new QPushButton("Back");
        
        layout->addWidget(ptBtn);
        layout->addWidget(enBtn);
        layout->addWidget(backBtn);
        
        QObject::connect(backBtn, SIGNAL(clicked()), this, SLOT(showOptionsPage()));
        QObject::connect(ptBtn, SIGNAL(clicked()), this, SLOT(onLanguagePtClicked()));
        QObject::connect(enBtn, SIGNAL(clicked()), this, SLOT(onLanguageEnClicked()));
    }
    
    void showFederationPage() {
        clearLayout(layout);
        
        QLineEdit *urlEntry = new QLineEdit();
        urlEntry->setPlaceholderText("Federation URL");
        
        QPushButton *addBtn = new QPushButton("Add");
        QPushButton *backBtn = new QPushButton("Back");
        
        layout->addWidget(urlEntry);
        layout->addWidget(addBtn);
        layout->addWidget(backBtn);
        
        QObject::connect(backBtn, SIGNAL(clicked()), this, SLOT(showOptionsPage()));
        QObject::connect(addBtn, SIGNAL(clicked()), this, SLOT(onAddFederationClicked()));
    }
    
    void onSigninClicked() {
        // Implementar sign in
    }
    
    void onSignupClicked() {
        // Implementar sign up
    }
    
    void onServerUrlChanged() {
        // Implementar mudança de URL
    }
    
    void onAddFriendClicked() {
        // Implementar adicionar amigo
    }
    
    void onProfileSaveClicked() {
        // Implementar salvar perfil
    }
    
    void onLanguagePtClicked() {
        config["LANG"]["lang"] = "pt-br";
        saveConfig();
        showInitialPage();
    }
    
    void onLanguageEnClicked() {
        config["LANG"]["lang"] = "en";
        saveConfig();
        showInitialPage();
    }
    
    void onAddFederationClicked() {
        // Implementar adicionar federação
    }
};

int main(int argc, char *argv[])
{
    QApplication app(argc, argv);
    QTranslator *translator = new QTranslator(&app);
    
    loadConfig();
    
    QString current_version = "1.0";
    QString username = QString::fromStdString(config["FAST-LOGIN"]["username"]);
    QString token_session = QString::fromStdString(config["FAST-LOGIN"]["token_session"]);
    QString url = QString::fromStdString(config["SERVER"]["url"]);
    
    if (token_session.isEmpty()) {
        QJsonObject json_token;
        QString response = requestHTTP(url + "/new-session", "POST", json_token);
        QByteArray byteArray = response.toUtf8();
        QJsonDocument doc = QJsonDocument::fromJson(byteArray);
        QJsonObject jsonObject = doc.object();
        QString new_token = jsonObject.value("token").toString();
        config["FAST-LOGIN"]["token_session"] = new_token.toStdString();
        saveConfig();
    }
    
    if (config["LANG"]["lang"] == "pt-br") {
        if (translator->load(":/translations/pt-br-main-page.qm")) {
            app.installTranslator(translator);
        }
    }
    
    QStyle *st = QStyleFactory::create("Plastique");
    if (!st) st = QStyleFactory::create("Cleanlooks");
    if (st) app.setStyle(st);
    
    loadConfig();
    loadStyle();
    
    for (std::map<std::string, std::map<std::string, std::string> >::iterator sec = config.begin(); 
         sec != config.end(); ++sec) {
        std::cout << "[" << sec->first << "]\n";
        for (std::map<std::string, std::string>::iterator kv = sec->second.begin(); 
             kv != sec->second.end(); ++kv) {
            std::cout << "  " << kv->first << " = " << kv->second << "\n";
        }
    }
    
    if (url.isEmpty()) {
        config["SERVER"]["url"] = "http://linkaProject.pythonanywhere.com";
        url = QString::fromStdString(config["SERVER"]["url"]);
        saveConfig();
    }
    
    qDebug() << "url" << url;
    
    QRect screenGeometry = QApplication::desktop()->screenGeometry();
    int screenWidth = screenGeometry.width();
    int screenHeight = screenGeometry.height();
    
    QFont globalFont = app.font();
    if (screenWidth >= 1080) {
        globalFont.setPointSize(16);
    } else if (screenWidth >= 720) {
        globalFont.setPointSize(13);
    } else {
        globalFont.setPointSize(11);
    }
    app.setFont(globalFont);
    
    QMainWindow window;
    app.setWindowIcon(QIcon(":/assets/icon.png"));
    QPixmap pixmap(":/assets/icon.png");
    QSplashScreen splash(pixmap);
    splash.show();
    
    window.setWindowTitle("Linka Mobile");
    window.setGeometry(0, 0, screenWidth, screenHeight);
    
    QWidget *central = new QWidget();
    QVBoxLayout *layout = new QVBoxLayout(central);
    int margin = screenWidth * 0.05;
    layout->setContentsMargins(margin, margin, margin, margin);
    layout->setSpacing(screenHeight * 0.03);
    
    window.setCentralWidget(central);
    
    QNetworkAccessManager *manager = new QNetworkAccessManager(&window);
    
    AppManager appManager(&window, central, layout, manager);
    
    // Validar token
    QString token = QString::fromStdString(config["FAST-LOGIN"]["token_session"]);
    QJsonObject json_valide;
    json_valide.insert("token", QJsonValue(token));
    int status_code = 0;
    requestHTTP(url + "/valide", "POST", json_valide, 5000, &status_code);
    
    if (token.isEmpty() || (status_code != 200 && status_code != 201)) {
        appManager.showLoginPage();
    } else {
        appManager.showInitialPage();
    }
    
    splash.finish(&window);
    window.show();
    
    return app.exec();
}

#include "menuQt48.moc"
