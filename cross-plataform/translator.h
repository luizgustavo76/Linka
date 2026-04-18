#ifndef TRANSLATOR_H
#define TRANSLATOR_H

#include <nlohmann/json.hpp>
#include <fstream>
#include <iostream>
#include <string>

using json = nlohmann::json;

inline std::string lang = "en";

inline std::string translate(const std::string& section, const std::string& key)
{
    std::string path = "strings/login/" + lang + ".json";

    std::ifstream f(path);

    if (!f.is_open()) {
        std::cout << "ERRO: nao consegui abrir o arquivo: " << path << std::endl;
        return "???";
    }

    json data;
    f >> data;

    return data[section][key];
}

#endif