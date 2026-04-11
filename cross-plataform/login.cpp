#include <QApplication>
#include <QWidget>
#include <QPushButton>

int main(int argc, char *argv[])
{
    QApplication app(argc, argv);

    QWidget window;
    window.setWindowTitle("Linka Mobile");
    window.resize(400, 600);

    QPushButton button("HI!", &window);
    button.move(150, 250);

    window.show();

    return app.exec();
}