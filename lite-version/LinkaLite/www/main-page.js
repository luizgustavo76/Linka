function carregarFeed() {
    var xhr = new XMLHttpRequest();

    xhr.open("GET", "http://127.0.0.1:5000/feed", true);

    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {

            if (xhr.status === 200) {
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

            } else {
                alert("Erro ao carregar feed: " + xhr.status);
            }
        }
    };

    xhr.send();
}