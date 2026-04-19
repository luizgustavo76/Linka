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
#include <QJsonDocument>
#include <QJsonObject>
#include <QLabel>
#include <QUrl>
#include <fstream>
#include <string>
#include <map>
#include <QFile>
#include <QTranslator>

QTranslator translator;

void loadLanguage(const QString& lang)
{
    qApp->removeTranslator(&translator);

    if (lang == "pt-br") {
        bool ok = translator.load(":/translations/pt-br.qm");
        qDebug() << "PT-BR carregado?" << ok;
    } else {
        translator.load(":/translations/en.qm");
    }

    qApp->installTranslator(&translator);
}

void loadStyle()
{
    QFile file(":/styles/style.qss");

    if (file.open(QFile::ReadOnly)) {
        qApp->setStyleSheet(file.readAll());
    }
}

std::map<std::string, std::map<std::string, std::string>> config;

void loadConfig() {
    std::ifstream file("config-login.cfg");
    std::string line;
    std::string section;

    while (std::getline(file, line)) {

        if(line.empty()) continue;

        // detectar seção
        if(line[0] == '['){
            section = line.substr(1, line.find(']') - 1);
        }

        else {
            size_t pos = line.find('=');
            if(pos != std::string::npos){

                std::string key = line.substr(0, pos);
                std::string value = line.substr(pos + 1);

                // remover espaços simples
                if(key[0] == ' ') key = key.substr(1);
                if(value[0] == ' ') value = value.substr(1);

                config[section][key] = value;
            }
        }
    }
}
void saveConfig() {
    std::ofstream file("config-login.cfg");

    file << "[SERVER]\n";
    file << "url = " << config["SERVER"]["url"] << "\n\n";

    file << "[LANG]\n";
    file << "lang = " << config["LANG"]["lang"] << "\n\n";

    file << "[FAST-LOGIN]\n";
    file << "username = " << config["FAST-LOGIN"]["username"] << "\n";
    file << "token = " << config["FAST-LOGIN"]["token"] << "\n";
    file << "password = " << config["FAST-LOGIN"]["password"] << "\n\n";

    file << "[FEDERATIONS]\n";
    file << "url = []\n\n";

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
    loadConfig();
    loadStyle();
    loadLanguage("pt-br");
    QString url = "http://127.0.0.1:5000";
    QString signup_text = QCoreApplication::translate("login", "sign-up");
    QString signin_text = QCoreApplication::translate("login", "sign-in");
    QString username_text = QCoreApplication::translate("login", "username");
    QString password_text = QCoreApplication::translate("login", "password");
    QString back_text = QCoreApplication::translate("global", "back");
    QString send_text = QCoreApplication::translate("login", "send");
    QString repeat_password_text = QCoreApplication::translate("sign-up", "repeat the password");
    QString error_401 = QCoreApplication::translate("errors", "401");

    QWidget window;
    window.setWindowTitle("Linka Mobile");
    window.resize(400, 600);
    QVBoxLayout *layout = new QVBoxLayout(&window);
    QNetworkAccessManager *manager = new QNetworkAccessManager(&window);
    auto entry = [&](QString text) -> QLineEdit* {
        QLineEdit *input = new QLineEdit();
        input->setPlaceholderText(text);
        layout->addWidget(input);
        return input;
    };

    // Declara antes
    std::function<void()> showInitialPage;
    std::function<void()> signupPage;
    std::function<void()> signinPage;

    auto button = [&](QString text, std::function<void()> func)
    {
        QPushButton *btn = new QPushButton(text);
        layout->addWidget(btn);

        QObject::connect(btn, &QPushButton::clicked, func);
    };

    showInitialPage = [&]() {
        clearLayout(layout);
        button(signup_text, signupPage);
        button(signin_text, signinPage);
        //button(signin_text, signinPage); // depois você cria
    };
    auto login_server = [&](QString username, QString password){
        qDebug() << "BASE URL:" << url;
        qDebug() << "FINAL:" << url + "/login";
        QUrl url_server(url + "/login");
        QNetworkRequest request(url_server);
        request.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");
        QJsonObject body;
        body["username"] = username;
        body["password"] = password;
        QJsonDocument doc(body);
        QByteArray data = doc.toJson();
        QNetworkReply *reply = manager->post(request, data);

        QObject::connect(reply, &QNetworkReply::finished, [=]() {

            int status = reply->attribute(
                QNetworkRequest::HttpStatusCodeAttribute
            ).toInt();

            QByteArray body = reply->readAll();


            if(status == 200){

                config["FAST-LOGIN"]["username"] = username.toStdString();
                config["FAST-LOGIN"]["password"] = password.toStdString();

                saveConfig();

                QApplication::quit();
            } else {
                QLabel *labelTexto = new QLabel(error_401);
                layout->addWidget(labelTexto);
            }

            reply->deleteLater();
        });
    };
    signinPage = [&]() {
        clearLayout(layout);
        QLineEdit *user_entry = entry(username_text);
        QLineEdit *password_entry = entry(password_text);
        password_entry->setEchoMode(QLineEdit::Password);
        button(back_text, showInitialPage);
        button(send_text, [=]() {
            login_server(user_entry->text(), password_entry->text());
        });
    };
    auto signup_server = [&](QString username, QString password, QString email){
        qDebug() << "BASE URL:" << url;
        qDebug() << "FINAL:" << url + "/register";
        QUrl url_server(url + "/register");
        QNetworkRequest request(url_server);
        request.setHeader(QNetworkRequest::ContentTypeHeader, "application/json");
        QJsonObject body;
        body["username"] = username;
        body["password"] = password;
        body["email"] = email;
        QJsonDocument doc(body);
        QByteArray data = doc.toJson();
        QNetworkReply *reply = manager->post(request, data);

        QObject::connect(reply, &QNetworkReply::finished, [=]() {

            int status = reply->attribute(
                QNetworkRequest::HttpStatusCodeAttribute
            ).toInt();

            QByteArray body = reply->readAll();


            if(status == 200||status == 201){

                config["FAST-LOGIN"]["username"] = username.toStdString();
                config["FAST-LOGIN"]["password"] = password.toStdString();

                saveConfig();

                QApplication::quit();
            } else {
                QLabel *labelTexto = new QLabel(error_401);
                layout->addWidget(labelTexto);
            }

            reply->deleteLater();
        });
    };
    signupPage = [&]() {
        clearLayout(layout);
        QLineEdit *user_entry = entry(username_text);
        QLineEdit *password_entry = entry(password_text);
        QLineEdit *repeat_entry = entry(repeat_password_text);
        QLineEdit *email_entry = entry("email");
        password_entry->setEchoMode(QLineEdit::Password);
        repeat_entry->setEchoMode(QLineEdit::Password);
        button(back_text, showInitialPage);
        button(send_text, [=]() {
            signup_server(user_entry->text(), password_entry->text(), email_entry->text());
        });
    };

    // começa na tela inicial
    showInitialPage();

    window.show();
    return app.exec();
}