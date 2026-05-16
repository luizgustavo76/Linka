function receberResposta(txt){
    let obj = JSON.parse(txt);
}
function login_request() {
    var url = Linka.loadCfgAsJson("config-login.cfg");
    var username = document.getElementById("username").value;
    var password = document.getElementById("password").value;
    login_json = {
        "username":username,
        "password":password
    };
    Linka.requestPOST(url + "/login", login_json)
    if (obj.status in (200, 201)){
        Linka.requestPOST(url + "/new-session", login_json)
        var token = obj.body["token"];
        var content = ""
        content += "[FAST-LOGIN]\n";
        content += "username=" + username;
        content += "token_session" + token;
        Linka.saveCfg("config-login.cfg", content)
        window.location.replace("index.html");
    }
    else{
        html="<h3>The username or password is invalid</h3>"
        document.getElementById("saida").innerHTML = html;
    };
}