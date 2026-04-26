#pragma once

#include <QWidget>

class Login : public QWidget
{
    Q_OBJECT

public:
    explicit Login(QWidget *parent = nullptr);

signals:
    void success();
};