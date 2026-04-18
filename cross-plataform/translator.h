#ifndef TRANSLATOR_H
#define TRANSLATOR_H

#include <nlohmann/json.hpp>
#include <QFile>
#include <QByteArray>
#include <QDebug>
#include <string>

using json = nlohmann::json;

inline std::string lang = "en";

inline std::string translate(const std::string& section, const std::string& key)
{
    std::string path = ":/strings/login/" + lang + ".json";

    QFile file(QString::fromStdString(path));

    if (!file.open(QIODevice::ReadOnly)) {
        qDebug() << "ERRO: nao consegui abrir o arquivo:" << QString::fromStdString(path);
        return "???";
    }

    QByteArray raw = file.readAll();

    try {
        json data = json::parse(raw.constData());
        return data[section][key].get<std::string>();
    }
    catch (const std::exception& e) {
        qDebug() << "ERRO JSON:" << e.what();
        return "???";
    }
}

#endif