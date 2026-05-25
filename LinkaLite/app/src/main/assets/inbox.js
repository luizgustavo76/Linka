function request_inbox(){

    var cfg = JSON.parse(
        Linka.loadCfgAsJson("config-login.cfg")
    );

    var username = cfg["FAST-LOGIN"]["username"];

    var url = "http://linkaProject.pythonanywhere.com";

    if (cfg["SERVER"] && cfg["SERVER"]["url"]) {
        url = cfg["SERVER"]["url"];
    }

    var json_inbox = {
        "username": username
    };

    Linka.httpPost(
        url + "/inbox",
        JSON.stringify(json_inbox)
    );
}