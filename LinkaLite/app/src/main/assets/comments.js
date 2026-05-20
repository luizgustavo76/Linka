function request_comments(post_id){
    json_comment = {"post_id":post_id}
    Linka.httpPost(url + "/view-comments");
    var obj = JSON.parse(txt);
    const comments = obj["comments"];
    for (var i = 0; i < comments.length; i++){
        var html = "";
        var username = comments[i]["username"];
        var text_comment = comments[i]["text_comment"];
        var comment_id = comments[i]["comment_id"];
        html += '<div class="post">' + '<b>' + username + "</b>" +"<br>" + "<p>" + text_comment + "</p><br>";
        document.getElementById("commments").innerHTML = html;
    }
}
