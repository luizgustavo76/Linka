#include <QFileDialog>
#include <QTextStream>
#include <QMessageBox>
#include <QAction>
#include <QPointer>
#include <QApplication>
#include <QWidget>
#include <QPushButton>
#include <QScroller>
#include <QVBoxLayout>
#include <QLineEdit>
#include <QDebug>
#include <functional>
#include <QNetworkAccessManager>
#include <QNetworkRequest>
#include <QNetworkReply>
#include <QHttpMultiPart>
#include <QHttpPart>
#include <QScreen>
#include <QStyleFactory>
#include <QDesktopServices>
#include <QUrl>
#include <QVector>
#include <QSplashScreen>
#include <QStandardPaths>
#include <QJsonDocument>
#include <QMenu>
#include <QPlainTextEdit>
#include <iostream>
#include <QJsonObject>
#include <QLabel>
#include <QUrl>
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
#include <QString>

#if defined(Q_OS_ANDROID)
    #include <QtAndroidExtras/QAndroidJniObject>
    #include <QtAndroidExtras/QAndroidJniEnv>
#else
    #include <QSystemTrayIcon>
    #include <QIcon>
#endif

void sendSystemNotification(const QString &title, const QString &message) {
#if defined(Q_OS_ANDROID)
    // Get the Android Context
    QAndroidJniObject context = QAndroidJniObject::callStaticObjectMethod(
        "org/qtproject/qt5/android/QtActivity", 
        "isActive", 
        "()Z"
    ) ? QAndroidJniObject::callStaticObjectMethod(
            "org/qtproject/qt5/android/QtActivity", 
            "object", 
            "()Lorg/qtproject/qt5/android/QtActivity;"
        ) : QAndroidJniObject();

    if (!context.isValid()) return;

    QAndroidJniObject channelId = QAndroidJniObject::fromString("linka_channel_id");
    QAndroidJniObject channelName = QAndroidJniObject::fromString("Linka Notifications");

    // Build the notification
    QAndroidJniObject builder("android/support/v4/app/NotificationCompat$Builder",
                              "(Landroid/content/Context;Ljava/lang/String;)V",
                              context.object<jobject>(),
                              channelId.object<jstring>());

    jint defaultIconId = QAndroidJniObject::getStaticField<jint>("android/R$drawable", "sym_def_app_icon");
    
    builder.callObjectMethod("setContentTitle", 
                             "(Ljava/lang/CharSequence;)Landroid/support/v4/app/NotificationCompat$Builder;", 
                             QAndroidJniObject::fromString(title).object<jstring>());
                             
    builder.callObjectMethod("setContentText", 
                             "(Ljava/lang/CharSequence;)Landroid/support/v4/app/NotificationCompat$Builder;", 
                             QAndroidJniObject::fromString(message).object<jstring>());
                             
    builder.callMethod<QAndroidJniObject>("setSmallIcon", 
                                          "(I)Landroid/support/v4/app/NotificationCompat$Builder;", 
                                          defaultIconId);

    // Get Notification Manager
    QAndroidJniObject notificationServiceString = QAndroidJniObject::getStaticObjectField(
        "android/content/Context", 
        "NOTIFICATION_SERVICE", 
        "Ljava/lang/String;"
    );
    
    QAndroidJniObject notificationManager = context.callObjectMethod(
        "getSystemService", 
        "(Ljava/lang/String;)Ljava/lang/Object;", 
        notificationServiceString.object<jobject>()
    );

    // Register Notification Channel
    jint importanceDefault = 3; 
    QAndroidJniObject notificationChannel("android/app/NotificationChannel",
                                          "(Ljava/lang/String;Ljava/lang/CharSequence;I)V",
                                          channelId.object<jstring>(),
                                          channelName.object<jstring>(),
                                          importanceDefault);

    notificationManager.callMethod<void>("createNotificationChannel", 
                                         "(Landroid/app/NotificationChannel;)V", 
                                         notificationChannel.object<jobject>());

    // Trigger Notification
    QAndroidJniObject notification = builder.callObjectMethod("build", "()Landroid/app/Notification;");
    jint notificationId = 1001; 
    
    notificationManager.callMethod<void>("notify", 
                                         "(ILandroid/app/Notification;)V", 
                                         notificationId, 
                                         notification.object<jobject>());

    // ==========================================
    // DESKTOP IMPLEMENTATION (Linux, Windows, macOS)
    // ==========================================
#else
    // Check if system tray is available on the host OS
    if (!QSystemTrayIcon::isSystemTrayAvailable()) {
        return; 
    }

    // Static pointer so we don't recreate the tray icon every time a message arrives
    static QSystemTrayIcon *trayIcon = nullptr;
    
    if (!trayIcon) {
        trayIcon = new QSystemTrayIcon();
        // Fallback icon. Make sure to use your actual resource path or system theme icon
        trayIcon->setIcon(QIcon::fromTheme("dialog-information", QIcon(":/icons/logo.png")));
        trayIcon->show();
    }

    // Show the notification toast
    trayIcon->showMessage(
        title, 
        message, 
        QSystemTrayIcon::Information, 
        5000
    );
#endif
}
void renderPostImage(QString urlImage, QBoxLayout *postLayout) {
    QLabel *imageLabel = new QLabel();
    imageLabel->setAlignment(Qt::AlignCenter);
    
    imageLabel->setContextMenuPolicy(Qt::NoContextMenu);
    imageLabel->setText("Loading image...");
    postLayout->addWidget(imageLabel);

    QNetworkAccessManager *manager = new QNetworkAccessManager();
    QNetworkRequest request((QUrl(urlImage)));
    QNetworkReply *reply = manager->get(request);

    QObject::connect(reply, &QNetworkReply::finished, [=]() {
        if (reply->error() == QNetworkReply::NoError) {
            QByteArray dataImage = reply->readAll();
            QPixmap pixmap;
            pixmap.loadFromData(dataImage);

            if (!pixmap.isNull()) {
                QPixmap imagemRedimension = pixmap.scaledToWidth(400, Qt::SmoothTransformation);
                
                imageLabel->setText("");
                imageLabel->setPixmap(imagemRedimension);
            } else {
                imageLabel->setText("Error: invalid format");
            }
        } else {
            imageLabel->setText("Error in image download");
            qDebug() << urlImage;
        }
        reply->deleteLater();
        manager->deleteLater();
    });
}
void renderAvatarImage(QString urlImage, QBoxLayout *postLayout) {
    QLabel *imageLabel = new QLabel();
    imageLabel->setAlignment(Qt::AlignCenter);
    imageLabel->setFixedSize(50, 50);
    imageLabel->setScaledContents(true);
    
    imageLabel->setContextMenuPolicy(Qt::NoContextMenu);
    imageLabel->setText("...");
    postLayout->addWidget(imageLabel);

    QNetworkAccessManager *manager = new QNetworkAccessManager();
    QNetworkRequest request((QUrl(urlImage)));
    QNetworkReply *reply = manager->get(request);

    QObject::connect(reply, &QNetworkReply::finished, [=]() {
        if (reply->error() == QNetworkReply::NoError) {
            QByteArray dataImage = reply->readAll();
            QPixmap pixmap;
            pixmap.loadFromData(dataImage);

            if (!pixmap.isNull()) {
                QPixmap imagemRedimension = pixmap.scaled(50, 50, Qt::KeepAspectRatioByExpanding, Qt::SmoothTransformation);
                
                imageLabel->setText("");
                imageLabel->setPixmap(imagemRedimension);
            } else {
                imageLabel->setText("Err");
            }
        } else {
            imageLabel->setText("Err");
            qDebug() << urlImage;
        }
        reply->deleteLater();
        manager->deleteLater();
    });
}
void fadeTransition(QWidget *widget)
{
    QGraphicsOpacityEffect *effect = new QGraphicsOpacityEffect(widget);
    widget->setGraphicsEffect(effect);

    QPropertyAnimation *anim = new QPropertyAnimation(effect, "opacity");
    anim->setDuration(300);
    anim->setStartValue(0);
    anim->setEndValue(1);

    anim->start(QAbstractAnimation::DeleteWhenStopped);
}
class ChatBubble : public QWidget {
public:
    ChatBubble(QString text, bool isMe, QWidget *parent = nullptr)
        : QWidget(parent), message(text), mine(isMe)
    {
        // Aumentado o limite máximo para o balão ocupar mais espaço horizontal
        setMaximumWidth(550);
        setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Fixed);
    };

