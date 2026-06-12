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
#include <QVector>
#include <QSplashScreen>
#include <QMenu>
#include <iostream>
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
#include <QScrollArea>
#include <QTimer>
#include <QFrame>
#include <QIcon>
#include <QSize>
#include <QPainter>
#include <QFontMetrics>
#include <QTranslator>
#include <QGraphicsOpacityEffect>
#include <QPropertyAnimation>
#include <QDesktopServices>
#include <QRegExp>
#include <QStringList>

// Tabela Hash Global de Configurações no padrão C++98
std::map<std::string, std::map<std::string, std::string> > config;

// Função Auxiliar de Remoção de Espaços para C++98
std::string trim(const std::string &s) {
    size_t start = s.find_first_not_of(" \t\r\n");
    size_t end = s.find_last_not_of(" \t\r\n");
    if(start == std::string::npos) return "";
    return s.substr(start, end - start + 1);
}

// Extrator Ultra-Leve de JSON nativo via Regex para substituir QJsonDocument/nlohmann no Android 2.3
QString getJsonValue(const QString &jsonStr, const QString &key) {
    QRegExp rx("\"" + key + "\"\\s*:\\s*\"([^\"]*)\"");
    if (rx.indexIn(jsonStr) != -1) {
        return rx.cap(1);
    }
    QRegExp rxNum("\"" + key + "\"\\s*:\\s*([^,\\s\\}\\]]+)");
    if (rxNum.indexIn(jsonStr) != -1) {
        return rxNum.cap(1).replace("\"", "").trimmed();
    }
    return "";
}

// Analisador de Arrays JSON Simplificado para extração de listas de strings (Feed, Notificações, Busca)
QStringList getJsonArrayValues(const QString &jsonStr, const QString &arrayKey) {
    QStringList result;
    QRegExp rx("\"" + arrayKey + "\"\\s*:\\s*\\[([^\\]]*)\\]");
    if (rx.indexIn(jsonStr) != -1) {
        QString block = rx.cap(1);
        QRegExp itemRx("\"([^\"]*)\"");
        int pos = 0;
        while ((pos = itemRx.indexIn(block, pos)) != -1) {
            result.append(itemRx.cap(1));
            pos += itemRx.matchedLength();
        }
    }
    return result;
}

// Analisador de blocos de objetos internos de JSON de forma incremental
QStringList getJsonObjectsInArray(const QString &jsonStr) {
    QStringList result;
    QRegExp rx("(\\{[^\\}]*\\})");
    int pos = 0;
    while ((pos = rx.indexIn(jsonStr, pos)) != -1) {
        result.append(rx.cap(1));
        pos += rx.matchedLength();
    }
    return result;
}

// Construtor manual de pares chave-valor para payloads JSON de requisições HTTP
QString jsonField(const QString &k, const QString &v) {
    return QString("\"%1\":\"%2\"").arg(k).arg(v);
}

// Efeito Visual de Transição Suave (Fade) compatível com hardware do Android Gingerbread
void fadeTransition(QWidget *widget) {
    if (!widget) return;
    QGraphicsOpacityEffect *effect = new QGraphicsOpacityEffect(widget);
    widget->setGraphicsEffect(effect);
    QPropertyAnimation *anim = new QPropertyAnimation(effect, "opacity");
    anim->setDuration(300);
    anim->setStartValue(0.0);
    anim->setEndValue(1.0);
    anim->start(QAbstractAnimation::DeleteWhenStopped);
}

// Renderizador Customizado de Balões de Conversa para Chat Assíncrono
class ChatBubble : public QWidget {
private:
    QString message;
    bool mine;
public:
    ChatBubble(QString text, bool isMe, QWidget *parent = NULL)
        : QWidget(parent), message(text), mine(isMe)
    {
        setMaximumWidth(280); 
        setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Fixed);
    }

protected:
    void paintEvent(QPaintEvent *event) {
        Q_UNUSED(event);
        QPainter painter(this);
        painter.setRenderHint(QPainter::Antialiasing);

        QRect bubbleRect = rect().adjusted(5, 3, -5, -3);
        QColor bubbleColor = mine ? QColor(0, 200, 120) : QColor(60, 60, 60);

        painter.setBrush(bubbleColor);
        painter.setPen(Qt::NoPen);
        painter.drawRoundedRect(bubbleRect, 12, 12);

        painter.setPen(Qt::white);
        painter.setFont(QFont("Arial", 10));
        painter.drawText(bubbleRect.adjusted(10, 8, -10, -8), Qt::TextWordWrap, message);
    }

    QSize sizeHint() const {
        QFontMetrics fm(QFont("Arial", 10));
        QRect r = fm.boundingRect(0, 0, 200, 1000, Qt::TextWordWrap, message);
        return QSize(r.width() + 40, r.height() + 22);
    }
};

// Resolução de Diretórios do Sistema de Arquivos substituindo o QStandardPaths do Qt 5
QString configPath() {
    QString dirPath = QDesktopServices::storageLocation(QDesktopServices::DataLocation);
    QDir().mkpath(dirPath);
    return dirPath + "/config-login.cfg";
}

// Carregamento de Configurações (.ini/.cfg) via Streams de Texto Clássicos
void loadConfig() {
    QString path = configPath();
    if (!QFile::exists(path)) {
        QFile res(":/config-login.cfg");
        if (!res.open(QIODevice::ReadOnly | QIODevice::Text)) return;
        QFile out(path);
        if (!out.open(QIODevice::WriteOnly | QIODevice::Text)) {
            res.close();
            return;
        }
        out.write(res.readAll());
        out.close();
        res.close();
    }

    QFile file(path);
    if (!file.open(QIODevice::ReadOnly | QIODevice::Text)) return;

    QTextStream in(&file);
    std::string section;
    QString line;

    while (!in.atEnd()) {
        line = in.readLine().trimmed();
        if (line.isEmpty()) continue;

        if (line.startsWith("[") && line.contains("]")) {
            section = line.toStdString();
            section = section.substr(1, section.find(']') - 1);
        } else {
            int pos = line.indexOf('=');
            if (pos != -1) {
                QString key = line.left(pos).trimmed();
                QString value = line.mid(pos + 1).trimmed();
                config[section][key.toStdString()] = value.toStdString();
            }
        }
    }
    file.close();
}

// Persistência de Dados de Configurações e Tokens de Autenticação Fast-Login
void saveConfig() {
    QString path = configPath();
    std::ofstream file(path.toStdString().c_str());
    if(!file.is_open()) return;

    file << "[SERVER]\nurl = " << config["SERVER"]["url"] << "\n\n";
    file << "[LANG]\nlang = " << config["LANG"]["lang"] << "\n\n";
    file << "[FAST-LOGIN]\nusername = " << config["FAST-LOGIN"]["username"] << "\n";
    file << "token = " << config["FAST-LOGIN"]["token"] << "\n";
    file << "password = " << config["FAST-LOGIN"]["password"] << "\n";
    file << "token_session = " << config["FAST-LOGIN"]["token_session"] << "\n\n";
    file << "[FEDERATIONS]\nurl = " << config["FEDERATIONS"]["url"] << "\n\n";
    file << "[THEMES]\ntheme = " << config["THEMES"]["theme"] << "\n";
    file.close();
}

// Geração de Sessão e Aquisição de Token JWT/Bearer no Servidor de Destino
QString newSession(QString username, QString password) {
    loadConfig();
    QString url = QString::fromStdString(config["SERVER"]["url"]);

    QNetworkAccessManager manager;
    QString payload = "{" + jsonField("username", username) + "," + jsonField("password", password) + "}";

    QNetworkRequest request;
    request.setUrl(QUrl(url + "/new-session"));
    request.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");

    QNetworkReply *reply = manager.post(request, payload.toUtf8());

    QEventLoop loop;
    QObject::connect(reply, SIGNAL(finished()), &loop, SLOT(quit()));
    loop.exec();

    QString response = QString::fromUtf8(reply->readAll().constData());
    reply->deleteLater();

    return getJsonValue(response, "token");
}

