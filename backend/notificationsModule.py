import sqlite3

def get_db():
    conn = sqlite3.connect("DB/notifications.db")
    conn.row_factory = sqlite3.Row 
    return conn

def CreateNotification(from_user, receiver, datetime, type, content):
    if hasattr(from_user, '__getitem__') and not isinstance(from_user, (str, int)):
        from_user = from_user[0]
        
    if hasattr(receiver, '__getitem__') and not isinstance(receiver, (str, int)):
        receiver = receiver[0]

    conn = get_db()
    cur = conn.cursor()
    
    cur.execute(
        "INSERT INTO notifications (receiver, from_user, datetime, type, content) VALUES (?,?,?,?,?)",
        (str(receiver), str(from_user), str(datetime), str(type), str(content))
    )
    
    conn.commit()
    conn.close()