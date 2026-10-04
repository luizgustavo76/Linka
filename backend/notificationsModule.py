import sqlite3
import os

base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
os.makedirs(db_dir, exist_ok=True)

notifications_dir = os.path.join(db_dir, "notifications.db")
login_dir = os.path.join(db_dir, "login.db")

def get_db_login():
    conn = sqlite3.connect(login_dir)
    conn.row_factory = sqlite3.Row 
    return conn

def get_db():
    conn = sqlite3.connect(notifications_dir)
    conn.row_factory = sqlite3.Row 
    return conn

def CreateNotification(from_user, receiver, datetime_val, notif_type, content):
    if hasattr(from_user, '__getitem__') and not isinstance(from_user, (str, int)):
        try:
            from_user = from_user[0]
        except (IndexError, KeyError):
            from_user = str(from_user)

    if hasattr(receiver, '__getitem__') and not isinstance(receiver, (str, int)):
        try:
            receiver = receiver[0]
        except (IndexError, KeyError):
            receiver = str(receiver)

    from_user = str(from_user).strip() if from_user is not None else ""
    receiver = str(receiver).strip() if receiver is not None else ""

    if not receiver or receiver.lower() in ("none", "null", ""):
        return

    try:
        conn = get_db()
        cur = conn.cursor()
        cur.execute(
            "INSERT INTO notifications (receiver, from_user, datetime, type, content) VALUES (?,?,?,?,?)",
            (receiver, from_user, str(datetime_val), str(notif_type), str(content))
        )
        conn.commit()
        conn.close()
    except Exception:
        pass

def CreateNotificationForEveryone(from_user, datetime_val, notif_type, content):
    if hasattr(from_user, '__getitem__') and not isinstance(from_user, (str, int)):
        try:
            from_user = from_user[0]
        except (IndexError, KeyError):
            from_user = str(from_user)

    from_user = str(from_user).strip() if from_user is not None else ""

    try:
        # Busca todos os usuários cadastrados na tabela users do login.db
        conn_login = get_db_login()
        cur_login = conn_login.cursor()
        cur_login.execute("SELECT username FROM users WHERE username != ?", (from_user,))
        users = cur_login.fetchall()
        conn_login.close()

        if not users:
            return

        records = [
            (str(user[0]), from_user, str(datetime_val), str(notif_type), str(content))
            for user in users if user[0]
        ]

        if not records:
            return

        # Insere em lote em uma única transação no notifications.db
        conn = get_db()
        cur = conn.cursor()
        cur.executemany(
            "INSERT INTO notifications (receiver, from_user, datetime, type, content) VALUES (?,?,?,?,?)",
            records
        )
        conn.commit()
        conn.close()
    except Exception:
        pass