// Mecanismo Unificado de Requisições Síncronas com Tratamento Automático de Banimento e Renovação de Sessão
QString requestHTTP(const QString &url, const QString &method, const QString &jsonPayload, int timeoutMs = 5000, int *statusCode = NULL, bool logged = true) {
    QNetworkAccessManager manager;
    QNetworkRequest request;
    
    QString currentToken = QString::fromStdString(config["FAST-LOGIN"]["token_session"]);
    if (!currentToken.isEmpty()) {
        request.setRawHeader("Authorization", QString("Bearer %1").arg(currentToken).toUtf8());
    }
    
    request.setUrl(QUrl(url));
    QString m = method.toUpper();

    if (m != "GET") {
        request.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");
    }

    QNetworkReply *reply = NULL;
    QByteArray jsonData = jsonPayload.toUtf8();

    if(m == "GET")          reply = manager.get(request);
    else if(m == "POST")    reply = manager.post(request, jsonData);
    else if(m == "PUT")     reply = manager.put(request, jsonData);
    else if(m == "DELETE")  reply = manager.sendCustomRequest(request, "DELETE", jsonData);
    else {
        if(statusCode) *statusCode = -1;
        return "ERRO: Metodo invalido";
    }
    
    QEventLoop loop;
    QTimer timer;
    timer.setSingleShot(true);
    
    QObject::connect(&timer, SIGNAL(timeout()), &loop, SLOT(quit()));
    QObject::connect(reply, SIGNAL(finished()), &loop, SLOT(quit()));
    
    timer.start(timeoutMs);
    loop.exec();
    
    if(!timer.isActive()) {
        reply->abort();
        if(statusCode) *statusCode = 408;
        reply->deleteLater();
        return "ERRO: Timeout";
    }
    
    int code = reply->attribute(QNetworkRequest::HttpStatusCodeAttribute).toInt();
    if(statusCode) *statusCode = code;
    
    QString response = QString::fromUtf8(reply->readAll().constData());
    reply->deleteLater();
    
    if (getJsonValue(response, "status") == "banned") {
        return "BANNED";
    }

    if (logged && (code == 401 || code == 403)) {
        QString username = QString::fromStdString(config["FAST-LOGIN"]["username"]);
        QString password = QString::fromStdString(config["FAST-LOGIN"]["password"]);
        QString newToken = newSession(username, password);
        if(!newToken.isEmpty()) {
            config["FAST-LOGIN"]["token_session"] = newToken.toStdString();
            saveConfig();
            return requestHTTP(url, method, jsonPayload, timeoutMs, statusCode, false);
        }
    }
    
    return response;
}

// Gerenciador Dinâmico de Modos de Visualização com ScrollArea de Alta Performance
void scroll_area(QVBoxLayout *layout, const QList<QWidget*> &widgets) {
    QScrollArea *scroll = new QScrollArea();
    scroll->setWidgetResizable(true);

    QWidget *container = new QWidget();
    QVBoxLayout *containerLayout = new QVBoxLayout(container);

    for(int i = 0; i < widgets.size(); ++i) {
        if(widgets[i]) containerLayout->addWidget(widgets[i]);
    }

    containerLayout->addStretch();
    scroll->setWidget(container);
    layout->addWidget(scroll);
}

// Limpeza Recursiva Segura de Elementos de Interface Gráfica para Evitar Vazamento de Memória
void clearLayout(QLayout *layout) {
    if (!layout) return;
    QLayoutItem *item;
    while ((item = layout->takeAt(0))) {
        if (item->layout()) {
            clearLayout(item->layout());
        } else if (item->widget()) {
            delete item->widget();
        }
        delete item;
    }
}

// Injeção Estática de Folhas de Estilo Baseada no Estado do Arquivo de Configurações
void loadStyle() {
    loadConfig();
    QString themePath = QString::fromStdString(config["THEMES"]["theme"]);
    if (themePath.isEmpty()) {
        themePath = ":/styles/theme.qss";
    }
    QFile file(themePath);
    if (!file.exists()) return;
    if (!file.open(QFile::ReadOnly | QFile::Text)) return;
    QString qss = file.readAll();
    file.close();
    if (!qss.isEmpty()) {
        qApp->setStyleSheet(qss);
    }
}

// ============================================================================
// CONTROLADOR MAESTRO DE INTERFACE GRAFICA (Substitui todas as 30+ lambdas do C++11)
// ============================================================================
class LinkaApplicationController : public QObject {
    Q_OBJECT
private:
    QWidget *centralWidget;
    QVBoxLayout *mainLayout;
    QStackedWidget *viewStack;
    QSplashScreen *splashRef;
    
    QString url;
    QString currentUsername;
    int lastGlobalMessageId;

    // Elementos e Inputs de Interface Armazenados como Membros da Classe
    QLineEdit *usernameInput;
    QLineEdit *passwordInput;
    QLineEdit *retryPasswordInput;
    QLineEdit *emailInput;
    QLineEdit *postTextInput;
    QLineEdit *messageInput;
    QLineEdit *bioInput;
    QLineEdit *urlInput;
    QLineEdit *searchBoxInput;
    
    // Layouts Dinâmicos e ScrollAreas Internas Remotificáveis
    QVBoxLayout *globalChatListLayout;
    QScrollArea *globalChatScrollArea;
    QVBoxLayout *privateChatListLayout;
    QScrollArea *privateChatScrollArea;
    QVBoxLayout *searchResultsLayout;
    
    // Relógio de Pooling Assíncrono do Sistema de Mensagens
    QTimer *chatUpdateTimer;
    QString currentActiveChatTarget;

public:
    LinkaApplicationController(QWidget *central, QVBoxLayout *layout, QSplashScreen *splash)
        : centralWidget(central), mainLayout(layout), splashRef(splash), lastGlobalMessageId(0), chatUpdateTimer(NULL)
    {
        url = QString::fromStdString(config["SERVER"]["url"]);
        currentUsername = QString::fromStdString(config["FAST-LOGIN"]["username"]);
    }

    ~LinkaApplicationController() {
        if(chatUpdateTimer) {
            chatUpdateTimer->stop();
            delete chatUpdateTimer;
        }
    }

public slots:
    // PÁGINA CENTRAL DE LOGIN / AUTENTICAÇÃO
    void showLoginPage() {
        clearLayout(mainLayout);
        fadeTransition(centralWidget);

        QPushButton *signinPageBtn = new QPushButton(QCoreApplication::translate("initial-page", "sign-in"));
        QPushButton *signupPageBtn = new QPushButton(QCoreApplication::translate("initial-page", "sign-up"));
        QPushButton *changeServerBtn = new QPushButton("Configurar Servidor Target");

        mainLayout->addWidget(signinPageBtn);
        mainLayout->addWidget(signupPageBtn);
        mainLayout->addWidget(changeServerBtn);

        connect(signinPageBtn, SIGNAL(clicked()), this, SLOT(showSigninPage()));
        connect(signupPageBtn, SIGNAL(clicked()), this, SLOT(showSignupPage()));
        connect(changeServerBtn, SIGNAL(clicked()), this, SLOT(showChangeServerPage()));
    }

    // FORMULÁRIO DE REGISTRO (SIGNIN PAGE)
    void showSigninPage() {
        clearLayout(mainLayout);
        fadeTransition(centralWidget);

        usernameInput = new QLineEdit();
        usernameInput->setPlaceholderText(QCoreApplication::translate("global", "username"));
        passwordInput = new QLineEdit();
        passwordInput->setPlaceholderText(QCoreApplication::translate("sign-up", "password"));
        passwordInput->setEchoMode(QLineEdit::Password);
        retryPasswordInput = new QLineEdit();
        retryPasswordInput->setPlaceholderText(QCoreApplication::translate("sign-up", "retry the password"));
        retryPasswordInput->setEchoMode(QLineEdit::Password);
        emailInput = new QLineEdit();
        emailInput->setPlaceholderText(QCoreApplication::translate("sign-up", "email"));

        QPushButton *sendBtn = new QPushButton(QCoreApplication::translate("global", "send"));
        QPushButton *backBtn = new QPushButton(QCoreApplication::translate("global", "back"));

        mainLayout->addWidget(usernameInput);
        mainLayout->addWidget(passwordInput);
        mainLayout->addWidget(retryPasswordInput);
        mainLayout->addWidget(emailInput);
        mainLayout->addWidget(sendBtn);
        mainLayout->addWidget(backBtn);

        connect(backBtn, SIGNAL(clicked()), this, SLOT(showLoginPage()));
        connect(sendBtn, SIGNAL(clicked()), this, SLOT(executeSigninRequest()));
    }

