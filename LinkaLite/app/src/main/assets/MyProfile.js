function view_my_profile(){
    var cfg = JSON.parse(Linka.loadCfgAsJson("config-login.cfg"));
    var username = cfg["FAST-LOGIN"]["username"];

    window.location.href = "profileTemplate.html?user=" + username;
}