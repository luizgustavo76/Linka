import json
import os
import requests

import setup
import token_manager


class LinkaBotSdk:

    def __init__(self):
        # 1. Se a configuração não existe, executa o setup primeiro
        if not os.path.exists("config-login.json"):
            print("[SDK] Configuração não encontrada. Iniciando setup...")
            setup.main()

        # 2. Carrega as credenciais para o SDK
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
            print(f"[SDK Error] Falha ao carregar configurações: {e}")
    def get_last_interaction(self, receiver):
        headers = {"Authorization": f"Bearer {self.token}"}
        try:
            response = requests.post(
                f"{self.url}/view",
                json={"user1": self.name, "user2": receiver},
                headers=headers,
                timeout=10,
            )
            if response.status_code == 200:
                response_json = response.json()
                messages_array = response_json.get("messages", [])
                if messages_array:
                    return messages_array[-1]
            return None
        except Exception as e:
            print(f"[SDK Error] Failed to get last interaction with {receiver}: {e}")
            return None
    def view_friends(self):
        try:
            # Garante que self.name seja uma string e evita erro se for None
            my_name = str(self.name) if self.name else ""

            # Converte o token com segurança
            token_str = (
                self.token.get("token")
                if isinstance(self.token, dict)
                else str(self.token)
            )

            headers = {"Authorization": f"Bearer {token_str}"}
            payload = {"username": my_name, "token": token_str}

            response = requests.post(
                f"{self.url}/friends", json=payload, headers=headers, timeout=10
            )

            if response.status_code == 200:
                data = response.json()
                friends_pairs = data.get("friends", [])

                list_friends = []
                for pair in friends_pairs:
                    # Garante que 'pair' é uma lista ou tupla
                    if isinstance(pair, (list, tuple)):
                        for user in pair:
                            # Filtra valores None, vazios e o próprio nome do bot
                            if (
                                user
                                and isinstance(user, str)
                                and user.lower() != my_name.lower()
                            ):
                                if user not in list_friends:
                                    list_friends.append(user)

                return list_friends

            print(
                f"[SDK Error] Falha ao buscar amigos. Status: {response.status_code} | {response.text}"
            )
            return []

        except Exception as e:
            print(f"[SDK Error] Erro de conexão em view_friends: {e}")
            return []
    def send_chat(self, message: str, receiver: str) -> bool:
        headers = {"Authorization": f"Bearer {self.token}"}
        try:
            response = requests.post(
                f"{self.url}/send-message",
                json={
                    "sender": self.name,
                    "receiver": receiver,
                    "message": message,
                },
                headers=headers,
                timeout=10,
            )
            return response.status_code == 200
        except requests.exceptions.RequestException as e:
            print(f"[SDK Error] Erro ao enviar mensagem para {receiver}: {e}")
            return False

    def get_last_message(self, receiver: str):
        headers = {"Authorization": f"Bearer {self.token}"}
        try:
            response = requests.post(
                f"{self.url}/view",
                json={"user1": self.name, "user2": receiver},
                headers=headers,
                timeout=10,
            )

            if response.status_code == 200:
                response_json = response.json()
                messages_array = response_json.get("messages", [])

                # Evita IndexError caso o histórico de mensagens esteja vazio
                if messages_array:
                    return messages_array[-1].get("message")

            return None
        except requests.exceptions.RequestException as e:
            print(f"[SDK Error] Erro ao buscar histórico com {receiver}: {e}")
            return None


if __name__ == "__main__":
    bot = LinkaBotSdk()