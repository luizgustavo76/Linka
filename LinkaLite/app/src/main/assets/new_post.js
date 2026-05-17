function new_post(){
    var text_post = document.getElementById("text_post").value;
    var cfg = JSON.parse(Linka.loadCfgAsJson("config-login.cfg"));
    var username = "Linka";
    var url = "http://linkaProject.pythonanywhere.com"
    if (url == null){
        url = "http://linkaProject.pythonanywhere.com"
    };
    var now = new Date();
    json_post = {
        "username":username,
        "text_post":text_post,
        "datetime":now.toISOString()
    };
    Linka.httpPost(url + "/new", JSON.stringify(json_post));
};