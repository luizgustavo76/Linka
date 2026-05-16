function existsLogin(){
    var exists = Linka.fileExists("config-login.cfg");
    if (exists == true){
        return null;
    }else{
        Linka.createEmptyFile("config-login.cfg")
    };
    var cfg = JSON.parse(Linka.loadCfgAsJson("config-login.cfg"));
    var username = cfg["FAST-LOGIN"]["username"];
    var token_session = cfg["FAST-LOGIN"]["token_session"];
    if (username == null || token_session == null){
        window.location.replace("login.html");
    }else{
        window.location.replace("index.html");
    }
}