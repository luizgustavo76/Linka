import json
import re
from linkaBotsSdk import LinkaBotSdk as bot_client
from dotenv import load_dotenv
import os
load_dotenv()
from pathlib import Path
bot = bot_client()
BOT_DIR = Path(__file__).resolve().parent
WORDS_JSON_PATH = BOT_DIR / "words.json"

# Abre o arquivo usando o caminho absoluto garantido
with open(WORDS_JSON_PATH, "r", encoding="utf-8") as f:
    words = json.load(f)
MASTER_KEY = os.getenv("MASTER_KEY")

sorted_words = sorted(words, key=len, reverse=True)
escaped_words = [re.escape(w) for w in sorted_words]
pattern = r'\b(' + '|'.join(escaped_words) + r')\b'
bad_words_regex = re.compile(pattern, re.IGNORECASE)

pt_regex = re.compile(r'\b(olá|opa|eai|cara|bom|dia|tarde|noite|como|você|tudo|bem|mano|não|sim|obrigado|vlw)\b', re.IGNORECASE)

def moderate_text(text: str) -> str:
    if bad_words_regex.search(text):
        return "[ This post was removed by AutoModerator. Read ToS: https://linkaProject.pythonanywhere.com/tos ]"
    return text

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

def main():
    feed = bot.view_posts()
    if feed is not None:
        for post in feed:
            post_id = post["id"]
            text_post = post["text_post"]
            username = post.get("username", "")
            if username == "AutoModerator" or "[ This post was removed" in text_post:
                continue
            if bad_words_regex.search(text_post):
                bot.delete_post(post_id, MASTER_KEY, response_deleted_post)
                bot.comment(post_id, response_deleted_post)
                print(f"[AutoMod] Post #{post_id} moderado por violação de ToS.")
                continue

            if pt_regex.search(text_post):
                bot.comment(post_id, response_for_pt)
                print(f"[AutoMod] Aviso de idioma enviado para o post #{post_id}.")

main()