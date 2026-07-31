import requests
from datetime import datetime
import token_manager
import json
import setup
import asyncio
class linkaBotSdk:
    def __init__(self):
        try:
            with open("config-login.json", "r") as f:
                json_login = json.load(f)
            self.name = json_login.get("username")
            self.password = json_login.get("password")
            self.url = json_login.get("url")
            self.token = token_manager.new_session()
        except:
            setup.main()
    def send_chat(self, message, receiver):
        headers = {
            "Authorization":f"Bearer {self.token}"
        }
        requests.post(
            self.url + "/send-message",
            json={
                "sender":self.name,
                "receiver":receiver,
                "message":message
            },
            headers=headers,
            timeout=10
        )
    def get_last_message(self, receiver):
        headers = {
            "Authorization":f"Bearer {self.token}"
        }
        response = requests.post(
            self.url + "/view",
            json={
                "user1":self.name,
                "user2":receiver
            },
            headers=headers,
            timeout=10
        )
        response_json = response.json()
        messages_array = response_json.get("messages")
        last_message = messages_array[-1].get("message")
        return last_message

    