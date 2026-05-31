var lastResponse = null;
var statusCode = 0;

var currentURL = "";
var currentUsername = "";

function receberStatusCode(code){
    statusCode = code;
}

function receberResposta(txt){

    lastResponse = txt;

    document.getElementById("saida").innerHTML = txt;

    try {

        var json_response = JSON.parse(txt);

        // resposta do login
        if (json_response["status"] == "login is sucessful") {

            document.getElementById("saida").innerHTML =
                "Login successful! Creating session...";

            return;
        }

        // resposta do token/session
        if (json_response["token"]) {

            var token = json_response.token;

            var content = "";

            content += "[SERVER]\n";
            content += "url=" + currentURL + "\n\n";

            content += "[FAST-LOGIN]\n";
            content += "username=" + currentUsername + "\n";
            content += "token_session=" + token + "\n";

            Linka.saveCfg("config-login.cfg", content);

            window.location.replace("file:///android_asset/index.html");

            return;
        }

    } catch(e){

        document.getElementById("saida").innerHTML =
            "JSON ERROR: " + e;
    }
}

function receberErro(txt){

    document.getElementById("saida").innerHTML =
        "ERRO: " + txt;
}

function login_request() {

    var cfg = JSON.parse(
        Linka.loadCfgAsJson("config-login.cfg")
    );

    var url = "http://linkaProject.pythonanywhere.com";

    if (cfg["SERVER"] && cfg["SERVER"]["url"]) {

        url = cfg["SERVER"]["url"];
    }

    var username =
        document.getElementById("username").value;

    var password =
        document.getElementById("password").value;

    currentURL = url;
    currentUsername = username;

    var login_json = {

        "username": username,
        "password": password
    };

    Linka.httpPost(
        url + "/login",
        JSON.stringify(login_json)
    );

    setTimeout(function(){

        var response = lastResponse;

        var code = statusCode;

        if (!response){

            document.getElementById("saida").innerHTML =
                "error in server side";

            return;
        }

        if (
            Number(code) === 200 ||
            Number(code) === 201
        ){

            Linka.httpPost(
                url + "/new-session",
                JSON.stringify(login_json)
            );

        } else {

            document.getElementById("saida").innerHTML =
                "<h3>The username or password is invalid</h3>";
        }

    }, 900);
}

function register_request(){

    var username =
        document.getElementById("reg-user").value;

    var password =
        document.getElementById("reg-pass").value;

    var email =
        document.getElementById("reg-email").value;

    var request_json = {

        "username": username,
        "password": password,
        "email": email
    };

    var cfg = JSON.parse(
        Linka.loadCfgAsJson("config-login.cfg")
    );

    var url = "http://linkaProject.pythonanywhere.com";

    if (cfg["SERVER"] && cfg["SERVER"]["url"]) {

        url = cfg["SERVER"]["url"];
    }

    Linka.httpPost(
        url + "/register",
        JSON.stringify(request_json)
    );

    setTimeout(function(){

        var response = lastResponse;

        var code = statusCode;

        
        if (
            code == 200 ||
            code == 201
        ){

            var content = "";

            content += "[FAST-LOGIN]\n";

            content +=
                "username=" + username + "\n";

            content +=
                "password=" + password + "\n";

            Linka.saveCfg(
                "config-login.cfg",
                content
            );

            window.location.href =
                "file:///android_asset/index.html";

        } else {

            document.getElementById("saida").innerHTML =
                "<h3>The username or password is invalid</h3>";
        }

    }, 1600);
}