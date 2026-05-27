var lastResponse = null;
var statusCode = 0;

function receberStatusCode(code){
    statusCode = code;
}

function receberResposta(txt){

    lastResponse = txt;

    messageFrame(txt);

}

function receberErro(txt){
    document.getElementById("saida").innerHTML = "ERRO: " + txt;
}
function messageFrame(txt){

    lastResponse = txt;

    var json_response = JSON.parse(txt);

    var cfg =
        JSON.parse(
            Linka.loadCfgAsJson("config-login.cfg")
        );

    var username =
        cfg["FAST-LOGIN"]["username"];

    var html = "";

    for(var i = 0; i < json_response.length; i++){

        var msg = json_response[i];

        var classe = "other";

        if(msg.sender == username){
            classe = "mine";
        }

        html +=
            "<div class='msg " + classe + "'>" +
            "<b>" + msg.sender + "</b><br>" +
            msg.message +
            "</div>";
    }

    document.getElementById("messagePage").innerHTML =
        html;
}
function send_message(){
    var cfg = JSON.parse(Linka.loadCfgAsJson("config-login.cfg"));
    var url = "http://linkaProject.pythonanywhere.com";
    if (cfg["SERVER"] && cfg["SERVER"]["url"]) {
        url = cfg["SERVER"]["url"];
    };
    var username = cfg["FAST-LOGIN"]["username"];
    var message = document.getElementById("messageInput").value;
    var target_user = document.getElementById("target_user").value;
    json_send = {
        "sender":username,
        "receiver":target_user,
        "message":message
    };
    Linka.httpPost(url + "/send-message", JSON.stringify(json_send));
}   
function get_chat(){
    var cfg = JSON.parse(Linka.loadCfgAsJson("config-login.cfg"));
    var url = "http://linkaProject.pythonanywhere.com";
    if (cfg["SERVER"] && cfg["SERVER"]["url"]) {
        url = cfg["SERVER"]["url"];
    };
    var username = cfg["FAST-LOGIN"]["username"];
    var token = cfg["FAST-LOGIN"]["token_session"];
    var target_user = document.getElementById("target_user").value;
    json_view = {
        "user1":username,
        "user2":target_user
    };
    Linka.httpPost(url + "/view", JSON.stringify(json_view));
    
}