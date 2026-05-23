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
function debug_chat(){
    var username = document.getElementById("username").value;
    var target_user = document.getElementById("target_user").value;
    var token = document.getElementById("token").value;
    json_debug = {
        "user1":username,
        "user2":target_user,
    };
    Linka.httpPost("http://linkaProject.pythonanywhere.com/view", JSON.stringify(json_debug));
}