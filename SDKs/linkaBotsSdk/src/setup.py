import requests
url = ""
def login():
    while True:
        print("type the username:")
        username = input()
        print("type the password")
        password = input()
        response = requests.post(
            url + "/login",
            json={
                "username":username,
                "password":password
            }
        )
        if response.status_code == 200:
            json_write = {
                "url":url,
                "username":username,
                "password":password
            }
            with open("config-login.json", "w") as f:
                f.write(json_write)
            print(f"welcome {username}")
        else:
            break
def register():
    while True:
            print("type the username:")
            username = input()
            print("type the password")
            password = input()
            print("retype the password")
            retype_password = input()
            print("type your email")
            email = input()
            if password == retype_password:
                response = requests.post(
                    url + "/register",
                    json={
                        "username":username,
                        "password":password,
                        "email":email
                    }
                )
                if response.status_code == 200:
                    print(f"welcome {username}")
                    json_write = {
                        "url":url,
                        "username":username,
                        "password":password
                    }
                    with open("config-login.json", "w") as f:
                        f.write(json_write)
                else:
                    break
def main():
    while True:
        print("type the url server or [0] for default linka server")
        url = input()
        if url == "0":
            url = "https://linkaProject.pythonanywhere.com"
        print("welcome to linka bot setup:")
        print("[1] use an existing account")
        print("[2] create a new account")
        login_options = input()
        if login_options == "1":
            login()
        if login_options == "2":
            register()