protected:
    void paintEvent(QPaintEvent *event) override {
        QPainter painter(this);
        painter.setRenderHint(QPainter::Antialiasing);

        QRect bubbleRect = rect().adjusted(10, 5, -10, -5);

        QColor bubbleColor;
        QColor textColor;

        if (mine) {
            bubbleColor = QColor(173, 216, 230); // Azul-bebê (Light Blue)
            textColor = QColor(30, 30, 30);      // Texto escuro para contrastar no azul claro
        } else {
            bubbleColor = QColor(255, 255, 255); // Branco puro
            textColor = QColor(30, 30, 30);      // Texto escuro para contrastar no branco
        }

        painter.setBrush(bubbleColor);
        painter.setPen(Qt::NoPen);

        // Raio reduzido de 18 para 6 para deixar o balão bem mais retangular
        painter.drawRoundedRect(bubbleRect, 6, 6);

        painter.setPen(textColor);
        painter.setFont(QFont("Arial", 11));

        // Ajuste do texto acompanhando o novo padding
        painter.drawText(
            bubbleRect.adjusted(18, 12, -18, -12),
            Qt::TextWordWrap,
            message
        );
    };

    QSize sizeHint() const override {
        QFontMetrics fm(QFont("Arial", 11));
        // Largura base do texto aumentada para 450 para o retângulo espalhar mais na tela
        QRect r = fm.boundingRect(0, 0, 450, 1000, Qt::TextWordWrap, message);
        
        // Adicionado mais padding (+80 na largura, +40 na altura) para o balão parecer maior e mais robusto
        return QSize(r.width() + 80, r.height() + 40);
    };

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
std::map<std::string, std::map<std::string, std::string>> config;
QString configPath() {
    QString dirPath = QStandardPaths::writableLocation(QStandardPaths::AppDataLocation);
    QDir().mkpath(dirPath);
    return dirPath + "/config-login.cfg";
};
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

    QJsonObject json;
    json["username"] = username;
    json["password"] = password;

    QNetworkRequest request;
    request.setUrl(QUrl(url + "/new-session"));

    request.setHeader(
        QNetworkRequest::ContentTypeHeader,
        "application/json"
    );

    QByteArray data =
        QJsonDocument(json).toJson();

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
        obj["token"].toString();

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
    
    QByteArray data = QByteArray::fromStdString(newTokenJson.dump());
    
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
        reply = manager.sendCustomRequest(request, "DELETE", jsonData);
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
    if (code == 403){
        renoveToken();
    };
    // Lê a resposta bruta do servidor
    QString response = reply->readAll();
    reply->deleteLater();
    
    return response;
}


