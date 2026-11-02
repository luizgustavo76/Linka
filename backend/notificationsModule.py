import os
import psycopg2
from psycopg2.extras import RealDictCursor

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

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

    conn = get_db()
    cur = conn.cursor()
    try:
        cur.execute(
            """
            INSERT INTO notifications (receiver, from_user, datetime, type, content, read) 
            VALUES (%s, %s, %s, %s, %s, FALSE)
            """,
            (receiver, from_user, str(datetime_val), str(notif_type), str(content))
        )
        conn.commit()
    except Exception as e:
        conn.rollback()
        print(f"[ERRO CreateNotification]: {e}")
    finally:
        cur.close()
        conn.close()


def CreateNotificationForEveryone(from_user, datetime_val, notif_type, content):
    if hasattr(from_user, '__getitem__') and not isinstance(from_user, (str, int)):
        try:
            from_user = from_user[0]
        except (IndexError, KeyError):
            from_user = str(from_user)

    from_user = str(from_user).strip() if from_user is not None else ""

    conn = get_db()
    cur = conn.cursor()
    try:
        # Insere notificações diretamente no Postgres para todos os usuários cadastrados na tabela 'login'
        query = """
            INSERT INTO notifications (receiver, from_user, datetime, type, content, read)
            SELECT username, %s, %s, %s, %s, FALSE
            FROM login
            WHERE username != %s AND username IS NOT NULL AND username != '';
        """
        cur.execute(query, (from_user, str(datetime_val), str(notif_type), str(content), from_user))
        conn.commit()
    except Exception as e:
        conn.rollback()
        print(f"[ERRO CreateNotificationForEveryone]: {e}")
    finally:
        cur.close()
        conn.close()