function view_my_profile(){
    var cfg = JSON.parse(Linka.loadCfgAsJson("config-login.cfg"));
    var username = cfg["FAST-LOGIN"]["username"];
    document.getElementById("username").innerHTML = "<h2>" + username + "</h2>";
}