import json

class Translator:
    def __init__(self, json_file: str):
        """Initialize the translator with a JSON file"""
        self.json_file = json_file
        try:
            with open(json_file, "r", encoding="utf-8") as f:
                self.translations = json.load(f)
        except FileNotFoundError:
            raise Exception(f"Translation file not found: {json_file}")
        except json.JSONDecodeError:
            raise Exception(f"Error decoding JSON file: {json_file}")

    def translate(self, key: str) -> str:
        """
        Translate a key in the format 'section.key'.
        Example: 'login.username' -> returns 'Nome de usuário'
        """
        parts = key.split(".")
        data = self.translations

        try:
            for part in parts:
                data = data[part]
        except KeyError:
            return f"[Translation not found: {key}]"
        return data