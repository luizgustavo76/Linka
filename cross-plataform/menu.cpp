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
#include <QVector>
#include <QJsonDocument>
#include <QJsonObject>
#include <QLabel>
#include <QUrl>
#include <fstream>
#include <string>
#include <QStackedWidget>
#include <map>
#include <QFile>
#include <QTabWidget>
#include <QTabBar>
#include <QMainWindow>
#include <QJsonArray>
#include <QScrollArea>
#include <QTimer>
#include <QFrame>
#include <QIcon>
#include <QSize>

void loadStyle()
{
    QFile file(":/styles/theme.qss");

    if (file.open(QFile::ReadOnly)) {
        qDebug() << "QSS loaded";
        qApp->setStyleSheet(file.readAll());
    } else {
        qDebug() << "QSS FAILED";
    }
}

std::map<std::string, std::map<std::string, std::string>> config;
void scroll_area(QVBoxLayout *layout, const QList<QWidget*> &widgets)
{
    QScrollArea *scroll = new QScrollArea();
    scroll->setWidgetResizable(true);

    QWidget *container = new QWidget();
    QVBoxLayout *containerLayout = new QVBoxLayout(container);

    for(QWidget *w : widgets)
    {
        containerLayout->addWidget(w);
    }

    containerLayout->addStretch();
    scroll->setWidget(container);

    layout->addWidget(scroll);
}
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
    QApplication::setStyle("Fusion");
    loadConfig();
    loadStyle();
    //url do servidor
    QString url = "http://127.0.0.1:5000";
    //janela principal
    QMainWindow window;
    window.setWindowTitle("Linka Mobile");
    window.resize(400, 600);

    // CENTRAL WIDGET
    QWidget *central = new QWidget();
    QVBoxLayout *layout = new QVBoxLayout(central);
    //strings traduzidas
    QString text_post = QCoreApplication::translate("feed", "text post");
    QString back_text = QCoreApplication::translate("global", "back");
    layout->setContentsMargins(0, 0, 0, 0);
    layout->setSpacing(0);

    window.setCentralWidget(central);

    QNetworkAccessManager *manager = new QNetworkAccessManager(&window);
    std::function<void()> showfeed;
    std::function<void()> initialPage;
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
    //novo ppost
    auto new_post = [&](){
        clearLayout(layout);
        QLineEdit *text = entry(text_post);
        button(back_text, initialPage);
    };
    showfeed = [&]()
    {
        clearLayout(layout);

        QNetworkRequest request(QUrl(url + "/feed"));
        QNetworkReply *reply = manager->get(request);

        QObject::connect(reply, &QNetworkReply::finished, [=]() mutable {

            if(reply->error() != QNetworkReply::NoError)
            {
                QLabel *err = new QLabel("Erro ao carregar feed!");
                layout->addWidget(err);
                reply->deleteLater();
                return;
            }

            QByteArray responseData = reply->readAll();
            reply->deleteLater();

            QJsonDocument doc = QJsonDocument::fromJson(responseData);

            if(!doc.isArray())
            {
                QLabel *err = new QLabel("Resposta inválida do servidor!");
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

                QLabel *lblUser = new QLabel(username);
                QLabel *lblText = new QLabel(textPost);
                QLabel *lblDate = new QLabel(datetime);

                lblUser->setStyleSheet("color: white; font-size: 16px; font-weight: bold;");
                lblText->setStyleSheet("color: white; font-size: 14px;");
                lblDate->setStyleSheet("color: gray; font-size: 12px;");

                frameLayout->addWidget(lblUser);
                frameLayout->addWidget(lblText);
                frameLayout->addWidget(lblDate);

                // ===== BOTÃO STAR =====
                QPushButton *iconButton = new QPushButton();
                iconButton->setIcon(QIcon("../assets/default_star.png"));
                iconButton->setIconSize(QSize(24, 24));
                iconButton->setFixedSize(30, 30);
                iconButton->setStyleSheet("border: none;");

                QLabel *starLabel = new QLabel("...");
                starLabel->setStyleSheet("color: white; font-size: 14px;");

                // buscar quantidade de estrelas
                QNetworkRequest starsReq(QUrl(url + "/return-stars/" + QString::number(postId)));
                QNetworkReply *starsReply = manager->get(starsReq);

                QObject::connect(starsReply, &QNetworkReply::finished, [=]() mutable {
                    if(starsReply->error() == QNetworkReply::NoError)
                    {
                        QString starsText = starsReply->readAll();
                        starLabel->setText(starsText);
                    }
                    else
                    {
                        starLabel->setText("0");
                    }

                    starsReply->deleteLater();
                });

                // clique da estrela (toggle)
                QObject::connect(iconButton, &QPushButton::clicked, [=]() mutable {
                    // aqui você chamaria sua função toggle_star
                    // exemplo:
                    // toggle_star(iconButton, postId);

                    qDebug() << "Star clicada no post:" << postId;
                });

                starLayout->addWidget(iconButton);
                starLayout->addWidget(starLabel);
                starLayout->addStretch();

                frameLayout->addLayout(starLayout);

                labels.append(frame);
            }

            scroll_area(layout, labels);

            // botões de baixo
            QPushButton *btnBack = new QPushButton("Voltar");
            QPushButton *btnNewPost = new QPushButton("Novo Post");
            QObject::connect(btnBack, &QPushButton::clicked, [=](){
                QTimer::singleShot(0, [&](){
                    initialPage();
                });
            });
            layout->addWidget(btnBack);
            layout->addWidget(btnNewPost);

           

            QObject::connect(btnNewPost, &QPushButton::clicked, [=](){
                new_post();
                // new_post();
            });

        });
    };
    //pagina inicial para renderizar
    initialPage = [&]()
    {
        clearLayout(layout);
        QStackedWidget *stack = new QStackedWidget(central);

        // ======= PÁGINAS =======
        QWidget *pageHome = new QWidget();
        QVBoxLayout *homeLayout = new QVBoxLayout(pageHome);
        QLabel *homeLabel = new QLabel("🏠 HOME");
        homeLabel->setAlignment(Qt::AlignCenter);
        homeLayout->addWidget(homeLabel);

        QWidget *pageSearch = new QWidget();
        QVBoxLayout *searchLayout = new QVBoxLayout(pageSearch);
        QLabel *searchLabel = new QLabel("🔍 BUSCA");
        searchLabel->setAlignment(Qt::AlignCenter);
        searchLayout->addWidget(searchLabel);

        QWidget *pageProfile = new QWidget();
        QVBoxLayout *profileLayout = new QVBoxLayout(pageProfile);
        QLabel *profileLabel = new QLabel("👤 PERFIL");
        profileLabel->setAlignment(Qt::AlignCenter);
        profileLayout->addWidget(profileLabel);

        stack->addWidget(pageHome);
        stack->addWidget(pageSearch);
        stack->addWidget(pageProfile);

        // ======= BARRA INFERIOR =======
        QWidget *bottomBar = new QWidget(central);
        bottomBar->setFixedHeight(90);

        QPushButton *btnHome = new QPushButton("🏠", bottomBar);
        QPushButton *btnSearch = new QPushButton("🔍", bottomBar);
        QPushButton *btnProfile = new QPushButton("👤", bottomBar);
        QObject::connect(btnHome, &QPushButton::clicked, [=]() {
            showfeed();
        });
        btnHome->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
        btnSearch->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);
        btnProfile->setSizePolicy(QSizePolicy::Expanding, QSizePolicy::Expanding);

        QHBoxLayout *barLayout = new QHBoxLayout(bottomBar);
        barLayout->setContentsMargins(10, 10, 10, 10);
        barLayout->setSpacing(10);

        barLayout->addWidget(btnHome);
        barLayout->addWidget(btnSearch);
        barLayout->addWidget(btnProfile);

        QObject::connect(btnHome, &QPushButton::clicked, [=](){
            stack->setCurrentIndex(0);
        });

        QObject::connect(btnSearch, &QPushButton::clicked, [=](){
            stack->setCurrentIndex(1);
        });

        QObject::connect(btnProfile, &QPushButton::clicked, [=](){
            stack->setCurrentIndex(2);
        });

        // ======= ESTILO =======
        bottomBar->setStyleSheet("background: #111;");

        btnHome->setStyleSheet("font-size: 32px; border: none; color: #00ffea; background: transparent;");
        btnSearch->setStyleSheet("font-size: 32px; border: none; color: white; background: transparent;");
        btnProfile->setStyleSheet("font-size: 32px; border: none; color: white; background: transparent;");

        // ======= MONTAGEM =======
        layout->addWidget(stack);
        layout->addWidget(bottomBar);
    };
    //chamada da função
    initialPage();
    //função para exibir o feed
    
    

    window.show();
    return app.exec();
}
