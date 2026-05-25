var lastResponse = null;
var statusCode = 0;

function receberStatusCode(code){
    statusCode = code;
}

function receberResposta(txt){
    lastResponse = txt;
    document.getElementById("commentOutput").innerHTML = txt;
}

function receberErro(txt){
    document.getElementById("saida").innerHTML = "ERRO: " + txt;
}

function new_comments_request(){
    var cfg = JSON.parse(Linka.loadCfgAsJson("config-login.cfg"));
    var username = cfg["FAST-LOGIN"]["username"];
    var url = cfg["FAST-LOGIN"]["username"];
    if (url == null){
        url = "http://linkaProject.pythonanywhere.com"
    };
    var text_comment = document.getElementById("text_comment");
    var post_id = document.getElementById("post_id");
    json_commment = {
        "username":username,
        "text_comment":text_comment,
        "post_id":post_id
    };
    Linka.httpPost(url + "/comments", JSON.stringify(json_commment));
}