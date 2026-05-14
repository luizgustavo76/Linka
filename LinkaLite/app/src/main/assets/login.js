function login_request() {
    var username = document.getElementById("username").value;
    var password = document.getElementById("password").value;
    linka.httpGet("http://linkaProject.pythonanywhere.com/login?username=" + username + "?password=" + password);
}