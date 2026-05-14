function feed(){
    Linka.httpGet("http://linkaProject.pythonanywhere.com/feed", function(txt) {
        let obj = JSON.parse(txt);

        let html = "";
        for (let i = 0; i < obj.length; i++) {
            html +=
                "<div class='post'>" +
                "<b>" + obj[i].user + "</b><br>" +
                obj[i].text +
                "</div><hr>";
        }

        document.getElementById("saida").innerHTML = html;
    });
}