    void executeSigninRequest() {
        if (passwordInput->text() != retryPasswordInput->text()) {
            QMessageBox::warning(centralWidget, "Erro", "As senhas digitadas nao conferem!");
            return;
        }

        QString payload = "{" + jsonField("username", usernameInput->text()) + "," +
                                jsonField("password", passwordInput->text()) + "," +
                                jsonField("email", emailInput->text()) + "}";
        int code = 0;
        requestHTTP(url + "/register", "POST", payload, 10000, &code, false);

        QString profilePayload = "{" + jsonField("username", usernameInput->text()) + "}";
        requestHTTP(url + "/create-profile", "POST", profilePayload, 5000, NULL, true);

        if (code == 200 || code == 201) {
            config["FAST-LOGIN"]["username"] = usernameInput->text().toStdString();
            config["FAST-LOGIN"]["password"] = passwordInput->text().toStdString();
            QString tkn = newSession(usernameInput->text(), passwordInput->text());
            config["FAST-LOGIN"]["token_session"] = tkn.toStdString();
            saveConfig();
            currentUsername = usernameInput->text();
            showInitialPage();
        } else {
            QMessageBox::critical(centralWidget, "Erro", "Erro ao efetuar registro no servidor remoto.");
        }
    }

    // FORMULÁRIO DE ENTRADA (SIGNUP PAGE)
    void showSignupPage() {
        clearLayout(mainLayout);
        fadeTransition(centralWidget);

        usernameInput = new QLineEdit();
        usernameInput->setPlaceholderText(QCoreApplication::translate("global", "username"));
        passwordInput = new QLineEdit();
        passwordInput->setPlaceholderText(QCoreApplication::translate("sign-up", "password"));
        passwordInput->setEchoMode(QLineEdit::Password);

        QPushButton *sendBtn = new QPushButton(QCoreApplication::translate("global", "send"));
        QPushButton *backBtn = new QPushButton(QCoreApplication::translate("global", "back"));

        mainLayout->addWidget(usernameInput);
        mainLayout->addWidget(passwordInput);
        mainLayout->addWidget(sendBtn);
        mainLayout->addWidget(backBtn);

        connect(backBtn, SIGNAL(clicked()), this, SLOT(showLoginPage()));
        connect(sendBtn, SIGNAL(clicked()), this, SLOT(executeSignupRequest()));
    }

    void executeSignupRequest() {
        QString userTxt = usernameInput->text();
        QString passTxt = passwordInput->text();

        QString tokenGerado = newSession(userTxt, passTxt);
        if (!tokenGerado.isEmpty()) {
            config["FAST-LOGIN"]["username"] = userTxt.toStdString();
            config["FAST-LOGIN"]["password"] = passTxt.toStdString();
            config["FAST-LOGIN"]["token_session"] = tokenGerado.toStdString();
            saveConfig();
            currentUsername = userTxt;
            showInitialPage();
        } else {
            QMessageBox::critical(centralWidget, "Autenticacao", "Usuario ou senha incorretos!");
        }
    }

    // TELA PRINCIPAL (DASHBOARD COM STACKED WIDGET E ABA INFERIOR)
    void showInitialPage() {
        if(chatUpdateTimer) { chatUpdateTimer->stop(); }
        clearLayout(mainLayout);
        fadeTransition(centralWidget);
        if(splashRef) { splashRef->finish(centralWidget->window()); }

        viewStack = new QStackedWidget(centralWidget);

        // Sub-página Home
        QWidget *pageHome = new QWidget();
        QVBoxLayout *homeLayout = new QVBoxLayout(pageHome);
        QLabel *homeLabel = new QLabel("HABITAT HOME");
        homeLabel->setAlignment(Qt::AlignCenter);
        homeLayout->addWidget(homeLabel);
        viewStack->addWidget(pageHome); // Index 0

        // Sub-página Busca
        QWidget *pageSearch = new QWidget();
        QVBoxLayout *searchLayout = new QVBoxLayout(pageSearch);
        QLabel *searchLabel = new QLabel("BUSCA INTEGRADA");
        searchLabel->setAlignment(Qt::AlignCenter);
        searchLayout->addWidget(searchLabel);
        viewStack->addWidget(pageSearch); // Index 1

        // Sub-página Chat
        QWidget *pageChat = new QWidget();
        QVBoxLayout *chatLayout = new QVBoxLayout(pageChat);
        QLabel *chatLabel = new QLabel("CENTRAL CHAT");
        chatLabel->setAlignment(Qt::AlignCenter);
        chatLayout->addWidget(chatLabel);
        viewStack->addWidget(pageChat); // Index 2

        // Sub-página Perfil
        QWidget *pageProfile = new QWidget();
        QVBoxLayout *profileLayout = new QVBoxLayout(pageProfile);
        QLabel *profileLabel = new QLabel("PAINEL PERFIL");
        profileLabel->setAlignment(Qt::AlignCenter);
        profileLayout->addWidget(profileLabel);
        viewStack->addWidget(pageProfile); // Index 3

        // Sub-página Opções
        QWidget *pageOptions = new QWidget();
        QVBoxLayout *optionsLayout = new QVBoxLayout(pageOptions);
        QLabel *optionsLabel = new QLabel("MENU CONFIG");
        optionsLabel->setAlignment(Qt::AlignCenter);
        optionsLayout->addWidget(optionsLabel);
        viewStack->addWidget(pageOptions); // Index 4

        // Criação Física do BottomBar
        QWidget *bottomBar = new QWidget(centralWidget);
        bottomBar->setFixedHeight(75);
        bottomBar->setStyleSheet("background: #111;");

        QHBoxLayout *barLayout = new QHBoxLayout(bottomBar);
        barLayout->setContentsMargins(5, 5, 5, 5);
        barLayout->setSpacing(5);

        QPushButton *btnHome = new QPushButton(bottomBar);
        QPushButton *btnChat = new QPushButton(bottomBar);
        QPushButton *btnProfile = new QPushButton(bottomBar);
        QPushButton *btnOptions = new QPushButton(bottomBar);
        QPushButton *btnSearch = new QPushButton(bottomBar);

        btnHome->setIcon(QIcon(":/assets/home.png"));     btnHome->setIconSize(QSize(48, 48));
        btnChat->setIcon(QIcon(":/assets/chat.png"));     btnChat->setIconSize(QSize(48, 48));
        btnProfile->setIcon(QIcon(":/assets/account.png")); btnProfile->setIconSize(QSize(48, 48));
        btnOptions->setIcon(QIcon(":/assets/options.png")); btnOptions->setIconSize(QSize(48, 48));
        btnSearch->setIcon(QIcon(":/assets/search.png"));   btnSearch->setIconSize(QSize(48, 48));

        QString btnStyle = "border: none; background: transparent; color: white;";
        btnHome->setStyleSheet(btnStyle); btnChat->setStyleSheet(btnStyle);
        btnProfile->setStyleSheet(btnStyle); btnOptions->setStyleSheet(btnStyle); btnSearch->setStyleSheet(btnStyle);

        barLayout->addWidget(btnHome); barLayout->addWidget(btnChat);
        barLayout->addWidget(btnProfile); barLayout->addWidget(btnOptions);
        barLayout->addWidget(btnSearch);

        mainLayout->addWidget(viewStack, 1);
        mainLayout->addWidget(bottomBar, 0);

        // Conexões Sinais/Slots Diretas e Seguras para Transições entre Módulos
        connect(btnHome, SIGNAL(clicked()), this, SLOT(showFeedView()));
        connect(btnChat, SIGNAL(clicked()), this, SLOT(showChatView()));
        connect(btnProfile, SIGNAL(clicked()), this, SLOT(showProfileView()));
        connect(btnOptions, SIGNAL(clicked()), this, SLOT(showOptionsView()));
        connect(btnSearch, SIGNAL(clicked()), this, SLOT(showSearchView()));

        // Inicia exibindo o Feed por padrão de design
        showFeedView();
    }

