var lastResponse = null;
var statusCode = 0;

function receberStatusCode(code){
    statusCode = code;
}

function receberResposta(txt) {
    let data = JSON.parse(txt);

    document.getElementById("usernameShow").innerText = data.username;
    document.getElementById("bio").innerText = data.bio;
    document.getElementById("create_at").innerText = data.created_at;
    document.getElementById("saida").innerText = txt;
}

function receberErro(txt){
    document.getElementById("saida").innerHTML = "ERRO: " + txt;
}
function getUserFromURL() {
    const params = new URLSearchParams(window.location.search);
    return params.get("user");
}

function loadProfile() {
    var user = getUserFromURL();

    if (!user) {
        var cfg = JSON.parse(Linka.loadCfgAsJson("config-login.cfg"));
        user = cfg["FAST-LOGIN"]["username"];
    }

    document.getElementById("usernameShow").innerText = user;

    var url = "http://linkaProject.pythonanywhere.com/view_profile?username=" + user;
    Linka.httpGet(url);
}