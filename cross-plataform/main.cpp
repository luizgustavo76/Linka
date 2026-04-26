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
#include "global.h"
std::map<std::string, std::map<std::string, std::string>> config_main;
int main(int argc, char *argv[])
{
    loadConfig();
    if (config_main["FAST-LOGIN"]["username"].empty()){
        openLogin(argc, argv);
    }else{
        openMenu(argc, argv);
    };
};