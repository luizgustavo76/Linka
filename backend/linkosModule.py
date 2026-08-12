import sqlite3
def get_db():
    conn = sqlite3.connect("DB/" + "profile.db")
    return conn
def add_linkos(username, quantity):
    conn = get_db()
    cur = conn.cursor()
    cur.execute("UPDATE linkos_table SET linkos = ? WHERE username = ?",(quantity, username))
    conn.commit()
