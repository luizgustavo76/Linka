function feed(){
    document.getElementById("debug").innerHTML = "Loading...";
    Linka.httpGet("http://linkaProject.pythonanywhere.com/feed");
}

function receberResposta(txt){
    document.getElementById("debug").innerText = txt;

    let obj = JSON.parse(txt);

    let html = "";

    for (let i = 0; i < obj.length; i++) {
        html += "<div class='post'>" +
            "<b>" + obj[i].username + "</b><br>" +
            obj[i].text_post +
            "</div><hr>";
    }

    document.getElementById("saida").innerHTML = html;
}

function receberErro(err){
    document.getElementById("debug").innerHTML = err;
}