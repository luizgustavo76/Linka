var url = "http://linkaProject.pythonanywhere.com";

    if (cfg["SERVER"] && cfg["SERVER"]["url"]) {
        url = cfg["SERVER"]["url"];
    }
function feed(){
    document.getElementById("debug").innerHTML = "Loading feed...";
    Linka.httpGet(url + "/feed");
}
function requestStar(id, username){
    json_star = {
        "username":username,
        "post_id":id
    };
    Linka.httpPost(url + "/star", JSON.stringify(json_star));
    
}
function receberResposta(txt){
    document.getElementById("debug").innerText = txt;

    let obj = JSON.parse(txt);

    let html = "";

    for (let i = 0; i < obj.length; i++) {
        html += "<div class='post'>" +
            "<b>" + obj[i].username + "</b><br>" +
            obj[i].text_post + "<a onclick='requestStar(" + obj[i].id + ", \"" + obj[i].username + "\")'>Star</a>"
            "</div><hr>";
    }

    document.getElementById("saida").innerHTML = html;
}

function receberErro(err){
    document.getElementById("debug").innerHTML = err;
}