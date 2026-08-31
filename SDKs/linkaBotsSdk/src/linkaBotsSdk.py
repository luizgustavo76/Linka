import json
import os
import requests
import setup
import token_manager
class LinkaBotSdk:
    def __init__(self):
        if not os.path.exists("config-login.json"):
            print("[SDK] Configuration not found. Starting setup...")
            setup.main()
        self._load_config()
    def _load_config(self):
        try:
            with open("config-login.json", "r") as f:
                json_login = json.load(f)
            self.name = json_login.get("username")
            self.password = json_login.get("password")
            self.url = json_login.get("url", "").rstrip("/")
            self.token = token_manager.new_session(self.url, self.name, self.password)
        except Exception as e:
            print(f"[SDK Error] Failed to load configuration: {e}")
    def _get_bearer_headers(self) -> dict:
        token_str = self.token.get("token") if isinstance(self.token, dict) else str(self.token) if self.token else ""
        return {"Authorization": f"Bearer {token_str}"}
    def get_last_interaction(self, receiver: str):
        try:
            response = requests.post(f"{self.url}/view", json={"user1": self.name, "user2": receiver}, headers=self._get_bearer_headers(), timeout=10)
            if response.status_code == 200:
                messages_array = response.json().get("messages", [])
                return messages_array[-1] if messages_array else None
            return None
        except Exception as e:
            print(f"[SDK Error] Failed to get last interaction with {receiver}: {e}")
            return None
    def view_friends(self) -> list:
        try:
            my_name = str(self.name) if self.name else ""
            token_str = self.token.get("token") if isinstance(self.token, dict) else str(self.token) if self.token else ""
            payload = {"username": my_name, "token": token_str}
            response = requests.post(f"{self.url}/friends", json=payload, headers=self._get_bearer_headers(), timeout=10)
            if response.status_code == 200:
                friends_pairs = response.json().get("friends", [])
                list_friends = []
                for pair in friends_pairs:
                    if isinstance(pair, (list, tuple)):
                        for user in pair:
                            if user and isinstance(user, str) and user.lower() != my_name.lower():
                                if user not in list_friends:
                                    list_friends.append(user)
                return list_friends
            print(f"[SDK Error] Failed to fetch friends. Status: {response.status_code} | {response.text}")
            return []
        except Exception as e:
            print(f"[SDK Error] Connection error in view_friends: {e}")
            return []
    def send_chat(self, message: str, receiver: str) -> bool:
        try:
            response = requests.post(f"{self.url}/send-message", json={"sender": self.name, "receiver": receiver, "message": message}, headers=self._get_bearer_headers(), timeout=10)
            return response.status_code == 200
        except requests.exceptions.RequestException as e:
            print(f"[SDK Error] Error sending message to {receiver}: {e}")
            return False
    def get_last_message(self, receiver: str):
        try:
            response = requests.post(f"{self.url}/view", json={"user1": self.name, "user2": receiver}, headers=self._get_bearer_headers(), timeout=10)
            if response.status_code == 200:
                messages_array = response.json().get("messages", [])
                if messages_array:
                    return messages_array[-1].get("message")
            return None
        except requests.exceptions.RequestException as e:
            print(f"[SDK Error] Error fetching message history with {receiver}: {e}")
            return None
if __name__ == "__main__":
    bot = LinkaBotSdk()