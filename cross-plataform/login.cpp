#include <QApplication>
#include <QWidget>
#include <QPushButton>
#include <stdio.h>
#include <QVBoxLayout>
#include "translator.h"
#include <QLineEdit>
#include <QDebug>
int main(int argc, char *argv[])
{
    QApplication app(argc, argv);
    QString signup_text = QString::fromStdString(translate("initial-page", "sign-up"));
    QWidget window;
    window.setWindowTitle("Linka Mobile");
    window.resize(400, 600);

    QVBoxLayout *layout = new QVBoxLayout(&window);
    auto entry = [&](QString text){
        QLineEdit *input = new QLineEdit();
        input->setPlaceholderText(text);
        layout->addWidget(input);
    };
    auto signup = [&]()
    {
        qDebug() << "CLICOU!";
        entry("hi");
    };
    auto button = [&](QString text, std::function<void()> func)
    {
        QPushButton *btn = new QPushButton(text);
        layout->addWidget(btn);

        QObject::connect(btn, &QPushButton::clicked, func);
    };
    button(signup_text, signup);

    window.show();
    return app.exec();
}