QString requestMultipart(const QString &url,
                         const QString &filePath,
                         int timeoutMs,
                         int *statusCode)
{
    QNetworkAccessManager manager;
    QNetworkRequest request;
    request.setUrl(QUrl(url));

    // Carrega o token de sessão idêntico à sua função original
    loadConfig();
    QString token = QString::fromStdString(config["FAST-LOGIN"]["token_session"]);
    if (!token.isEmpty()) {
        request.setRawHeader(
            "Authorization", 
            QString("Bearer %1").arg(token).toUtf8()
        );
    }

    // Prepara o arquivo binário
    QFile *file = new QFile(filePath);
    if (!file->open(QIODevice::ReadOnly)) {
        if (statusCode) *statusCode = -1;
        delete file;
        return "ERROR: Could not open file";
    }

    // Cria o esqueleto do formulário multipart
    QHttpMultiPart *multiPart = new QHttpMultiPart(QHttpMultiPart::FormDataType);
    QHttpPart imagePart;
    QFileInfo fileInfo(filePath);
    
    // Define a chave exatamente como "image" para o seu Flask receber
    imagePart.setHeader(QNetworkRequest::ContentDispositionHeader, 
        QVariant(QString("form-data; name=\"image\"; filename=\"%1\"").arg(fileInfo.fileName())));
    imagePart.setBodyDevice(file);
    file->setParent(multiPart); 
    multiPart->append(imagePart);

    // Dispara o POST com o arquivo
    QNetworkReply *reply = manager.post(request, multiPart);
    multiPart->setParent(reply); 

    // O mesmo loop de eventos síncrono que você já usa e confia
    QEventLoop loop;
    QTimer timer;
    timer.setSingleShot(true);
    
    QObject::connect(&timer, &QTimer::timeout, &loop, &QEventLoop::quit);
    QObject::connect(reply, &QNetworkReply::finished, &loop, &QEventLoop::quit);
    
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
    if (code == 403){
        renoveToken(); // Mantive sua lógica de renovar token
    }
    
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
    QCoreApplication::setAttribute(Qt::AA_EnableHighDpiScaling);
    QCoreApplication::setAttribute(Qt::AA_UseHighDpiPixmaps);
    #endif
    QString url = QString::fromStdString(config["SERVER"]["url"]);
    QApplication app(argc, argv);
    QTranslator *translator = new QTranslator(&app);
    loadConfig();
    QString current_version = "1.0";
    QString username = QString::fromStdString(config["FAST-LOGIN"]["username"]);
    QString token_session = QString::fromStdString(config["FAST-LOGIN"]["token_session"]);
    
    if (token_session.isEmpty()){
        QJsonObject json_token;
        QString response = requestHTTP(
            url + "/new-session",
            "POST",
            json_token
        );
        QByteArray byteArray = response.toUtf8();
        QJsonDocument doc = QJsonDocument::fromJson(byteArray);
        QJsonObject jsonObject = doc.object();
        QString new_token = jsonObject["token"].toString();
        config["FAST-LOGIN"]["token_session"] = new_token.toStdString();
        saveConfig();
    }
    
    if (config["LANG"]["lang"] == "pt-br"){
        if (translator->load(":/translations/pt-br-main-page.qm")) {
            app.installTranslator(translator);
        }
    };    
    
    app.setStyle(QStyleFactory::create("breeze"));
    loadConfig();
    loadStyle();
    
    for (auto &sec : config) {
        std::cout << "[" << sec.first << "]\n";
        for (auto &kv : sec.second) {
            std::cout << "  " << kv.first << " = " << kv.second << "\n";
        }
    }
    
    if (url.isEmpty())
    {
        config["SERVER"]["url"] = "http://127.0.0.1:5000";
        url = QString::fromStdString(config["SERVER"]["url"]);
        saveConfig();
    }
    qDebug() << "url" << url;   
    
    QFont fonteGlobal = app.font();
    app.setFont(fonteGlobal);
    QMainWindow window;
    app.setWindowIcon(QIcon(":/assets/icon.png"));
    QPixmap pixmap(":/assets/icon.png");
    QSplashScreen splash(pixmap);
    splash.show();
    window.setWindowTitle("Linka Mobile");
    QWidget *central = new QWidget(&window);
    QVBoxLayout *layout = new QVBoxLayout(central);
    QLabel *header_linka = new QLabel();
    QPixmap banner(":/assets/linka_app_login_banner.png");
    header_linka->setPixmap(banner.scaled(400, 200, Qt::KeepAspectRatio, Qt::SmoothTransformation));
    header_linka->setMinimumSize(400, 200); 
    header_linka->setAlignment(Qt::AlignCenter);
    layout->addWidget(header_linka);
    layout->setContentsMargins(30, 30, 30, 30);
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
    QString new_group_text = QCoreApplication::translate("chat", "New group");
    QString name_group_text = QCoreApplication::translate("chat", "name group");
    QString description_text = QCoreApplication::translate("chat", "description");
    QString newer_text = QCoreApplication::translate("feed", "newer");
    QString trending_text = QCoreApplication::translate("feed", "trending");
    QString federations_text = QCoreApplication::translate("feed", "federations");
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
    std::function<void(QString)> editAccount;
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
    std::function<void()> logout;
    std::function<void()> newGroupPage;
    std::function<void(QString, QString)> newGroupRequest;
    std::function<void()> new_chat;
    std::function<QJsonObject()> viewGroupsRequest;
    std::function<void()> trendingFeed;
    std::function<void(QString)> renderBottomBar;
    std::function<void(QString, QString)> profilePicturePage;
    std::function<void(QBoxLayout*, QString)> viewProfilePicture;
    loginPage = [&](){
        clearLayout(layout);
        fadeTransition(central);
        QIcon *banner_image = new QIcon(":/assets/linka_app_login_banner.png");
        QLabel *banner_login = new QLabel();
        banner_login->setPixmap(banner_image->pixmap(QSize(400, 200)));
        banner_login->setAlignment(Qt::AlignCenter);
        QHBoxLayout *loginButtons = new QHBoxLayout();
        QPushButton *signinPage_button = new QPushButton(signup_text);
        QPushButton *signupPage_button = new QPushButton(signin_text);
        QPushButton *change_server_button = new QPushButton();
        signinPage_button->setProperty("class", "tab-button");
        signupPage_button->setProperty("class", "tab-button");
        signinPage_button->setProperty("active", true);
        signupPage_button->setProperty("active", false);
        loginButtons->addWidget(signinPage_button);
        loginButtons->addWidget(signupPage_button);
        layout->addLayout(loginButtons);
        QLineEdit *usernameEntry = new QLineEdit();
        QLineEdit *passwordEntry = new QLineEdit();
        usernameEntry->setPlaceholderText(username_text);
        passwordEntry->setPlaceholderText(password_text);
        QPushButton *send_button = new QPushButton(send_text);
        layout->addWidget(usernameEntry);
        layout->addWidget(passwordEntry);
        layout->addWidget(send_button);
        QObject::connect(signinPage_button, &QPushButton::clicked, [=](){
            signupPage();
        });
        QObject::connect(signupPage_button, &QPushButton::clicked, [=](){
            signinPage();
        });
        QObject::connect(send_button, &QPushButton::clicked, [=, &token_session]() mutable {
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
                QLabel *error_label = new QLabel("Username or password incorrect!");
                layout->addWidget(error_label);
            };
        });
        QPushButton *discordButton = new QPushButton();
        discordButton->setIcon(QIcon(":/assets/discord.png"));
        
        QPushButton *redditButton = new QPushButton();
        redditButton->setIcon(QIcon(":/assets/reddit.png"));
        
        // 1. Reduza o tamanho dos ícones para algo realista em telas de celular
        // Em vez de 200 de largura, use tamanhos quadrados ou mais compactos para não estourar
        discordButton->setIconSize(QSize(120, 40)); 
        redditButton->setIconSize(QSize(120, 40));

        // 2. O SEGREDO: Trave a largura máxima do BOTÃO para ele não crescer além disso
        discordButton->setMaximumWidth(130);
        redditButton->setMaximumWidth(130);
        
        QHBoxLayout *layoutHorizontal = new QHBoxLayout();
        layoutHorizontal->addSpacerItem(new QSpacerItem(0, 0, QSizePolicy::Expanding, QSizePolicy::Minimum));
        layoutHorizontal->addWidget(discordButton);
        layoutHorizontal->addWidget(redditButton);
        
        // Adiciona um spacer na direita também para centralizar os dois botões bonitinho no meio da tela
        layoutHorizontal->addSpacerItem(new QSpacerItem(0, 0, QSizePolicy::Expanding, QSizePolicy::Minimum));
        
        layout->addSpacerItem(new QSpacerItem(0, 0, QSizePolicy::Minimum, QSizePolicy::Expanding));
        layout->addLayout(layoutHorizontal);
        QObject::connect(discordButton, &QPushButton::clicked, [=](){
            QDesktopServices::openUrl(QUrl("https://discord.gg/bhru6SWcvC"));
        });
        QObject::connect(redditButton, &QPushButton::clicked, [=](){
            QDesktopServices::openUrl(QUrl("https://www.reddit.com/r/LinkaProject/"));
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
    QString min_ver_str = jsonObject["minim-version"].toString().trimmed();
    QString linkUpdate = jsonObject["url"].toString();
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
        window.showMaximized();
        return app.exec(); 
    }
    // validation of token
    QString token = QString::fromStdString(config["FAST-LOGIN"]["token_session"]);
    QJsonObject json_valide;
    json_valide["token"] = token;
    int status_code;
    requestHTTP(
        url + "/valide-session",
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
        post["username"] = username;
        post["text_post"] = text;
        QDateTime actualHour = QDateTime::currentDateTime();
        QString formatedHour = actualHour.toString("yyyy-MM-dd HH:mm:ss");
        post["datetime"] = formatedHour;
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
        QPlainTextEdit *textPost = new QPlainTextEdit(); 
        textPost->setPlaceholderText(text_post);
        textPost->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Preferred);
        textPost->setMinimumHeight(50);
        QPushButton *imageButton = new QPushButton();
        QIcon *imageIcon = new QIcon(":/assets/image_icon.png");
        imageButton->setIcon(*imageIcon);
        QPushButton *sendButton = new QPushButton(send_text);
        QPushButton *backButton = new QPushButton(back_text);
        QString *urlImage = new QString("");
        layout->addWidget(textPost);
        layout->addWidget(imageButton);
        layout->addWidget(sendButton);
        layout->addWidget(backButton);
        QObject::connect(imageButton, &QPushButton::clicked, [=](){
            QString filePath = QFileDialog::getOpenFileName(
                nullptr, 
                "Select a image", 
                "", 
                "Images (*.png *.jpg *.jpeg *.webp)"
            );
            
            if (!filePath.isEmpty()) {
                int statusCode = 0;
                QString response = requestMultipart(
                    url + "/upload-image", 
                    filePath, 
                    10000,
                    &statusCode
                );
                QJsonDocument doc = QJsonDocument::fromJson(response.toUtf8());
                QJsonObject obj = doc.object();
                
                *urlImage = obj["image_url"].toString();
                
                qDebug() << response;
                qDebug() << *urlImage; 
            }
        });

        QObject::connect(sendButton, &QPushButton::clicked, [=](){
            if (!urlImage->isEmpty()){
                new_post_request(textPost->toPlainText() + "\n" + "[IMAGE]" + *urlImage + "\n", username);
            } else {
                new_post_request(textPost->toPlainText(), username); 
            }
            delete urlImage; 
        });
        
        QObject::connect(backButton, &QPushButton::clicked, [=](){
            initialPage();
        });
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
                QLabel *notification_inbox = new QLabel(inbox_json[i].toArray()[b].toString());
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
                initialPage();
        });
        renderBottomBar("options");
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

            config["FEDERATIONS"]["url"] =
                newDoc.toJson(QJsonDocument::Compact).toStdString();

            saveConfig();

            qDebug() << "Depois de salvar:" << QString::fromStdString(config["FEDERATIONS"]["url"]);
        });
        renderBottomBar("options");
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
                QTimer::singleShot(0, [&](){
                    QString filePath = QFileDialog::getOpenFileName(
                        nullptr,
                        "Select a theme",
                        QDir::homePath(),
                        "All the files (*.*)"
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
                    if (filePath.contains(".json") || filePath.contains(".jsn")) {
                        QJsonDocument inputDoc = QJsonDocument::fromJson(content.toUtf8());
                        if (inputDoc.isNull()) {
                            qDebug() << "Erro: O conteúdo do arquivo de tema não é um JSON válido!";
                            return;
                        }
                        QJsonObject inputJsonObj = inputDoc.object();

                        QJsonObject theme_payload;
                        theme_payload["input"] = inputJsonObj; 
                        theme_payload["output"] = "qss";

                        QString response_theme = requestHTTP(
                            url + "/convert-theme",
                            "POST",
                            theme_payload
                        );

                        if (!response_theme.isEmpty() && !response_theme.contains("Error")) {
                            
                            qApp->setStyleSheet(response_theme);
                        }
                    }else{
                        qApp->setStyleSheet(content);
                    }
                    config["THEMES"]["theme"] = filePath.toStdString();
                    saveConfig();
                });
        });
        renderBottomBar("options");
    };
    sentFriendRequest = [&](QString receiver){
        QJsonObject json_friends;
        json_friends["receiver"] = receiver;
        json_friends["remittee"] = username;
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
        json_friends["friend"] = receiver;
        json_friends["username"] = username;
        int status_code = 0;
        requestHTTP(
            url + "/unfriend",
            "POST",
            json_friends,
            10000,
            &status_code
        );
    };
    
    otherProfilePage = [&](QString usernameProfile){ // Mantém o [&] para pegar o layout e as variáveis de fora
        clearLayout(layout);
        
        // 1. Elementos principais da UI
        QLabel *titleUsername = new QLabel(usernameProfile);
        titleUsername->setStyleSheet("font-size: 16px; font-weight: bold; color: #333333;");
        
        QLabel *biography = new QLabel(); // Começa vazio
        
        QPushButton *sentAFriend = new QPushButton(sent_friend_text);
        QPushButton *unFriend = new QPushButton(un_friend_text);
        QPushButton *back_button = new QPushButton(back_text);

        // 2. Requisição HTTP da Bio (Atualizando o texto na ordem certa)
        QString response_bio = requestHTTP(url + "view_profile/" + usernameProfile, "GET", QJsonObject());
        QJsonDocument doc_bio = QJsonDocument::fromJson(response_bio.toUtf8());
        QJsonObject json_response_bio = doc_bio.object();
        
        QString bio = json_response_bio["bio"].toString();
        biography->setText(bio); // Agora o texto entra no label de verdade

        // 3. Requisição de Amigos e Lógica de Checagem Sem Duplicar Botão
        QJsonObject isFriend;
        isFriend["username"] = username;
        
        QString response = requestHTTP(url + "/friends", "POST", isFriend);
        QJsonDocument doc = QJsonDocument::fromJson(response.toUtf8());
        QJsonObject json_response = doc.object();
        QJsonArray friendsArray = json_response["friends"].toArray();
        
        bool jaEAmigo = false; 
        
        for (int i = 0; i < friendsArray.size(); ++i) {
            QJsonArray subArray = friendsArray[i].toArray();
            if (subArray.size() >= 2) {
                QString friendCheck1 = subArray[0].toString();
                QString friendCheck2 = subArray[1].toString();
                
                if (friendCheck1 == usernameProfile || friendCheck2 == usernameProfile) {
                    jaEAmigo = true;
                    break; 
                }
            }
        }

        layout->addWidget(titleUsername);
        layout->addWidget(biography);

        if (jaEAmigo) {
            layout->addWidget(unFriend);
            
            QObject::connect(unFriend, &QPushButton::clicked, [=](){
                sentUnFriendRequest(usernameProfile);
            });
            
            delete sentAFriend;
        } else {
            layout->addWidget(sentAFriend);
            
            QObject::connect(sentAFriend, &QPushButton::clicked, [=](){
                sentFriendRequest(usernameProfile);
            });
            
            delete unFriend;    
        }

        layout->addWidget(back_button);
        QObject::connect(back_button, &QPushButton::clicked, [=](){
            initialPage();
        });
        
        renderBottomBar("profile");
    };
    changeLangPage = [&](){
        clearLayout(layout);
        QMenu *optionsMenu = new QMenu();
        QAction *pt_br = optionsMenu->addAction("pt-br");
        QAction *en = optionsMenu->addAction("en");
        QObject::connect(pt_br, &QAction::triggered, [=](){
            loadConfig();
            config["LANG"]["lang"] = "pt_br";
            saveConfig();
            initialPage();
        });
        QObject::connect(en, &QAction::triggered, [=](){
            loadConfig();
            config["LANG"]["lang"] = "en";
            saveConfig();
            initialPage();
        });
        QPushButton *backButton = new QPushButton(back_text);
        QObject::connect(backButton, &QPushButton::clicked, [=](){
            initialPage();
        });
        layout->addWidget(optionsMenu);
        layout->addWidget(backButton);
        renderBottomBar("options");
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
        renderBottomBar("options");
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
                QTimer::singleShot(0, [&](){
                    initialPage();
                });
        });
        QObject::connect(button_send, &QPushButton::clicked, [=](){
                config["SERVER"]["url"] = urlEntry->text().toStdString();
                saveConfig();
                initialPage();
        });
        renderBottomBar("options");
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
                QTimer::singleShot(0, [optionsPage](){ 
                    if (optionsPage) optionsPage();
                });
        });
        QObject::connect(inbox, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [inboxPage](){
                    if (inboxPage) inboxPage();
                });
        });
        QObject::connect(back, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [initialPage](){ 
                    if (initialPage) initialPage();
                });
        });
        QObject::connect(friends, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [friendsPage](){
                    if (friendsPage) friendsPage();
                });
        });
        QObject::connect(button_change_url, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [change_url](){
                    if (change_url) change_url();
                });
        });
        scroll_area(layout, buttons);
        renderBottomBar("options");
    };
    sendEdit = [&](QString content){
        QJsonObject edit;
        edit["edit-mode"] = "bio";
        edit["content"] = content;
        edit["username"] = username;
        QString edit_response = requestHTTP(
            url + "/edit",
            "POST",
            edit
        );
        initialPage();
    };
    sendEdit = [&](QString content){
        QJsonObject edit;
        edit["edit-mode"] = "bio";
        edit["content"] = content;
        edit["username"] = username;
        QString edit_response = requestHTTP(
            url + "/edit",
            "POST",
            edit
        );
        initialPage();
    };
    profilePicturePage = [&](QString actualImage, QString biography){
        clearLayout(layout);
        fadeTransition(central);
        renderPostImage(actualImage, layout);
        
        QPushButton *newPicture = new QPushButton(profile_picture_text);
        
        // Capturamos apenas por valor [=], garantindo total segurança de memória
        QObject::connect(newPicture, &QPushButton::clicked, [=]() mutable {
            QString filePath = QFileDialog::getOpenFileName(
                nullptr, 
                "Select a image", 
                "", 
                "Images (*.png *.jpg *.jpeg *.webp)"
            );
            
            if (!filePath.isEmpty()) {
                int statusCode = 0;
                QString response = requestMultipart(
                    url + "/upload-profile", 
                    filePath,
                    10000,
                    &statusCode
                );
                QJsonDocument doc = QJsonDocument::fromJson(response.toUtf8());
                QJsonObject obj = doc.object();
                
                // 1. Pegamos a string real da URL vinda do JSON
                QString linkUrl = obj["image_url"].toString();
                
                qDebug() << "Resposta do servidor:" << response;
                qDebug() << "Link da URL obtido:" << linkUrl;
                
                // 2. Guardamos o texto com segurança dentro do próprio botão
                newPicture->setProperty("saved_url", linkUrl);
                
                clearLayout(layout);
                
                // 3. Recuperamos a string guardada e passamos para a função renderizar
                QString urlParaRenderizar = newPicture->property("saved_url").toString();
                renderPostImage(urlParaRenderizar, layout); 
                
                renderBottomBar("profile");
            }
        });
        
        layout->addWidget(newPicture);
        renderBottomBar("profile");
    };
    editAccount = [&](QString biography){
        clearLayout(layout);
        fadeTransition(central);
        QList<QWidget*> scroll_layout;
        QLineEdit *bioEntry = new QLineEdit(biography);
        bioEntry->setPlaceholderText(bio_text);
        QPushButton *loadPhoto = new QPushButton(profile_picture_text);
        QPushButton *sendButton = new QPushButton(send_text);
        QPushButton *backButton = new QPushButton(back_text);
        scroll_layout.append(bioEntry);
        scroll_layout.append(loadPhoto);
        scroll_layout.append(sendButton);
        scroll_layout.append(backButton);
        QStringList lines = biography.split('\n');
        QString urlImage;
        for (const QString &line : lines) {
            if (line.isEmpty()) continue;
            if (line.contains("[PROFILE]")){
                urlImage = line;
                urlImage.remove("[PROFILE]");
                break;
            }
        }
        for (const QString &line : lines) {
            if (line.isEmpty()) continue;                    
            if (line.contains("[PROFILE]")) {
                continue; 
            }
        };
        QObject::connect(backButton, &QPushButton::clicked, [=](){
            initialPage();
        });
        QObject::connect(sendButton, &QPushButton::clicked, [=](){
            sendEdit(bioEntry->text());
        });
        QObject::connect(loadPhoto, &QPushButton::clicked, [=](){
            profilePicturePage(urlImage, biography);
        });
        scroll_area(layout, scroll_layout);
        renderBottomBar("profile");
    };
    logout = [&](){
        loadConfig();
        config["FAST-LOGIN"]["username"] = "";
        config["FAST-LOGIN"]["password"] = "";
        config["FAST-LOGIN"]["token_session"] = "";
        saveConfig();
        loginPage();
    };
    viewProfilePicture = [&](QBoxLayout *picLayout, QString username){
        QJsonObject json_profile;
        if(!username.isEmpty()){}else{
            username = username;
        }
        json_profile["username"] = username;
        
        QString response_profile = requestHTTP(
            url + "/view-profile-picture",
            "POST",
            json_profile
        );
        
        QJsonDocument doc = QJsonDocument::fromJson(response_profile.toUtf8());
        QJsonObject json_response = doc.object();        
        QString profile_picture = json_response["profile-picture"].toString(); 
        qDebug() << "entrando no renderizador de foto" + profile_picture;
        renderAvatarImage(profile_picture, picLayout);
    };
    account = [&](){
        clearLayout(layout);
        fadeTransition(central);
        QHBoxLayout *layoutProfile = new QHBoxLayout();
        viewProfilePicture(layoutProfile, username);
        QLabel *label_username = new QLabel(username);
        label_username->resize(300, 50);
        label_username->setFixedSize(600, 100);
        label_username->setStyleSheet("font-size: 20px; font-weight: bold; color: white;");
        label_username->setWordWrap(true);
        layoutProfile->addWidget(label_username);
        layout->addLayout(layoutProfile);
        QJsonObject empty;
        QString bio_response = requestHTTP(
            url + "/view_profile/" + username,
            "GET",
            empty
        );
        QJsonDocument doc =
            QJsonDocument::fromJson(bio_response.toUtf8());
        QPushButton *logout_button = new QPushButton(exit_text);
        QJsonObject json_bio = doc.object();
        QString MyBio = json_bio["bio"].toString();
        QLabel *label_bio = new QLabel(MyBio);
        layout->addWidget(label_bio);
        QPushButton *button_edit = new QPushButton(edit_text);
        layout->addWidget(button_edit);
        layout->addWidget(logout_button);
        QObject::connect(button_edit, &QPushButton::clicked, [=](){
                editAccount(MyBio);
        });
        QObject::connect(logout_button, &QPushButton::clicked, [=](){
                logout();
        });
        renderBottomBar("profile");
        
    };
    fast_login = [&]()
    {
        if (config["FAST-LOGIN"]["token_login"].empty()){
            loginPage();
            return;
        }else{
            int status_code = 0;
            QJsonObject json_fast;
            json_fast["username"] = QString::fromStdString(config["FAST-LOGIN"]["username"]);
            json_fast["token"] = QString::fromStdString(config["FAST-LOGIN"]["token_login"]);
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
    trendingFeed = [&](){
        clearLayout(layout);
        fadeTransition(central);
        QHBoxLayout *tabPages = new QHBoxLayout();
        QPushButton *newer = new QPushButton(newer_text);
        QPushButton *trending = new QPushButton(trending_text);
        QPushButton *federations = new QPushButton(federations_text);
        newer->setProperty("class", "tab-button");
        trending->setProperty("class", "tab-button");
        federations->setProperty("class", "tab-button");
        newer->setProperty("active", false);
        trending->setProperty("active", true);
        federations->setProperty("active", false);
        tabPages->addWidget(newer);
        tabPages->addWidget(trending);
        tabPages->addWidget(federations);
        layout->addLayout(tabPages);
        QObject::connect(newer, &QPushButton::clicked, [=](){
            showfeed();
        });
        QString url_feed = url + "/trending-feed";
        qDebug() << "url feed" << url_feed;
        QNetworkRequest request{QUrl(url_feed)};
        QNetworkReply *reply = manager->get(request);
        QHBoxLayout *search_layout = new QHBoxLayout();
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
                QLabel *starLabel = new QLabel("...");
                frameLayout->addWidget(iconButton);
                frameLayout->addWidget(starLabel);

                iconButton->setIcon(QIcon(":/assets/default_star.png"));
                iconButton->setIconSize(QSize(24, 24));
                iconButton->setFixedSize(30, 30);
                iconButton->setStyleSheet("border: none;");

                starLabel->setStyleSheet("color: white; font-size: 14px;");


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
                initialPage();
            });
            QLineEdit *searchEntry = new QLineEdit();
            searchEntry->setPlaceholderText(search_text);
            QPushButton *sendButton = new QPushButton(send_text);
            search_layout->addWidget(searchEntry);
            search_layout->addWidget(sendButton);
            layout->addLayout(search_layout);
            layout->addWidget(btnBack);
            layout->addWidget(btnNewPost);

           

            QObject::connect(btnNewPost, &QPushButton::clicked, [=](){
                new_post();
            });

        });
        renderBottomBar("home");
    };
    showfeed = [&]()
    {
        clearLayout(layout);
        fadeTransition(central);
        QHBoxLayout *tabPages = new QHBoxLayout();
        QPushButton *newer = new QPushButton(newer_text);
        QPushButton *trending = new QPushButton(trending_text);
        QPushButton *federations = new QPushButton(federations_text);
        newer->setProperty("class", "tab-button");
        trending->setProperty("class", "tab-button");
        federations->setProperty("class", "tab-button");
        newer->setProperty("active", true);
        trending->setProperty("active", false);
        federations->setProperty("active", false);
        tabPages->addWidget(newer);
        tabPages->addWidget(trending);
        tabPages->addWidget(federations);
        layout->addLayout(tabPages);
        QObject::connect(trending, &QPushButton::clicked, [=](){
            trendingFeed();
        });
        QString url_feed = url + "/feed";
        qDebug() << "url feed" << url_feed;
        QNetworkRequest request{QUrl(url_feed)};
        QNetworkReply *reply = manager->get(request);
        QHBoxLayout *search_layout = new QHBoxLayout();
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

                int postId = post["id"].toInt();
                QString username = post["username"].toString();
                QString textPost = post["text_post"].toString();
                QString datetime = post["datetime"].toString();
                QStringList lines = textPost.split('\n');
                QString urlImage;
                QVBoxLayout *textLayout = new QVBoxLayout();
                for (const QString &line : lines) {
                    if (line.isEmpty()) continue;
                    if (line.contains("[IMAGE]")){
                        urlImage = line;
                        urlImage.remove("[IMAGE]");
                        break;
                    }
                }
                for (const QString &line : lines) {
                    if (line.isEmpty()) continue;                    
                    if (line.contains("[IMAGE]")) {
                        continue; 
                    }
                    QLabel *textLabel = new QLabel(line);
                    textLayout->addWidget(textLabel);
                }
                if (!urlImage.isEmpty()) {
                    renderPostImage(urlImage, textLayout);
                }
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
                frameLayout->addLayout(textLayout);
                frameLayout->addWidget(lblDate);

                // ===== BOTÃO STAR =====
                QPushButton *iconButton = new QPushButton();
                QLabel *starLabel = new QLabel("...");
                frameLayout->addWidget(iconButton);
                frameLayout->addWidget(starLabel);

                iconButton->setIcon(QIcon(":/assets/default_star.png"));
                iconButton->setIconSize(QSize(24, 24));
                iconButton->setFixedSize(30, 30);
                iconButton->setStyleSheet("border: none;");

                starLabel->setStyleSheet("color: white; font-size: 14px;");


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
                initialPage();
            });
            QLineEdit *searchEntry = new QLineEdit();
            searchEntry->setPlaceholderText(search_text);
            QPushButton *sendButton = new QPushButton(send_text);
            search_layout->addWidget(searchEntry);
            search_layout->addWidget(sendButton);
            layout->addLayout(search_layout);
            layout->addWidget(btnNewPost);
            renderBottomBar("home");

           

            QObject::connect(btnNewPost, &QPushButton::clicked, [=](){
                new_post();
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
            view_chat["id"] = *lastId;
            
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
                    msg["sender"].toString();

                QString text =
                    msg["message"].toString();

                int id =
                    msg["id"].toInt();

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
                QTimer::singleShot(
                    50,
                    [=](){

                        scroll->verticalScrollBar()->setValue(
                            scroll->verticalScrollBar()->maximum()
                        );

                    }
                );
            }
        };

        QObject::connect(
            timer,
            &QTimer::timeout,
            updateChat
        );

        updateChat();

        timer->start(100);

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

                json["sender"] = username;
                json["message"] = text;

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
        renderBottomBar("chat");
    };
    //tela quando você esta conversando com o usuario
    chat = [&](QString user){
        clearLayout(layout);
        QList<QWidget*> message;
        QHBoxLayout *lineMessage;


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

                    QString sender = msg["sender"].toString();
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

                
                bool lastMsgIsMe = false;
                if (msgs.size() > 0) {
                    lastMsgIsMe = (msgs[msgs.size() - 1].toObject()["sender"].toString() == username);
                }

                if (oldSize == 0 || lastMsgIsMe)
                {
                    QTimer::singleShot(50, [=](){
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
            QTimer::singleShot(0, [=](){
                timer->stop();
                delete currentMessageCount; // Deleta o ponteiro para não dar Memory Leak
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
        renderBottomBar("chat");
    };
    //pagina inicial de chat
    viewGroupsRequest = [&](){
        QJsonObject group_request;
        group_request["username"] = username;
        QString response = requestHTTP(
            url + "/my-groups",
            "POST",
            group_request
        );
        QJsonDocument doc = QJsonDocument::fromJson(response.toUtf8());
        QJsonObject obj = doc.object();
        QString group_id = obj["group_id"].toString();
        QString name_group = obj["name_group"].toString();
        QJsonObject response_json;
        response_json["group_id"] = group_id;
        response_json["name_group"] = name_group;
        return response_json;
    };
    chatPage = [&](){
        clearLayout(layout);
        fadeTransition(central);
        QList<QWidget*> widgets;
        QPushButton *ChatGlobalButton = new QPushButton("Chat Global");
        QPushButton *newChatButton = new QPushButton("new chat");
        QObject::connect(newChatButton, &QPushButton::clicked, [=](){
            new_chat();
        });
        layout->addWidget(newChatButton);
        QObject::connect(ChatGlobalButton, &QPushButton::clicked, [=]() mutable{
            chatGlobal();
        });
        
        QJsonObject friends_json;
        friends_json["username"] = username;
        QString response_friends = requestHTTP(
            url + "/friends",
            "POST",
            friends_json
        );
        QJsonObject json_response_groups = viewGroupsRequest();
        if (json_response_groups.isEmpty()){}else{
            QPushButton *buttonGroup = new QPushButton(json_response_groups["name_group"].toString());
            layout->addWidget(buttonGroup);
        };
        QJsonDocument doc = QJsonDocument::fromJson(response_friends.toUtf8());
        QJsonObject obj = doc.object();
        QJsonArray friends = obj["friends"].toArray();
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
                QWidget *containerWidget = new QWidget();
                QPushButton *user = new QPushButton(friendName);
                QHBoxLayout *buttonLayout = new QHBoxLayout();
                viewProfilePicture(buttonLayout, username);
                buttonLayout->addWidget(user);
                buttonLayout->addStretch();
                containerWidget->setLayout(buttonLayout);
                QObject::connect(user, &QPushButton::clicked, [=]() mutable{
                    QTimer::singleShot(0, [=](){
                        chat(friendName);
                    });
                });
                widgets.append(containerWidget);
            };
        };
        QPushButton *back_button = new QPushButton(back_text);
        QObject::connect(back_button, &QPushButton::clicked, [=]() mutable{
            QTimer::singleShot(0, [=](){
                initialPage();
            });
        });
        widgets.append(ChatGlobalButton);
        widgets.append(back_button);
        scroll_area(layout, widgets);
        renderBottomBar("chat");
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
    renderBottomBar = [&](QString actual_window){
        splash.finish(&window);

        QWidget *bottomBar = new QWidget(central);
        bottomBar->setFixedHeight(74);

        QPushButton *btnHome = new QPushButton(bottomBar);
        QPushButton *btnChat = new QPushButton(bottomBar);
        QPushButton *btnProfile = new QPushButton(bottomBar);
        QPushButton *btnOptions = new QPushButton(bottomBar);
        btnHome->setProperty("class", "tab-button");
        btnChat->setProperty("class", "tab-button");
        btnProfile->setProperty("class", "tab-button");
        btnOptions->setProperty("class", "tab-button");
        btnHome->setIcon(QIcon(":/assets/home.png"));
        btnChat->setIcon(QIcon(":/assets/chat.png"));
        btnProfile->setIcon(QIcon(":/assets/account.png"));
        btnOptions->setIcon(QIcon(":/assets/options.png"));
        if (actual_window == "home"){
            btnHome->setProperty("active", true);
        }
        if (actual_window == "chat"){
            btnChat->setProperty("active", true);
        }
        if (actual_window == "profile"){
            btnProfile->setProperty("active", true);
        }
        if (actual_window == "options"){
            btnOptions->setProperty("active", true);
        }
        QSize iconSize(64, 64);
        QSize iconSizeHome(32, 32);
        
        btnHome->setIconSize(iconSizeHome);
        btnChat->setIconSize(iconSize);
        btnProfile->setIconSize(iconSize);
        btnOptions->setIconSize(iconSize);

        btnHome->setFixedSize(iconSize);
        btnChat->setFixedSize(iconSize);
        btnProfile->setFixedSize(iconSize);
        btnOptions->setFixedSize(iconSize);

        QObject::connect(btnHome, &QPushButton::clicked, [=]() { 
            showfeed();
        });
        QObject::connect(btnChat, &QPushButton::clicked, [=]() { chatPage(); });
        QObject::connect(btnProfile, &QPushButton::clicked, [=]() { account(); });
        QObject::connect(btnOptions, &QPushButton::clicked, [options]() { if (options) options(); });

        QHBoxLayout *barLayout = new QHBoxLayout(bottomBar);
        barLayout->setContentsMargins(10, 5, 10, 5);
        barLayout->setSpacing(10);
        
        barLayout->addWidget(btnHome);
        barLayout->addWidget(btnChat);
        barLayout->addWidget(btnProfile);
        barLayout->addWidget(btnOptions);

        if (layout) {
            layout->addWidget(bottomBar, 0);
        }
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
        valideToken["username"] = QString::fromStdString(config["FAST-LOGIN"]["username"]);
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
        showfeed();
    };
    signinRequest = [&](QString username, QString password, QString email){
        QJsonObject json_signin;
        json_signin["username"] = username;
        json_signin["password"] = password;
        json_signin["email"] = email;
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
        QHBoxLayout *bannerLayout = new QHBoxLayout();
        QWidget *container = new QWidget();
        QIcon *banner_image = new QIcon(":/assets/linka_app_login_banner.png");
        QLabel *banner_login = new QLabel();
        banner_login->setPixmap(banner_image->pixmap(QSize(400, 200)));
        banner_login->setAlignment(Qt::AlignCenter);
        bannerLayout->addWidget(banner_login);
        bannerLayout->setContentsMargins(0, 0, 0, 0);
        bannerLayout->setSpacing(0);
        container->setLayout(bannerLayout);
        int larguraDaTela = QGuiApplication::primaryScreen()->geometry().width();
        container->setMaximumWidth(larguraDaTela);
        layout->addWidget(container);
        QHBoxLayout *loginButtons = new QHBoxLayout();
        QPushButton *signinPage_button = new QPushButton(signup_text);
        QPushButton *signupPage_button = new QPushButton(signin_text);
        QPushButton *change_server_button = new QPushButton();
        signinPage_button->setProperty("class", "tab-button");
        signupPage_button->setProperty("class", "tab-button");
        signinPage_button->setProperty("active", false);
        signupPage_button->setProperty("active", true);
        loginButtons->addWidget(signinPage_button);
        loginButtons->addWidget(signupPage_button);
        layout->addLayout(loginButtons);
        QLineEdit *usernameEntry = new QLineEdit();
        QLineEdit *passwordEntry = new QLineEdit();
        QLineEdit *retryPasswordEntry = new QLineEdit();
        QLineEdit *emailEntry = new QLineEdit();
        usernameEntry->setPlaceholderText(username_text);
        passwordEntry->setPlaceholderText(password_text);
        retryPasswordEntry->setPlaceholderText(retry_password_text);
        emailEntry->setPlaceholderText(email_text);
        usernameEntry->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Fixed);
        passwordEntry->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Fixed);
        retryPasswordEntry->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Fixed);
        emailEntry->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Fixed);
        QPushButton *send_button = new QPushButton(send_text);
        QObject::connect(signinPage_button, &QPushButton::clicked, [=](){
            signupPage();
        });
        QObject::connect(signupPage_button, &QPushButton::clicked, [=](){
            signinPage();
        });
        QObject::connect(send_button, &QPushButton::clicked, [=](){
            if (passwordEntry->text() == retryPasswordEntry->text()) {
                int status_code = signinRequest(usernameEntry->text(), passwordEntry->text(), emailEntry->text());
                if (status_code == 200 || status_code == 201){
                    QJsonObject create_json;
                    create_json["username"] = username;
                    requestHTTP(
                        url + "/create_profile",
                        "POST",
                        create_json
                    );
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
        QPushButton *discordButton = new QPushButton();
        discordButton->setIcon(QIcon(":/assets/discord.png"));
        
        QPushButton *redditButton = new QPushButton();
        redditButton->setIcon(QIcon(":/assets/reddit.png"));
        
        // 1. Reduza o tamanho dos ícones para algo realista em telas de celular
        // Em vez de 200 de largura, use tamanhos quadrados ou mais compactos para não estourar
        discordButton->setIconSize(QSize(120, 40)); 
        redditButton->setIconSize(QSize(120, 40));

        // 2. O SEGREDO: Trave a largura máxima do BOTÃO para ele não crescer além disso
        discordButton->setMaximumWidth(130);
        redditButton->setMaximumWidth(130);
        
        QHBoxLayout *layoutHorizontal = new QHBoxLayout();
        layoutHorizontal->addSpacerItem(new QSpacerItem(0, 0, QSizePolicy::Expanding, QSizePolicy::Minimum));
        layoutHorizontal->addWidget(discordButton);
        layoutHorizontal->addWidget(redditButton);
        
        // Adiciona um spacer na direita também para centralizar os dois botões bonitinho no meio da tela
        layoutHorizontal->addSpacerItem(new QSpacerItem(0, 0, QSizePolicy::Expanding, QSizePolicy::Minimum));
        
        layout->addSpacerItem(new QSpacerItem(0, 0, QSizePolicy::Minimum, QSizePolicy::Expanding));
        layout->addLayout(layoutHorizontal);

    };
    
    signupRequest = [&](QString username, QString password){
        int status_code = 0;
        QJsonObject json_signup;
        json_signup["username"] = username;
        json_signup["password"] = password;
        QString response_signup = requestHTTP(
            url + "/login",
            "POST",
            json_signup,
            10000,
            &status_code
        );
        token = newSession(username, password);
        loadConfig();
        config["FAST-LOGIN"]["token_session"] = token.toStdString();
        saveConfig();
        return status_code;
    };
    signupPage = [&](){
        clearLayout(layout);
        fadeTransition(central);
        QHBoxLayout *bannerLayout = new QHBoxLayout();
        QWidget *container = new QWidget();
        QIcon *banner_image = new QIcon(":/assets/linka_app_login_banner.png");
        QLabel *banner_login = new QLabel();
        banner_login->setPixmap(banner_image->pixmap(QSize(400, 200)));
        banner_login->setAlignment(Qt::AlignCenter);
        bannerLayout->setContentsMargins(0, 0, 0, 0);
        bannerLayout->setSpacing(0);
        bannerLayout->addWidget(banner_login);
        container->setLayout(bannerLayout);
        layout->addWidget(container);
        layout->addLayout(bannerLayout);
        QHBoxLayout *loginButtons = new QHBoxLayout();
        QPushButton *signinPage_button = new QPushButton(signup_text);
        QPushButton *signupPage_button = new QPushButton(signin_text);
        QPushButton *change_server_button = new QPushButton();
        signinPage_button->setProperty("class", "tab-button");
        signupPage_button->setProperty("class", "tab-button");
        signinPage_button->setProperty("active", true);
        signupPage_button->setProperty("active", false);
        loginButtons->addWidget(signinPage_button);
        loginButtons->addWidget(signupPage_button);
        layout->addLayout(loginButtons);
        QLineEdit *usernameEntry = new QLineEdit();
        QLineEdit *passwordEntry = new QLineEdit();
        usernameEntry->setPlaceholderText(username_text);
        passwordEntry->setPlaceholderText(password_text);
        usernameEntry->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Fixed);
        passwordEntry->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Fixed);
        QPushButton *send_button = new QPushButton(send_text);
        layout->addWidget(usernameEntry);
        layout->addWidget(passwordEntry);
        layout->addWidget(send_button);
        QObject::connect(signinPage_button, &QPushButton::clicked, [=](){
            signupPage();
        });
        QObject::connect(signupPage_button, &QPushButton::clicked, [=](){
            signinPage();
        });
        QObject::connect(send_button, &QPushButton::clicked, [=, &token_session]() mutable {
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
        QPushButton *discordButton = new QPushButton();
        discordButton->setIcon(QIcon(":/assets/discord.png"));
        
        QPushButton *redditButton = new QPushButton();
        redditButton->setIcon(QIcon(":/assets/reddit.png"));
        
        // 1. Reduza o tamanho dos ícones para algo realista em telas de celular
        // Em vez de 200 de largura, use tamanhos quadrados ou mais compactos para não estourar
        discordButton->setIconSize(QSize(120, 40)); 
        redditButton->setIconSize(QSize(120, 40));

        // 2. O SEGREDO: Trave a largura máxima do BOTÃO para ele não crescer além disso
        discordButton->setMaximumWidth(130);
        redditButton->setMaximumWidth(130);
        
        QHBoxLayout *layoutHorizontal = new QHBoxLayout();
        layoutHorizontal->addSpacerItem(new QSpacerItem(0, 0, QSizePolicy::Expanding, QSizePolicy::Minimum));
        layoutHorizontal->addWidget(discordButton);
        layoutHorizontal->addWidget(redditButton);
        
        // Adiciona um spacer na direita também para centralizar os dois botões bonitinho no meio da tela
        layoutHorizontal->addSpacerItem(new QSpacerItem(0, 0, QSizePolicy::Expanding, QSizePolicy::Minimum));
        
        layout->addSpacerItem(new QSpacerItem(0, 0, QSizePolicy::Minimum, QSizePolicy::Expanding));
        layout->addLayout(layoutHorizontal);
    };
    newGroupRequest = [&](QString name_group, QString description){
        QJsonObject json_group;
        json_group["username"] = username;
        json_group["name_group"] = name_group;
        json_group["description"] = description;
        requestHTTP(
            url + "/create-group",
            "POST",
            json_group
        );
        chatPage();
    };
    newGroupPage = [&](){
        clearLayout(layout);
        fadeTransition(central);
        QLineEdit *name_group = new QLineEdit();
        QLineEdit *description = new QLineEdit();
        name_group->setPlaceholderText(name_group_text);
        description->setPlaceholderText(description_text);
        layout->addWidget(name_group);
        layout->addWidget(description);
        QPushButton *sendButton = new QPushButton(send_text);
        QPushButton *backButton = new QPushButton(back_text);
        QObject::connect(backButton, &QPushButton::clicked, [=](){
            chatPage();
        });
        QObject::connect(sendButton, &QPushButton::clicked, [=](){
            newGroupRequest(name_group->text(), description->text());
        });
        layout->addWidget(sendButton);
        layout->addWidget(backButton);
        renderBottomBar("chat");
    };
    new_chat = [&](){
        clearLayout(layout);
        QPushButton *newGroupButton = new QPushButton(new_group_text);
        layout->addWidget(newGroupButton);
        QList<QWidget*> button_area;
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
            QLabel *label_error = new QLabel("No Friends....");
            button_area.append(label_error);
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
                button_area.append(user);
            };
        };
        QObject::connect(newGroupButton, &QPushButton::clicked, [=](){
            newGroupPage();
        });
        scroll_area(layout, button_area);
        renderBottomBar("chat");
        
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
        renderBottomBar("options");
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
        renderBottomBar("options");
    };
    //chamada da função
    initialPage();
    //função para exibir o feed
    window.showMaximized();
    return app.exec();
    
}