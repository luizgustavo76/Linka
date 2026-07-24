import sqlite3
def get_db():
    conn = sqlite3.connect("DB/" + "notifications.db")
    return conn
def CreateNotification(from_user, receiver, datetime, type, content):
    conn = get_db()
    cur = conn.cursor()
    cur.execute("INSERT INTO notifications (receiver, from_user, datetime, type, content) VALUES (?,?,?,?,?)")