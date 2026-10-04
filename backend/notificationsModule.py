import sqlite3
import os

# Unifica o caminho absoluto para o banco de dados
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
os.makedirs(db_dir, exist_ok=True)
notifications_dir = os.path.join(db_dir, "notifications.db")

def get_db():
    conn = sqlite3.connect(notifications_dir)
    conn.row_factory = sqlite3.Row 
    return conn

def CreateNotification(from_user, receiver, datetime_val, notif_type, content):
    print(f"\n[DEBUG CreateNotification] Chamado com:")
    print(f" -> from_user: {from_user} ({type(from_user)})")
    print(f" -> receiver:  {receiver} ({type(receiver)})")
    print(f" -> type:      {notif_type}")

    # Desempacota do sqlite3.Row, tupla ou lista vinda de fetchone()
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

    # Limpeza final das variáveis
    from_user = str(from_user).strip() if from_user is not None else ""
    receiver = str(receiver).strip() if receiver is not None else ""

    if not receiver or receiver.lower() in ("none", "null", ""):
        print(" [DEBUG CreateNotification] CANCELADO: 'receiver' está vazio ou é inválido!")
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
        print(f" [DEBUG CreateNotification] SUCESSO: Notificação inserida para receiver='{receiver}' no DB '{notifications_dir}'")
    except Exception as e:
        print(f" [DEBUG CreateNotification] ERRO AO INSERIR NO BANCO: {e}")