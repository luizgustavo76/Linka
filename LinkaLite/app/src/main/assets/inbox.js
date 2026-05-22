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

function request_inbox(){
    var cfg = Linka.loadCfgAsJson("config-login.cfg");
    var username = cfg["FAST-LOGIN"]["username"];
    var url = cfg["SERVER"]["url"];
    json_inbox = {
        "username":username
    };
    Linka.httpPost(url + "/inbox", json_inbox);

}