import sqlite3
import os
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
def get_db():
    conn = sqlite3.connect(db_dir + "/mentions.db")
    return conn
def createMention(username, author, content, post_id, comment_id, type):
    conn = get_db()
    cur = conn.cursor()
    cur.execute("INSERT INTO MENTIONS (username , author, content, post_id, comment_id, type) VALUES(?, ?, ?, ?, ?, ?)",(username , author, content, post_id, comment_id, type))
    conn.close()
    conn.commit()