    // MÓDULO 1: VIEW DO FEED DE POSTAGENS E GERENCIAMENTO DE ESTRELAS
    void showFeedView() {
        clearLayout(mainLayout);
        viewStack = new QStackedWidget(centralWidget);
        
        QWidget *feedContainer = new QWidget();
        QVBoxLayout *feedContentLayout = new QVBoxLayout(feedContainer);
        
        QLabel *loadingLbl = new QLabel("Carregando feed de postagens assincronamente...");
        feedContentLayout->addWidget(loadingLbl);

        QString response = requestHTTP(url + "/feed", "GET", "", 6000, NULL, true);
        QStringList objects = getJsonObjectsInArray(response);

        QList<QWidget*> feedWidgets;
        for (int i = 0; i < objects.size(); ++i) {
            QString item = objects[i];
            QString idStr = getJsonValue(item, "id");
            QString user = getJsonValue(item, "username");
            QString text = getJsonValue(item, "text_post");
            QString dt = getJsonValue(item, "datetime");

            QFrame *frame = new QFrame();
            frame->setStyleSheet("background-color: #1A1A1A; border: 1px solid #2F2F2F; border-radius: 14px; padding: 10px;");
            QVBoxLayout *frameLayout = new QVBoxLayout(frame);

            QLabel *lblUser = new QLabel(user);
            lblUser->setStyleSheet("color: white; font-size: 16px; font-weight: bold;");
            QLabel *lblText = new QLabel(text);
            lblText->setStyleSheet("color: white; font-size: 14px;");
            lblText->setWordWrap(true);
            QLabel *lblDate = new QLabel(dt);
            lblDate->setStyleSheet("color: gray; font-size: 12px;");

            frameLayout->addWidget(lblUser);
            frameLayout->addWidget(lblText);
            frameLayout->addWidget(lblDate);

            // Setor de Likes / Estrelas Dinâmico
            QHBoxLayout *starLayout = new QHBoxLayout();
            QPushButton *iconButton = new QPushButton();
            iconButton->setIcon(QIcon(":/assets/default_star.png"));
            iconButton->setIconSize(QSize(24, 24));
            iconButton->setFixedSize(30, 30);
            iconButton->setStyleSheet("border: none; background: transparent;");

            QLabel *starLabel = new QLabel("...");
            starLabel->setStyleSheet("color: white; font-size: 14px;");

            // Chamada paralela simulada por sincronismo para contagem das estrelas
            QString countStars = requestHTTP(url + "/return-stars/" + idStr, "GET", "", 2000, NULL, false);
            starLabel->setText(countStars.trimmed().isEmpty() ? "0" : countStars.trimmed());

            QPushButton *commentButton = new QPushButton("Comentarios");
            commentButton->setStyleSheet("color: #00ffea; border: none; background: transparent;");

            starLayout->addWidget(iconButton);
            starLayout->addWidget(starLabel);
            starLayout->addStretch();
            starLayout->addWidget(commentButton);
            frameLayout->addLayout(starLayout);

            // Mapeamento de propriedades para resgatar nos slots acionados por sinal
            iconButton->setProperty("postId", idStr);
            iconButton->setProperty("postUser", user);
            commentButton->setProperty("postId", idStr);

            connect(iconButton, SIGNAL(clicked()), this, SLOT(handleStarToggle()));
            connect(commentButton, SIGNAL(clicked()), this, SLOT(handleCommentNavigation()));

            feedWidgets.append(frame);
        }

        scroll_area(feedContentLayout, feedWidgets);

        QPushButton *btnNewPost = new QPushButton(QCoreApplication::translate("feed", "new post"));
        feedContentLayout->addWidget(btnNewPost);
        connect(btnNewPost, SIGNAL(clicked()), this, SLOT(showNewPostForm()));

        rebuildBottomNavigation(feedContentLayout, 0);
    }

    void handleStarToggle() {
        QPushButton *btn = qobject_cast<QPushButton*>(sender());
        if(!btn) return;
        QString pId = btn->property("postId").toString();
        QString pUser = btn->property("postUser").toString();

        QString payload = "{" + jsonField("username", pUser) + "," + jsonField("post_id", pId) + "}";
        requestHTTP(url + "/star", "POST", payload);
        QString statusStarred = requestHTTP(url + "/has-star", "POST", payload);

        if(statusStarred.contains("true")) {
            btn->setIcon(QIcon(":/assets/star.png"));
        } else {
            btn->setIcon(QIcon(":/assets/default_star.png"));
        }
    }

    void handleCommentNavigation() {
        QPushButton *btn = qobject_cast<QPushButton*>(sender());
        if(!btn) return;
        QString pId = btn->property("postId").toString();
        showCommentPage(pId);
    }

    void showNewPostForm() {
        clearLayout(mainLayout);
        postTextInput = new QLineEdit();
        postTextInput->setPlaceholderText(QCoreApplication::translate("feed", "text post"));

        QPushButton *btnPublish = new QPushButton(QCoreApplication::translate("feed", "new post"));
        QPushButton *btnBack = new QPushButton(QCoreApplication::translate("global", "back"));

        mainLayout->addWidget(postTextInput);
        mainLayout->addWidget(btnPublish);
        mainLayout->addWidget(btnBack);

        connect(btnBack, SIGNAL(clicked()), this, SLOT(showInitialPage()));
        connect(btnPublish, SIGNAL(clicked()), this, SLOT(executeNewPostRequest()));
    }

    void executeNewPostRequest() {
        QString payload = "{" + jsonField("username", currentUsername) + "," +
                                jsonField("text_post", postTextInput->text()) + "," +
                                jsonField("datetime", "12/06/2026") + "}";
        requestHTTP(url + "/new", "POST", payload);
        showInitialPage();
    }

    // SEÇÃO DE COMENTÁRIOS DE UMA POSTAGEM
    void showCommentPage(QString postId) {
        clearLayout(mainLayout);
        
        QLabel *title = new QLabel(QCoreApplication::translate("feed", "comments"));
        mainLayout->addWidget(title);

        QList<QWidget*> commentWidgets;
        QString payload = "{" + jsonField("post_id", postId) + "}";
        QString response = requestHTTP(url + "/view-comments", "POST", payload);
        QStringList objects = getJsonObjectsInArray(response);

        for(int i = 0; i < objects.size(); ++i) {
            QString commentText = getJsonValue(objects[i], "comment");
            commentWidgets.append(new QLabel("💬 " + commentText));
        }
        scroll_area(mainLayout, commentWidgets);

        QPushButton *btnNewComment = new QPushButton(QCoreApplication::translate("feed", "new comment"));
        btnNewComment->setProperty("postId", postId);
        mainLayout->addWidget(btnNewComment);
        
        QPushButton *btnBack = new QPushButton(QCoreApplication::translate("global", "back"));
        mainLayout->addWidget(btnBack);

        connect(btnBack, SIGNAL(clicked()), this, SLOT(showInitialPage()));
        connect(btnNewComment, SIGNAL(clicked()), this, SLOT(showNewCommentInputPage()));
    }

    void showNewCommentInputPage() {
        QPushButton *btn = qobject_cast<QPushButton*>(sender());
        if(!btn) return;
        QString pId = btn->property("postId").toString();

        clearLayout(mainLayout);
        postTextInput = new QLineEdit();
        postTextInput->setPlaceholderText(QCoreApplication::translate("feed", "new comment"));

        QPushButton *btnSend = new QPushButton(QCoreApplication::translate("global", "send"));
        btnSend->setProperty("postId", pId);
        QPushButton *btnBack = new QPushButton(QCoreApplication::translate("global", "back"));

        mainLayout->addWidget(postTextInput);
        mainLayout->addWidget(btnSend);
        mainLayout->addWidget(btnBack);

        connect(btnBack, SIGNAL(clicked()), this, SLOT(showInitialPage()));
        connect(btnSend, SIGNAL(clicked()), this, SLOT(executeNewCommentRequest()));
    }

    void executeNewCommentRequest() {
        QPushButton *btn = qobject_cast<QPushButton*>(sender());
        if(!btn) return;
        QString pId = btn->property("postId").toString();

        QString payload = "{" + jsonField("post_id", pId) + "," +
                                jsonField("username", currentUsername) + "," +
                                jsonField("text_comment", postTextInput->text()) + "}";
        requestHTTP(url + "/comments", "POST", payload);
        showCommentPage(pId);
    }

    // MÓDULO 2: VIEW DO ENGINE DE BUSCA (USERNAMES E POSTS)
    void showSearchView() {
        clearLayout(mainLayout);
        QVBoxLayout *searchContentLayout = new QVBoxLayout();

        searchBoxInput = new QLineEdit();
        searchBoxInput->setPlaceholderText(QCoreApplication::translate("initial-page", "search"));
        searchContentLayout->addWidget(searchBoxInput);

        QPushButton *btnDoSearch = new QPushButton(QCoreApplication::translate("initial-page", "search") + "!");
        searchContentLayout->addWidget(btnDoSearch);

        QWidget *resultsContainer = new QWidget();
        searchResultsLayout = new QVBoxLayout(resultsContainer);
        searchContentLayout->addWidget(resultsContainer);

        connect(btnDoSearch, SIGNAL(clicked()), this, SLOT(executeSearchQuery()));

        mainLayout->addLayout(searchContentLayout);
        rebuildBottomNavigation(mainLayout, 1);
    }

    void executeSearchQuery() {
        clearLayout(searchResultsLayout);
        QString query = searchBoxInput->text();
        if(query.isEmpty()) return;

        QString payload = "{" + jsonField("content", query) + "}";
        QString response = requestHTTP(url + "/search", "POST", payload);

        // Parse e extração manual de dados de arrays nativos estruturados em blocos
        QStringList usernamesList = getJsonArrayValues(response, "usernames");
        for(int i = 0; i < usernamesList.size(); ++i) {
            QFrame *f = new QFrame();
            f->setStyleSheet("background-color: #1A1A1A; border: 1px solid #2F2F2F; border-radius: 10px; padding: 5px;");
            QVBoxLayout *fl = new QVBoxLayout(f);
            QLabel *l = new QLabel(usernamesList[i]);
            l->setStyleSheet("color: #00ffea; font-weight: bold;");
            fl->addWidget(l);
            searchResultsLayout->addWidget(f);
        }

        QStringList postsList = getJsonArrayValues(response, "posts");
        for(int j = 0; j < postsList.size(); ++j) {
            QFrame *f = new QFrame();
            f->setStyleSheet("background-color: #252525; border: 1px solid #3F3F3F; border-radius: 10px; padding: 5px;");
            QVBoxLayout *fl = new QVBoxLayout(f);
            QLabel *l = new QLabel(postsList[j]);
            l->setStyleSheet("color: white;");
            l->setWordWrap(true);
            fl->addWidget(l);
            searchResultsLayout->addWidget(f);
        }
        searchResultsLayout->addStretch();
    }

    // MÓDULO 3: VIEW DO CHAT (CHAT GLOBAL E SELEÇÃO DE CHAT DIRETO)
    void showChatView() {
        if(chatUpdateTimer) { chatUpdateTimer->stop(); }
        clearLayout(mainLayout);
        QVBoxLayout *chatMenuLayout = new QVBoxLayout();

        QPushButton *btnGlobalChat = new QPushButton("Chat Global");
        chatMenuLayout->addWidget(btnGlobalChat);
        connect(btnGlobalChat, SIGNAL(clicked()), this, SLOT(showGlobalChatWindow()));

        // Extração de Amigos Cadastrados na conta para canais diretos
        QString payload = "{" + jsonField("username", currentUsername) + "}";
        QString response = requestHTTP(url + "/friends", "POST", payload);
        QStringList friendsList = getJsonArrayValues(response, "friends");

        if(friendsList.isEmpty()) {
            chatMenuLayout->addWidget(new QLabel("Nenhum amigo listado para chats diretos..."));
        } else {
            for(int i = 0; i < friendsList.size(); ++i) {
                QPushButton *btnFriend = new QPushButton(friendsList[i]);
                chatMenuLayout->addWidget(btnFriend);
                btnFriend->setProperty("targetFriend", friendsList[i]);
                connect(btnFriend, SIGNAL(clicked()), this, SLOT(showPrivateChatWindow()));
            }
        }

        mainLayout->addLayout(chatMenuLayout);
        rebuildBottomNavigation(mainLayout, 2);
    }

    // CHAT ASSÍNCRONO GLOBAL (POOLING COM TIMER)
    void showGlobalChatWindow() {
        clearLayout(mainLayout);
        lastGlobalMessageId = 0;

        globalChatScrollArea = new QScrollArea();
        globalChatScrollArea->setWidgetResizable(true);
        QWidget *container = new QWidget();
        globalChatListLayout = new QVBoxLayout(container);
        globalChatScrollArea->setWidget(container);
        mainLayout->addWidget(globalChatScrollArea);

        messageInput = new QLineEdit();
        messageInput->setPlaceholderText(QCoreApplication::translate("chat", "type here"));
        QPushButton *btnSend = new QPushButton(QCoreApplication::translate("global", "send"));
        
        QHBoxLayout *hBox = new QHBoxLayout();
        hBox->addWidget(messageInput);
        hBox->addWidget(btnSend);
        mainLayout->addLayout(hBox);

        QPushButton *btnBack = new QPushButton(QCoreApplication::translate("global", "back"));
        mainLayout->addWidget(btnBack);

        connect(btnBack, SIGNAL(clicked()), this, SLOT(showInitialPage()));
        connect(btnSend, SIGNAL(clicked()), this, SLOT(executeSendGlobalMessage()));
        connect(messageInput, SIGNAL(returnPressed()), btnSend, SLOT(click()));

        // Ativação do pooling assíncrono para captura periódica de mensagens a cada 2 segundos
        if(!chatUpdateTimer) {
            chatUpdateTimer = new QTimer(this);
        }
        disconnect(chatUpdateTimer, SIGNAL(timeout()), 0, 0);
        connect(chatUpdateTimer, SIGNAL(timeout()), this, SLOT(pollGlobalChatMessages()));
        chatUpdateTimer->start(2000);
        pollGlobalChatMessages();
    }

