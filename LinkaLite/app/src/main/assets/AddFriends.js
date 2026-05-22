function request_add_friends(){
    var cfg = JSON.parse(Linka.loadCfgAsJson("config-login.cfg"));
    var url = "http://linkaProject.pythonanywhere.com";
    if (cfg["SERVER"] && cfg["SERVER"]["url"]) {
        url = cfg["SERVER"]["url"];
    };
    var sender = cfg["FAST-LOGIN"]["username"];
    var username = document.getElementById("username").value;
    var message = document.getElementById("message").value;
    json_friends = {
        "receiver":username,
        "message":message,
        "sender":sender
    };
    Linka.httpPost(url + "/feed", JSON.stringify(json_friends));
};