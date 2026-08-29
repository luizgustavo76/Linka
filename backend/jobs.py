import os
import sqlite3
from flask import Blueprint

jobs_bp = Blueprint("jobs_bp", __name__)

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LOGIN_DB = os.path.join(BASE_DIR, "DB", "login.db")
PROFILE_DB = os.path.join(BASE_DIR, "DB", "profile.db")

def fix_login_ids():
    if not os.path.exists(LOGIN_DB):
        return
    conn = sqlite3.connect(LOGIN_DB)
    cursor = conn.cursor()
    cursor.execute("SELECT username FROM login WHERE id IS NULL OR id = ''")
    null_users = cursor.fetchall()
    if null_users:
        cursor.execute("SELECT MAX(CAST(id AS INTEGER)) FROM login WHERE id IS NOT NULL AND id != ''")
        max_id = cursor.fetchone()[0]
        current_id = 1 if max_id is None else max_id + 1
        for user in null_users:
            cursor.execute("UPDATE login SET id = ? WHERE username = ?", (current_id, user[0]))
            current_id += 1
        conn.commit()
    conn.close()

def sync_profiles():
    if not os.path.exists(LOGIN_DB) or not os.path.exists(PROFILE_DB):
        return
    conn_login = sqlite3.connect(LOGIN_DB)
    cursor_login = conn_login.cursor()
    cursor_login.execute("SELECT username FROM login")
    logins = [row[0] for row in cursor_login.fetchall() if row[0]]
    conn_login.close()

    conn_profile = sqlite3.connect(PROFILE_DB)
    cursor_profile = conn_profile.cursor()
    
    for username in logins:
        cursor_profile.execute(
            "INSERT OR IGNORE INTO profile (username, bio, actual_badge, ProfilePicture, followers, following) VALUES (?, ?, NULL, NULL, 0, 0)",
            (username, f"Hi! my name is {username}")
        )
    conn_profile.commit()
    conn_profile.close()

def run_jobs():
    fix_login_ids()
    sync_profiles()

run_jobs()