    // CHAT ASSÍNCRONO PRIVADO (POOLING INDIVIDUAL DE CANAIS)
    void showPrivateChatWindow() {
        QPushButton *btn = qobject_cast<QPushButton*>(sender());
        if(!btn) return;
        currentActiveChatTarget = btn->property("targetFriend").toString();

        clearLayout(mainLayout);
        privateChatScrollArea = new QScrollArea();
        privateChatScrollArea->setWidgetResizable(true);
        QWidget *container = new QWidget();
        privateChatListLayout = new QVBoxLayout(container);
        privateChatScrollArea->setWidget(container);
        mainLayout->addWidget(privateChatScrollArea);

        messageInput = new QLineEdit();
        messageInput->setPlaceholderText(QCoreApplication::translate("chat", "type here"));
        QPushButton *btnSend = new QPushButton(QCoreApplication::translate("global", "send"));
        
        QHBoxLayout *hBox = new QHBoxLayout();
        hBox->addWidget(messageInput);
        hBox->addWidget(btnSend);
        mainLayout->addLayout(hBox);

        QPushButton *btnBack = new QPushButton(QCoreApplication::translate("global", "back"));
        mainLayout->addWidget(btnBack);

        connect(btnBack, SIGNAL(clicked()), this, SLOT(showInitialPage()));
        connect(btnSend, SIGNAL(clicked()), this, SLOT(executeSendPrivateMessage()));
        connect(messageInput, SIGNAL(returnPressed()), btnSend, SLOT(click()));

        if(!chatUpdateTimer) {
            chatUpdateTimer = new QTimer(this);
        }
        disconnect(chatUpdateTimer, SIGNAL(timeout()), 0, 0);
        connect(chatUpdateTimer, SIGNAL(timeout()), this, SLOT(pollPrivateChatMessages()));
        chatUpdateTimer->start(2000);
        pollPrivateChatMessages();
    }

public slots:
    void pollGlobalChatMessages() {
        QString payload = "{\"id\":" + QString::number(lastGlobalMessageId) + "}";
        QString response = requestHTTP(url + "/view-global-message", "POST", payload);
        QStringList messagesBlock = getJsonObjectsInArray(response);

        bool added = false;
        for(int i = 0; i < messagesBlock.size(); ++i) {
            QString msg = messagesBlock[i];
            QString senderName = getJsonValue(msg, "sender");
            QString textMsg = getJsonValue(msg, "message");
            int idMsg = getJsonValue(msg, "id").toInt();

            if(idMsg > lastGlobalMessageId) {
                lastGlobalMessageId = idMsg;
            }

            bool isMe = (senderName == currentUsername);
            ChatBubble *bubble = new ChatBubble(textMsg, isMe);
            QHBoxLayout *row = new QHBoxLayout();
            
            if(isMe) { row->addStretch(); row->addWidget(bubble); }
            else { row->addWidget(bubble); row->addStretch(); }

            QWidget *rowW = new QWidget();
            rowW->setLayout(row);
            globalChatListLayout->addWidget(rowW);
            added = true;
        }

        if(added) {
            QTimer::singleShot(50, this, SLOT(scrollGlobalChatDown()));
        }
    }

    void scrollGlobalChatDown() {
        globalChatScrollArea->verticalScrollBar()->setValue(globalChatScrollArea->verticalScrollBar()->maximum());
    }

    void executeSendGlobalMessage() {
        QString txt = messageInput->text();
        if(txt.isEmpty()) return;
        QString payload = "{" + jsonField("sender", currentUsername) + "," + jsonField("message", txt) + "}";
        requestHTTP(url + "/send-global-message", "POST", payload);
        messageInput->clear();
        pollGlobalChatMessages();
    }

    void pollPrivateChatMessages() {
        QString payload = "{" + jsonField("user1", currentUsername) + "," + jsonField("user2", currentActiveChatTarget) + "}";
        QString response = requestHTTP(url + "/view", "POST", payload);
        QStringList messagesBlock = getJsonObjectsInArray(response);

        clearLayout(privateChatListLayout);
        for(int i = 0; i < messagesBlock.size(); ++i) {
            QString msg = messagesBlock[i];
            QString senderName = getJsonValue(msg, "sender");
            QString textMsg = getJsonValue(msg, "message");

            bool isMe = (senderName == currentUsername);
            ChatBubble *bubble = new ChatBubble(textMsg, isMe);
            QHBoxLayout *row = new QHBoxLayout();
            
            if(isMe) { row->addStretch(); row->addWidget(bubble); }
            else { row->addWidget(bubble); row->addStretch(); }

            QWidget *rowW = new QWidget();
            rowW->setLayout(row);
            privateChatListLayout->addWidget(rowW);
        }
        QTimer::singleShot(50, this, SLOT(scrollPrivateChatDown()));
    }

    void scrollPrivateChatDown() {
        privateChatScrollArea->verticalScrollBar()->setValue(privateChatScrollArea->verticalScrollBar()->maximum());
    }

    void executeSendPrivateMessage() {
        QString txt = messageInput->text();
        if(txt.isEmpty()) return;
        QString payload = "{" + jsonField("receiver", currentActiveChatTarget) + "," +
                                jsonField("sender", currentUsername) + "," +
                                jsonField("message", txt) + "}";
        requestHTTP(url + "/send-message", "POST", payload);
        messageInput->clear();
        pollPrivateChatMessages();
    }

    // MÓDULO 4: VIEW DO PERFIL E EDIÇÃO DE BIOGRAFIA
    void showProfileView() {
        clearLayout(mainLayout);
        QVBoxLayout *profileContentLayout = new QVBoxLayout();

        QLabel *lblUser = new QLabel("Usuario ativo: " + currentUsername);
        lblUser->setStyleSheet("font-size: 16px; font-weight: bold; color: white;");
        profileContentLayout->addWidget(lblUser);

        QString bioRes = requestHTTP(url + "/view-profile/?username=" + currentUsername, "GET", "");
        QString extractedBio = getJsonValue(bioRes, "bio");
        if(extractedBio.isEmpty()) extractedBio = "Nenhuma biografia redigida ate o momento.";

        QLabel *lblBio = new QLabel(QCoreApplication::translate("my account", "biography") + ": " + extractedBio);
        lblBio->setWordWrap(true);
        profileContentLayout->addWidget(lblBio);

        QPushButton *btnEdit = new QPushButton(QCoreApplication::translate("my account", "edit"));
        profileContentLayout->addWidget(btnEdit);
        connect(btnEdit, SIGNAL(clicked()), this, SLOT(showEditProfileForm()));

        mainLayout->addLayout(profileContentLayout);
        rebuildBottomNavigation(mainLayout, 3);
    }

    void showEditProfileForm() {
        clearLayout(mainLayout);
        bioInput = new QLineEdit();
        bioInput->setPlaceholderText(QCoreApplication::translate("my account", "biography"));

        QPushButton *btnSave = new QPushButton(QCoreApplication::translate("global", "send"));
        QPushButton *btnBack = new QPushButton(QCoreApplication::translate("global", "back"));

        mainLayout->addWidget(bioInput);
        mainLayout->addWidget(btnSave);
        mainLayout->addWidget(btnBack);

        connect(btnBack, SIGNAL(clicked()), this, SLOT(showInitialPage()));
        connect(btnSave, SIGNAL(clicked()), this, SLOT(executeEditProfileRequest()));
    }

    void executeEditProfileRequest() {
        QString payload = "{" + jsonField("edit-mode", "bio") + "," +
                                jsonField("content", bioInput->text()) + "," +
                                jsonField("username", currentUsername) + "}";
        requestHTTP(url + "/edit", "POST", payload);
        showInitialPage();
    }

    // MÓDULO 5: VIEW DE OPÇÕES (CONFIGURAÇÕES DO SISTEMA, TEMAS, INBOX, LANG E FEDERAÇÕES)
    void showOptionsView() {
        clearLayout(mainLayout);
        QVBoxLayout *optionsContentLayout = new QVBoxLayout();

        QPushButton *btnInbox = new QPushButton(QCoreApplication::translate("main-page", "inbox"));
        QPushButton *btnFriendsMenu = new QPushButton(QCoreApplication::translate("add friends", "friends"));
        QPushButton *btnFederations = new QPushButton(QCoreApplication::translate("configurations", "add federations"));
        QPushButton *btnThemes = new QPushButton(QCoreApplication::translate("configurations", "add theme"));
        QPushButton *btnLang = new QPushButton("Mudar Idioma (Lang)");
        QPushButton *btnChangeUrl = new QPushButton("Mudar Endereco URL Servidor");

        optionsContentLayout->addWidget(btnInbox);
        optionsContentLayout->addWidget(btnFriendsMenu);
        optionsContentLayout->addWidget(btnFederations);
        optionsContentLayout->addWidget(btnThemes);
        optionsContentLayout->addWidget(btnLang);
        optionsContentLayout->addWidget(btnChangeUrl);

        connect(btnInbox, SIGNAL(clicked()), this, SLOT(showInboxPage()));
        connect(btnFriendsMenu, SIGNAL(clicked()), this, SLOT(showFriendsPage()));
        connect(btnFederations, SIGNAL(clicked()), this, SLOT(showFederationsPage()));
        connect(btnThemes, SIGNAL(clicked()), this, SLOT(showThemesPage()));
        connect(btnLang, SIGNAL(clicked()), this, SLOT(showLangPage()));
        connect(btnChangeUrl, SIGNAL(clicked()), this, SLOT(showChangeServerPage()));

        mainLayout->addLayout(optionsContentLayout);
        rebuildBottomNavigation(mainLayout, 4);
    }

