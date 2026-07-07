import requests
import datetime
class linkaBotsSdk:
    #defines the essential data of a bot
    def __init__(self):
        self.bot_id = ""
        self.bot_name = ""
        self.bot_password = ""
        self.bot_token = ""
        self.url = ""
        self.friends = []
    #function to send a message
    def sendMessage(self, message, receiver, token):
        json_send = {
            "sender":self.bot_name,
            "message":message,
            "receiver":receiver
        }
        headers = {
            "Authorization": f"Bearer {self.bot_token}"
        }
        response_send = requests.post(
            url=self.url,
            json=json_send,
            timeout=10,
            headers=headers
        )
        print("MESSAGE SENT")
    