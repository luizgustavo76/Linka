function carregarFeed() {
    var xhr = new XMLHttpRequest();

    xhr.open("GET", "http://linkaProject.pythonanywhere.com/feed", true);

    xhr.timeout = 8000;

    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {

            if (xhr.status === 200) {
                try {
                    var data = JSON.parse(xhr.responseText);

                    var container = document.getElementById("feed");
                    container.innerHTML = "";

                    for (var i = 0; i < data.length; i++) {
                        var post = data[i];

                        var div = document.createElement("div");
                        div.className = "post";

                        div.innerHTML =
                            "<h4>" + post.username + "</h4>" +
                            "<p>" + post.text_post + "</p>";

                        container.appendChild(div);
                    }

                } catch (e) {
                    alert("Erro ao ler JSON");
                }

            } else {
                alert("Erro HTTP: " + xhr.status);
            }
        }
    };

    xhr.onerror = function () {
        alert("Erro de rede (não conectou)");
    };

    xhr.ontimeout = function () {
        alert("Timeout na requisição");
    };

    xhr.send();
}