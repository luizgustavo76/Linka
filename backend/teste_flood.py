import requests
while True:
    json_post = {
        "username":"flooder",
        "text_post":"hduhdihdjdid",
    }
    response = requests.post(
        "http://linkaProject.pythonanywhere.com/new",
        json=json_post
    )
    print(response.status_code)
    print(response)