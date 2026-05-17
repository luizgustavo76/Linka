function existsLogin(){
    var result = Linka.ensureFile("config-login.cfg");

    document.getElementById("debug").innerHTML = "ensureFile: " + result;

    var cfg = JSON.parse(Linka.loadCfgAsJson("config-login.cfg"));

    var username = null;
    var token_session = null;

    if (cfg["FAST-LOGIN"]) {
        username = cfg["FAST-LOGIN"]["username"];
        token_session = cfg["FAST-LOGIN"]["token_session"];
    }

    if (!username || !token_session){
        window.location.href = "file:///android_asset/login.html";
    } else {
        window.location.href = "file:///android_asset/index.html";
    }
}