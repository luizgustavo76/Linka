import login
import menu
import os
import configparser
if not os.path.exists("config-login.cfg"):
    login.main_app()
else:
    config = configparser.ConfigParser()
    config.read("config-login.cfg")
    if config["FAST-LOGIN"]["username"] == "" or config["FAST-LOGIN"]["password"] == "":
        login.main_app()
    else:
        menu.main_app()