    // SUB-SISTEMA: GERENCIADOR DE SOLICITAÇÕES DE AMIZADE (INBOX)
    void showInboxPage() {
        clearLayout(mainLayout);
        QList<QWidget*> notificationWidgets;

        QString payload = "{" + jsonField("username", currentUsername) + "}";
        QString response = requestHTTP(url + "/inbox", "POST", payload);
        
        // Captura de notificações brutas listadas no Array
        QStringList inboxItems = getJsonArrayValues(response, "inbox");

        if(inboxItems.isEmpty()) {
            notificationWidgets.append(new QLabel("Sua caixa de entrada de notificacoes esta vazia."));
        } else {
            for(int i = 0; i < inboxItems.size(); ++i) {
                QLabel *lblItem = new QLabel(inboxItems[i]);
                notificationWidgets.append(lblItem);

                // Como as notificações contêm metadados implícitos do remetente
                QString remitteeUser = inboxItems[i].split(" ").last(); 

                QPushButton *btnAccept = new QPushButton(QCoreApplication::translate("inbox", "accept"));
                QPushButton *btnDenied = new QPushButton(QCoreApplication::translate("inbox", "denied"));

                btnAccept->setProperty("remittee", remitteeUser);
                btnDenied->setProperty("remittee", remitteeUser);

                connect(btnAccept, SIGNAL(clicked()), this, SLOT(handleAcceptFriendship()));
                connect(btnDenied, SIGNAL(clicked()), this, SLOT(handleDeniedFriendship()));

                notificationWidgets.append(btnAccept);
                notificationWidgets.append(btnDenied);
            }
        }

        scroll_area(mainLayout, notificationWidgets);
        QPushButton *btnBack = new QPushButton(QCoreApplication::translate("global", "back"));
        mainLayout->addWidget(btnBack);
        connect(btnBack, SIGNAL(clicked()), this, SLOT(showInitialPage()));
    }

    void handleAcceptFriendship() {
        QString rem = sender()->property("remittee").toString();
        QString payload = "{" + jsonField("receiver", currentUsername) + "," + jsonField("remittee", rem) + "}";
        requestHTTP(url + "/accept", "POST", payload);
        showInboxPage();
    }

    void handleDeniedFriendship() {
        QString rem = sender()->property("remittee").toString();
        QString payload = "{" + jsonField("receiver", currentUsername) + "," + jsonField("remittee", rem) + "}";
        requestHTTP(url + "/denied", "POST", payload);
        showInboxPage();
    }

    // SUB-SISTEMA: ADICIONAR NOVOS AMIGOS
    void showFriendsPage() {
        clearLayout(mainLayout);
        QPushButton *btnAddNewFriend = new QPushButton(QCoreApplication::translate("initial-page", "add friends"));
        QPushButton *btnBack = new QPushButton(QCoreApplication::translate("global", "back"));

        mainLayout->addWidget(btnAddNewFriend);
        mainLayout->addWidget(btnBack);

        connect(btnBack, SIGNAL(clicked()), this, SLOT(showInitialPage()));
        connect(btnAddNewFriend, SIGNAL(clicked()), this, SLOT(showAddFriendForm()));
    }

    void showAddFriendForm() {
        clearLayout(mainLayout);
        usernameInput = new QLineEdit();
        usernameInput->setPlaceholderText(QCoreApplication::translate("global", "username"));
        messageInput = new QLineEdit();
        messageInput->setPlaceholderText(QCoreApplication::translate("add friends", "message"));

        QPushButton *btnSend = new QPushButton(QCoreApplication::translate("global", "send"));
        QPushButton *btnBack = new QPushButton(QCoreApplication::translate("global", "back"));

        mainLayout->addWidget(usernameInput);
        mainLayout->addWidget(messageInput);
        mainLayout->addWidget(btnSend);
        mainLayout->addWidget(btnBack);

        connect(btnBack, SIGNAL(clicked()), this, SLOT(showInitialPage()));
        connect(btnSend, SIGNAL(clicked()), this, SLOT(executeAddFriendRequest()));
    }

    void executeAddFriendRequest() {
        QString payload = "{" + jsonField("receiver", usernameInput->text()) + "," +
                                jsonField("remittee", currentUsername) + "," +
                                jsonField("message", messageInput->text()) + "}";
        requestHTTP(url + "/send-friend", "POST", payload);
        showInitialPage();
    }

    // SUB-SISTEMA: CONFIGURAÇÕES DE FEDERAÇÕES INTERNAS
    void showFederationsPage() {
        clearLayout(mainLayout);
        urlInput = new QLineEdit();
        urlInput->setPlaceholderText("url federacao:");

        QPushButton *btnSave = new QPushButton(QCoreApplication::translate("global", "send"));
        QPushButton *btnBack = new QPushButton(QCoreApplication::translate("global", "back"));

        mainLayout->addWidget(urlInput);
        mainLayout->addWidget(btnSave);
        mainLayout->addWidget(btnBack);

        connect(btnBack, SIGNAL(clicked()), this, SLOT(showInitialPage()));
        connect(btnSave, SIGNAL(clicked()), this, SLOT(executeAddFederation()));
    }

    void executeAddFederation() {
        QString raw = QString::fromStdString(config["FEDERATIONS"]["url"]);
        if(raw.trimmed().isEmpty()) raw = "[]";

        // Adiciona de forma compatível concatenando a string em um Array JSON plano
        raw.replace("]", "");
        if(raw != "[") raw += ",";
        raw += "\"" + urlInput->text() + "\"]";

        config["FEDERATIONS"]["url"] = raw.toStdString();
        saveConfig();
        showInitialPage();
    }

    // SUB-SISTEMA: COMPORTAMENTO E CARREGAMENTO DE QSS (THEMES PAGE)
    void showThemesPage() {
        clearLayout(mainLayout);
        QLabel *lblCurrent = new QLabel("Tema registrado: " + QString::fromStdString(config["THEMES"]["theme"]));
        mainLayout->addWidget(lblCurrent);

        QPushButton *btnSelect = new QPushButton(QCoreApplication::translate("configurations", "add theme"));
        QPushButton *btnBack = new QPushButton(QCoreApplication::translate("global", "back"));

        mainLayout->addWidget(btnSelect);
        mainLayout->addWidget(btnBack);

        connect(btnBack, SIGNAL(clicked()), this, SLOT(showInitialPage()));
        connect(btnSelect, SIGNAL(clicked()), this, SLOT(executeThemeSelectionDialog()));
    }

    void executeThemeSelectionDialog() {
        QString filePath = QFileDialog::getOpenFileName(NULL, "Selecionar Tema QSS", QDir::homePath(), "Estilos (*.qss *.txt)");
        if(filePath.isEmpty()) return;

        QFile file(filePath);
        if(!file.open(QIODevice::ReadOnly | QIODevice::Text)) return;
        QTextStream in(&file);
        QString styleContent = in.readAll();
        file.close();

        config["THEMES"]["theme"] = filePath.toStdString();
        qApp->setStyleSheet(styleContent);
        saveConfig();
        showInitialPage();
    }

