function new_post(){
    var text_post = document.getElementById("text_post").value;
    var cfg = JSON.parse(Linka.loaCfgAsJson("config-login.cfg"));
    var username = cfg["FAST-LOGIN"]["username"];
    var url = cfg["SERVER"]["url"];
    var now = new Date();
    json_post = {
        "username":username,
        "text_post":text_post,
        "datetime":now
    };
    Linka.httpPost(url + "/new", json_post);
};