import json
import requests


def login(url):
    while True:
        print("type the username:")
        username = input()
        print("type the password:")
        password = input()

        response = requests.post(
            f"{url}/login", json={"username": username, "password": password}
        )

        if response.status_code == 200:
            json_write = {"url": url, "username": username, "password": password}
            with open("config-login.json", "w") as f:
                json.dump(json_write, f, indent=4)
            print(f"welcome {username}")
            break
        else:
            print("Login failed. Check your credentials.")
            break


def register(url):
    while True:
        print("type the username:")
        username = input()
        print("type the password:")
        password = input()
        print("retype the password:")
        retype_password = input()
        print("type your email:")
        email = input()

        if password == retype_password:
            response = requests.post(
                f"{url}/register",
                json={"username": username, "password": password, "email": email},
            )

            if response.status_code == 200:
                print(f"welcome {username}")
                json_write = {
                    "url": url,
                    "username": username,
                    "password": password,
                }
                with open("config-login.json", "w") as f:
                    json.dump(json_write, f, indent=4)
                break
            else:
                print(
                    f"Registration failed with status code {response.status_code}."
                )
                break
        else:
            print("Passwords do not match. Try again.\n")


def main():
    while True:
        print("type the url server or [0] for default linka server:")
        url_option = input().strip()

        if url_option == "0":
            url = "https://linkaProject.pythonanywhere.com"
        else:
            url = url_option
        url = url.rstrip("/")

        print("\nwelcome to linka bot setup:")
        print("[1] use an existing account")
        print("[2] create a new account")
        login_options = input().strip()

        if login_options == "1":
            login(url)
            break
        elif login_options == "2":
            register(url)
            break


if __name__ == "__main__":
    main()