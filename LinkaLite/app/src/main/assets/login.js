var lastResponse = null;
var statusCode = 0;

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

function login_request() {

    var cfg = JSON.parse(Linka.loadCfgAsJson("config-login.cfg"));

    var url = "http://linkaProject.pythonanywhere.com";

    if (cfg["SERVER"] && cfg["SERVER"]["url"]) {
        url = cfg["SERVER"]["url"];
    }

    var username = document.getElementById("username").value;
    var password = document.getElementById("password").value;

    var login_json = {
        "username": username,
        "password": password
    };

    Linka.httpPost(url + "/login", JSON.stringify(login_json));

    setTimeout(function(){

        if (!lastResponse){
            document.getElementById("saida").innerHTML = "error in the server side";
            return;
        }

        var obj = JSON.parse(lastResponse);

        if (statusCode == 200 || statusCode == 201){

            Linka.httpPost(url + "/new-session", JSON.stringify(login_json));

            setTimeout(function(){

                var sessionObj = JSON.parse(lastResponse);

                var token = sessionObj.token;

                var content = "";
                content += "[FAST-LOGIN]\n";
                content += "username=" + username + "\n";
                content += "token_session=" + token + "\n";

                Linka.saveCfg("config-login.cfg", content);

                window.location.replace("index.html");

            }, 600);

        } else {
            document.getElementById("saida").innerHTML =
                "<h3>The username or password is invalid</h3>";
        }

    }, 600);
}
function register_request(){
    var username = document.getElementById("reg-user").value;
    var password = document.getElementById("reg-pass").value;
    var email = document.getElementById("reg-email").value;
    var request_json = {
        "username":username,
        "password":password,
        "email":email
    };
    var cfg = JSON.parse(Linka.loadCfgAsJson("config-login.cfg"));
    var url = "http://linkaProject.pythonanywhere.com";

    if (cfg["SERVER"] && cfg["SERVER"]["url"]) {
        url = cfg["SERVER"]["url"];
    }
    Linka.httpPost(url + "/register", JSON.stringify(request_json))

    setTimeout(function(){

        if (!lastResponse){
            document.getElementById("saida").innerHTML = "error in the server side";
            return;
        }

        var obj = JSON.parse(lastResponse);

        if (statusCode == 200 || statusCode == 201){
            window.location.href = "file:///android_asset/index.html";
        }else{
            document.getElementById("saida").innerHTML =
                "<h3>The username or password is invalid</h3>";
        }
    })


}