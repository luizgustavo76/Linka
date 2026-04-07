#include <QApplication>
#include <QTextEdit>
#include <iostream>
#include <QtWidgets>
#include <QtCore>
#include <QtGui>
using namespace std;
int main(int argv, char **args)
{
QApplication app(argv, args);
QWidget window;
window.setWindowTitle("Linka Mobile");
window.resize(400, 600);

QPushButton button("HI!");
button.show();
return app.exec();
}