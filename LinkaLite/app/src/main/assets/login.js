function login_request() {
    var username = document.getElementById("user").value;
    var password = document.getElementById("pass").value;

    var xhr = new XMLHttpRequest();
    xhr.open("POST", "http://linkaProject.pythonanywhere.com/login", true);
    xhr.setRequestHeader("Content-Type", "application/json");

    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {
            if (xhr.status === 200) {
                var data = JSON.parse(xhr.responseText);
                alert("Login feito com sucesso");
            } else {
                alert("Falha no login");
            }
        }
    };

    var body = JSON.stringify({
        username: username,
        password: password
    });

    xhr.send(body);
}