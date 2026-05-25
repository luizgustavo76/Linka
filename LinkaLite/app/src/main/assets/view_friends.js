function receberResposta(txt) {
    var data = JSON.parse(txt);
    var friends = data["friends"];
    var html = "";
    for (let i = 0; i < friends.length; i++){
        html += "<div><a>" += friends[i] += "</a>"
    }
    document.getElementById("Friends").innerHTML = html;
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