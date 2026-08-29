import os
import sqlite3
from flask import Blueprint

jobs_bp = Blueprint("jobs_bp", __name__)

# Pega o diretório exato onde o arquivo jobs.py está (ex: /home/LinkaProject/Linka/backend)
CURRENT_DIR = os.path.dirname(os.path.abspath(__file__))

# Aponta direto para a pasta DB dentro de backend
LOGIN_DB = os.path.join(CURRENT_DIR, "DB", "login.db")
PROFILE_DB = os.path.join(CURRENT_DIR, "DB", "profile.db")

def fix_login_ids():
    if not os.path.exists(LOGIN_DB):
        print(f"[ERRO] Banco não encontrado em: {LOGIN_DB}")
        return
        
    conn = sqlite3.connect(LOGIN_DB)
    cursor = conn.cursor()
    
    cursor.execute("SELECT username FROM login WHERE id IS NULL OR id = ''")
    null_users = cursor.fetchall()
    
    if null_users:
        cursor.execute("SELECT MAX(CAST(id AS INTEGER)) FROM login WHERE id IS NOT NULL AND id != ''")
        row = cursor.fetchone()
        max_id = row[0] if row and row[0] is not None else 0
        current_id = max_id + 1
        
        for user in null_users:
            cursor.execute("UPDATE login SET id = ? WHERE username = ?", (current_id, user[0]))
            current_id += 1
        conn.commit()
        print(f"[OK] {len(null_users)} IDs corrigidos no login.db!")
    else:
        print("[INFO] Todos os logins já possuem ID.")
        
    conn.close()

def sync_profiles():
    if not os.path.exists(LOGIN_DB) or not os.path.exists(PROFILE_DB):
        print(f"[ERRO] Verifique os caminhos: \nLOGIN: {LOGIN_DB}\nPROFILE: {PROFILE_DB}")
        return

    conn_login = sqlite3.connect(LOGIN_DB)
    cursor_login = conn_login.cursor()
    cursor_login.execute("SELECT username FROM login")
    logins = [row[0] for row in cursor_login.fetchall() if row[0]]
    conn_login.close()

    conn_profile = sqlite3.connect(PROFILE_DB)
    cursor_profile = conn_profile.cursor()
    
    # Busca perfis que já existem no profile.db para evitar duplicatas manuais
    cursor_profile.execute("SELECT username FROM profile")
    existing_profiles = {row[0] for row in cursor_profile.fetchall() if row[0]}

    inserted_count = 0
    for username in logins:
        if username not in existing_profiles:
            cursor_profile.execute(
                "INSERT INTO profile (username, bio, actual_badge, ProfilePicture, followers, following) VALUES (?, ?, NULL, NULL, 0, 0)",
                (username, f"Hi! my name is {username}")
            )
            inserted_count += 1
            
    conn_profile.commit()
    conn_profile.close()
    print(f"[OK] {inserted_count} novos perfis sincronizados no profile.db!")

def run_jobs():
    print("--- INICIANDO SYNC DE BANCOS ---")
    fix_login_ids()
    sync_profiles()
    print("--- CONCLUÍDO ---")

run_jobs()