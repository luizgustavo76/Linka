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
function request_comments(){
    const params = new URLSearchParams(window.location.search);
    const post_id = params.get("post_id");
    json_comment = {"post_id":post_id};
    Linka.httpPost(url + "/view-comments", JSON.stringify(json_comment));
    var obj = JSON.parse(txt);
    const comments = obj["comments"];
    for (var i = 0; i < comments.length; i++){
        var html = "";
        var username = comments[i]["username"];
        var text_comment = comments[i]["text_comment"];
        var comment_id = comments[i]["comment_id"];
        html += '<div class="post">' + '<b>' + username + "</b>" +"<br>" + "<p>" + text_comment + "</p><br>";
        document.getElementById("comments").innerHTML = html;
    }
}
