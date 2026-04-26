#pragma once
#include <QString>
void clearLayout();
void loadStyle();
void loadConfig();
QString configPath();
int openLogin(int argc, char *argv[]);
int openMenu(int argc, char *argv[]);