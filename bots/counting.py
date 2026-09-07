import linkaBotsSdk as bot
import sqlite3
from concurrent.futures import ThreadPoolExecutor
commands = [
    "/start",
    "/help"
]
def get_db():
    conn = sqlite3.connect("config.db")
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""CREATE TABLE IF NOT EXISTS config(
    group_id INTEGER,
    channel_name TEXT,
    actual_number INTEGER)""")
    conn.commit()
    conn.close()
create_db()
def accept_invite():
    inbox = bot.inbox()
    if "inbox" in inbox:
        for content in inbox["inbox"]: