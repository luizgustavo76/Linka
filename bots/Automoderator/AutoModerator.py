import os
import json
import re
from pathlib import Path
from dotenv import load_dotenv
from linkaBotsSdk import LinkaBotSdk as bot_client
import time
load_dotenv()

bot = bot_client()
BOT_DIR = Path(__file__).resolve().parent
WORDS_JSON_PATH = BOT_DIR / "words.json"
MASTER_KEY = os.getenv("MASTER_KEY")

# Termos de ódio/spam gravíssimos para bloquear SEMPRE (mesmo se o words.json falhar)
CRITICAL_HATE_WORDS = [
    "hitler", "nigger", "niggers", "n1gger", "n1gg3r", "niga", "nigga",
    "nazism", "nazi", "heil", "white supremacy", "white supramacy", "kkk"
]

# Palavras normais em PT/ES para garantir que NUNCA sejam bloqueadas
IGNORE_WORDS = {"cara", "como", "estan", "estais", "hola", "bien", "escaravelho"}

# Carrega palavras do words.json se existir
json_words = []
if WORDS_JSON_PATH.exists():
    with open(WORDS_JSON_PATH, "r", encoding="utf-8") as f:
        json_words = json.load(f)

# Junta tudo, limpa os ignorados e termos de 1~2 letras
all_bad_words = set(CRITICAL_HATE_WORDS + json_words)
valid_words = [w for w in all_bad_words if w.lower() not in IGNORE_WORDS and len(w) > 2]

# Compila Regex para palavras exatas
pattern_str = r'\b(' + '|'.join(map(re.escape, valid_words)) + r')\b'
bad_words_regex = re.compile(pattern_str, re.IGNORECASE)

# Expressão para detectar português informal
PT_WORDS = {"olá", "opa", "eai", "bom", "dia", "tarde", "noite", "você", "tudo", "bem", "mano", "não", "sim", "obrigado", "vlw"}
pt_regex = re.compile(r'\b(' + '|'.join(map(re.escape, PT_WORDS)) + r')\b', re.IGNORECASE)

response_for_pt = (
    "Olá... aqui no linka recomendamos que fale inglês sabe, comunidade pequena e "
    "a maioria fala se puder se adaptar ao ingles ou conversar com outro que fala tambem ajudaria"
)

response_deleted_post = (
    "Your post was removed for violating the community guidelines "
    "(available at https://linkaProject.pythonanywhere.com/tos)\n"
    "If you believe this was a mistake, contact linka.plataform@gmail.com\n"
    "I am a bot, beep boop."
)

def normalize_text(text: str) -> str:
    """Substitui caracteres de leetspeak comuns para impedir desvios."""
    substitutions = {'@': 'a', '4': 'a', '1': 'i', '!': 'i', '0': 'o', '3': 'e', '$': 's'}
    normalized = text.lower()
    for char, replacement in substitutions.items():
        normalized = normalized.replace(char, replacement)
    return normalized

def main():
    while True:
        try:
            feed = bot.view_posts()
            if feed is not None:
                for post in feed:
                    post_id = post["id"]
                    text_post = post.get("text_post", "")
                    username = post.get("username", "")

                    # Ignora posts do próprio bot ou posts já deletados
                    if username == "AutoModerator" or "[ This post was removed" in text_post:
                        continue

                    # Checagem com texto normalizado
                    normalized_post = normalize_text(text_post)

                    if bad_words_regex.search(text_post) or bad_words_regex.search(normalized_post):
                        bot.delete_post(MASTER_KEY, post_id, response_deleted_post)
                        print(f"[AutoMod] Post #{post_id} moderado por violação de ToS.")
                        continue

                    if pt_regex.search(text_post):
                        bot.comment(response_for_pt, post_id)
                        print(f"[AutoMod] Aviso de idioma enviado para o post #{post_id}.")

        except Exception as e:
            print(f"[AutoMod Error] Erro durante a varredura: {e}")

        time.sleep(120)

if __name__ == "__main__":
    main()