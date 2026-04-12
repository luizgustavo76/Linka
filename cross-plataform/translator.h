#ifndef TRANSLATOR_H
#define TRANSLATOR_H

#include <nlohmann/json.hpp>
#include <fstream>
#include <iostream>
#include <string>
using json = nlohmann::json;
std::string lang = "";
std::ifstream f("strings/login/en.json");
int define_lang()
{
    if (lang == "pt-br"){
        std::ifstream f("strings/login/pt-br.json");
    }
    if (lang == "en"){
        std::ifstream f("strings/login/en.json");
    }
    return 0;
}
inline std::string translate(const std::string& section, const std::string& key)
{
    json data;
    f >> data;
    return data[section][key];
    return 0;
}
#endif