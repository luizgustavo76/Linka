function receberResposta(txt){
    lastResponse = txt;
    document.getElementById("messages").innerHTML = txt;
}
function send_message(){
    var message = document.getElementById("messageInput").value;
    var cfg =
        JSON.parse(
            Linka.loadCfgAsJson("config-login.cfg")
        );
    var url = "http://linkaProject.pythonanywhere.com";

    if (cfg["SERVER"] && cfg["SERVER"]["url"]) {

        url = cfg["SERVER"]["url"];
    }

    var username =
        cfg["FAST-LOGIN"]["username"];
    json_send = {
        "sender":username,
        "message":message
    };
    Linka.httpPost(url + "/send-global-message", JSON.stringify(json_send));
}
function view_messages(){
    var last_id = 0;
    var cfg =
        JSON.parse(
            Linka.loadCfgAsJson("config-login.cfg")
        );
    var url = "http://linkaProject.pythonanywhere.com";

    if (cfg["SERVER"] && cfg["SERVER"]["url"]) {

        url = cfg["SERVER"]["url"];
    };
    var json_view = {
        "id":last_id,
    };
    Linka.httpPost(url + "/view-global-message", JSON.stringify(json_view));
}