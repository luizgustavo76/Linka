#include <QApplication>
#include <QWidget>
#include <QPushButton>
#include <QVBoxLayout>
#include "translator.h"
#include <QLineEdit>
#include <QDebug>
#include <functional>

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

    QString signup_text = QString::fromStdString(translate("initial-page", "sign-up"));
    QString signin_text = QString::fromStdString(translate("initial-page", "sign-in"));
    QString username_text = QString::fromStdString(translate("login", "username"));
    QString password_text = QString::fromStdString(translate("login", "password"));
    QString back_text = QString::fromStdString(translate("global", "back"));
    QString send_text = QString::fromStdString(translate("login", "send"));
    QString repeat_password_text = QString::fromStdString(translate("sign-up", "repeat the password"));
    QWidget window;
    window.setWindowTitle("Linka Mobile");
    window.resize(400, 600);

    QVBoxLayout *layout = new QVBoxLayout(&window);

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

    signinPage = [&]() {
        clearLayout(layout);
        QLineEdit *user_entry = entry(username_text);
        QLineEdit *password_entry = entry(password_text);
        password_entry->setEchoMode(QLineEdit::Password);
        button(back_text, showInitialPage);
        button(send_text, nullptr);
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