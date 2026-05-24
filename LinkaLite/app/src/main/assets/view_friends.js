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
function view_friends(){
    var cfg = JSON.parse(Linka.loadCfgAsJson("config-login.cfg"));
    var url = "http://linkaProject.pythonanywhere.com"
    var username = cfg["FAST-LOGIN"]["username"];
    json_view = {"username":username};
    Linka.httpPost(url + "/friends", JSON.stringify(json_view));
};