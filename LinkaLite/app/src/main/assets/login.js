function receberResposta(txt){
    let obj = JSON.parse(txt);
}
function login_request() {

    var cfg = JSON.parse(Linka.loadCfgAsJson("config-login.cfg"));
    var url = cfg["SERVER"]["url"];
    if (url == null){
        var content = ""
        url = "http://linkaProject.pythonanywhere.com"
        content += "[SERVER]";
        content += "ulr=" + url;
        Linka.saveCfg("config-login.cfg", content);
    }
    var username = document.getElementById("username").value;
    var password = document.getElementById("password").value;
    login_json = {
        "username":username,
        "password":password
    };
    Linka.requestPOST(url + "/login", JSON.stringify(login_json))
    if (obj.status in (200, 201)){
        Linka.requestPOST(url + "/new-session", JSON.stringify(login_json))
        var token = obj.body["token"];
        var content = ""
        content += "[FAST-LOGIN]\n";
        content += "username=" + username;
        content += "token_session" + token;
        Linka.saveCfg("config-login.cfg", content);
        window.location.replace("index.html");
    }
    else{
        html="<h3>The username or password is invalid</h3>"
        document.getElementById("saida").innerHTML = html;
    };
}