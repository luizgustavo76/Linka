var lastResponse = null;
var statusCode = 0;

function receberStatusCode(code){
    statusCode = code;
}

function receberResposta(txt){
    lastResponse = txt;
    document.getElementById("saida").innerHTML = txt;
}

function receberErro(txt){
    document.getElementById("saida").innerHTML = "ERRO: " + txt;
}
    
function get_chat(){
    var cfg = JSON.parse(Linka.loadCfgAsJson("config-login.cfg"));
    var url = "http://linkaProject.pythonanywhere.com";
    if (cfg["SERVER"] && cfg["SERVER"]["url"]) {
        url = cfg["SERVER"]["url"];
    };
    var username = cfg["FAST-LOGIN"]["username"];
    var token = cfg["FAST-LOGIN"]["token_session"];
    var target_user = document.getElementById("target_user");
    json_view = {
        "user1":username,
        "user2":target_user
    };
    Linka.httpPost(url + "/view", JSON.stringify(json_view));
    
}