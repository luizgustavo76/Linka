#include <QApplication>
#include <QWidget>
#include <QPushButton>
#include <QVBoxLayout>
#include "translator.h"
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
    QString url = "http://127.0.0.1:5000";
    QString signup_text = QString::fromStdString(translate("initial-page", "sign-up"));
    QString signin_text = QString::fromStdString(translate("initial-page", "sign-in"));
    QString username_text = QString::fromStdString(translate("login", "username"));
    QString password_text = QString::fromStdString(translate("login", "password"));
    QString back_text = QString::fromStdString(translate("global", "back"));
    QString send_text = QString::fromStdString(translate("login", "send"));
    QString repeat_password_text = QString::fromStdString(translate("sign-up", "repeat the password"));
    QString error_401 = QString::fromStdString(translate("errors", "401"));
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
    signupPage = [&]() {
        clearLayout(layout);
        QLineEdit *user_entry = entry(username_text);
        QLineEdit *password_entry = entry(password_text);
        QLineEdit *repeat_entry = entry(repeat_password_text);
        password_entry->setEchoMode(QLineEdit::Password);
        repeat_entry->setEchoMode(QLineEdit::Password);
        button(back_text, showInitialPage);
        button(send_text, nullptr);
    };

    // começa na tela inicial
    showInitialPage();

    window.show();
    return app.exec();
}