import login
import menu
import os
if not os.path.exists("config-login.cfg"):
    login.main_app()
else:
    menu.main_app()