    // SUB-SISTEMA: GERENCIAMENTO DE INTERNACIONALIZAÇÃO (LANG PAGE)
    void showLangPage() {
        clearLayout(mainLayout);
        QPushButton *btnPt = new QPushButton("Português (pt-br)");
        QPushButton *btnEn = new QPushButton("English (en)");
        QPushButton *btnBack = new QPushButton(QCoreApplication::translate("global", "back"));

        mainLayout->addWidget(btnPt);
        mainLayout->addWidget(btnEn);
        mainLayout->addWidget(btnBack);

        connect(btnBack, SIGNAL(clicked()), this, SLOT(showInitialPage()));
        connect(btnPt, SIGNAL(clicked()), this, SLOT(setLanguagePt()));
        connect(btnEn, SIGNAL(clicked()), this, SLOT(setLanguageEn()));
    }

    void setLanguagePt() { config["LANG"]["lang"] = "pt_br"; saveConfig(); showInitialPage(); }
    void setLanguageEn() { config["LANG"]["lang"] = "en"; saveConfig(); showInitialPage(); }

    // SUB-SISTEMA: CONFIGURAÇÃO DO ENDEREÇO DO SERVIDOR API TARGET
    void showChangeServerPage() {
        clearLayout(mainLayout);
        urlInput = new QLineEdit();
        urlInput->setText(url);

        QPushButton *btnSave = new QPushButton("Salvar Novo Endereco");
        QPushButton *btnBack = new QPushButton(QCoreApplication::translate("global", "back"));

        mainLayout->addWidget(urlInput);
        mainLayout->addWidget(btnSave);
        mainLayout->addWidget(btnBack);

        connect(btnBack, SIGNAL(clicked()), this, SLOT(showInitialPage()));
        connect(btnSave, SIGNAL(clicked()), this, SLOT(executeChangeServerUrl()));
    }

    void executeChangeServerUrl() {
        config["SERVER"]["url"] = urlInput->text().toStdString();
        saveConfig();
        url = urlInput->text();
        showInitialPage();
    }

    // PÁGINA PUNITIVA: TRAVA SE USUÁRIO FOR BANNED PELO BACKEND
    void triggerBannedPage(QString reason) {
        clearLayout(mainLayout);
        QLabel *lblBanned = new QLabel(QCoreApplication::translate("banned", "bannedText"));
        QFont f("Arial", 22, QFont::Bold);
        lblBanned->setFont(f);
        lblBanned->setStyleSheet("color: red;");
        
        QLabel *lblReason = new QLabel("Razao: " + reason);
        QPushButton *btnExit = new QPushButton(QCoreApplication::translate("banned", "exit"));

        mainLayout->addWidget(lblBanned);
        mainLayout->addWidget(lblReason);
        mainLayout->addWidget(btnExit);

        connect(btnExit, SIGNAL(clicked()), qApp, SLOT(quit()));
    }

private:
    // Auxiliar Técnico para reconstituir a Barra de Abas Inferiores fixa de forma desacoplada
    void rebuildBottomNavigation(QVBoxLayout *targetLayout, int activeIndex) {
        QWidget *bottomBar = new QWidget(centralWidget);
        bottomBar->setFixedHeight(65);
        bottomBar->setStyleSheet("background: #111; border-top: 1px solid #222;");
        QHBoxLayout *barLayout = new QHBoxLayout(bottomBar);
        barLayout->setContentsMargins(2, 2, 2, 2);

        QPushButton *btnHome = new QPushButton(bottomBar);
        QPushButton *btnSearch = new QPushButton(bottomBar);
        QPushButton *btnChat = new QPushButton(bottomBar);
        QPushButton *btnProfile = new QPushButton(bottomBar);
        QPushButton *btnOptions = new QPushButton(bottomBar);

        btnHome->setIcon(QIcon(":/assets/home.png"));
        btnSearch->setIcon(QIcon(":/assets/search.png"));
        btnChat->setIcon(QIcon(":/assets/chat.png"));
        btnProfile->setIcon(QIcon(":/assets/account.png"));
        btnOptions->setIcon(QIcon(":/assets/options.png"));

        QString styleActive = "border: none; background: #222; color: #00ffea; padding: 5px;";
        QString styleInactive = "border: none; background: transparent; color: white; padding: 5px;";

        btnHome->setStyleSheet(activeIndex == 0 ? styleActive : styleInactive);
        btnSearch->setStyleSheet(activeIndex == 1 ? styleActive : styleInactive);
        btnChat->setStyleSheet(activeIndex == 2 ? styleActive : styleInactive);
        btnProfile->setStyleSheet(activeIndex == 3 ? styleActive : styleInactive);
        btnOptions->setStyleSheet(activeIndex == 4 ? styleActive : styleInactive);

        barLayout->addWidget(btnHome); barLayout->addWidget(btnSearch);
        barLayout->addWidget(btnChat); barLayout->addWidget(btnProfile);
        barLayout->addWidget(btnOptions);

        connect(btnHome, SIGNAL(clicked()), this, SLOT(showFeedView()));
        connect(btnSearch, SIGNAL(clicked()), this, SLOT(showSearchView()));
        connect(btnChat, SIGNAL(clicked()), this, SLOT(showChatView()));
        connect(btnProfile, SIGNAL(clicked()), this, SLOT(showProfileView()));
        connect(btnOptions, SIGNAL(clicked()), this, SLOT(showOptionsView()));

        targetLayout->addWidget(bottomBar, 0);
    }
};

// ============================================================================
// METODO DE ENTRADA DO SISTEMA OPERACIONAL (MAIN FUNCTION)
// ============================================================================
int main(int argc, char *argv[]) {
    QApplication app(argc, argv);
    
    // Tratamento e injeção do dicionário de arquivos de tradução compilados do Qt (.qm)
    QTranslator *translator = new QTranslator(&app);
    if (translator->load(":/translations/pt-br-main-page.qm")) {
        app.installTranslator(translator);
    }

    // Atribuição de Tema Legado Compatível com Processamento do Gingerbread
    app.setStyle(QStyleFactory::create("Plastique"));
    app.setWindowIcon(QIcon(":/assets/icon.png"));

    loadConfig();
    loadStyle();

    if (config["SERVER"]["url"].empty()) {
        config["SERVER"]["url"] = "http://linkaproject.pythonanywhere.com";
        saveConfig();
    }

    // Instanciação e Inicialização da Janela Nativa de Exibição de Carga (Splash Screen)
    QPixmap pixmap(":/assets/icon.png");
    QSplashScreen *splash = new QSplashScreen(pixmap);
    splash->show();

    QMainWindow mainWindow;
    mainWindow.setWindowTitle("Linka Mobile Engine");
    mainWindow.resize(320, 480); // Foco em aparelhos legados MDPI/HVGA

    QWidget *centralWidget = new QWidget(&mainWindow);
    QVBoxLayout *mainLayout = new QVBoxLayout(centralWidget);
    mainLayout->setContentsMargins(8, 8, 8, 8);
    mainLayout->setSpacing(6);
    mainWindow.setCentralWidget(centralWidget);

    // Inicialização do Controlador de Estados da Interface Gráfica
    LinkaApplicationController *controller = new LinkaApplicationController(centralWidget, mainLayout, splash);

    // Validação Síncrona Inicial de Persistência do Token da Sessão do Usuário (Fast-Login)
    QString token = QString::fromStdString(config["FAST-LOGIN"]["token_session"]);
    int statusCode = 0;
    QString payloadValidation = "{\"token\":\"" + token + "\"}";
    
    QString validationResponse = requestHTTP(
        QString::fromStdString(config["SERVER"]["url"]) + "/valide",
        "POST",
        payloadValidation,
        4000,
        &statusCode,
        false
    );

    if (token.isEmpty() || (statusCode != 200 && statusCode != 201) || validationResponse == "BANNED") {
        controller->showLoginPage();
    } else {
        controller->showInitialPage();
    }

    mainWindow.show();
    int execResult = app.exec();
    
    delete controller;
    return execResult;
}

#include "main.moc" // Macro de expansão do Meta-Object Compiler obrigatória para compilar classes Q_OBJECT dentro de arquivos .cpp únicos