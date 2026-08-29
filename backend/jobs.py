import os
import sqlite3
from flask import Blueprint

jobs_bp = Blueprint("jobs_bp", __name__)

LOGIN_DB = os.path.join("backend", "DB", "login.db")
PROFILE_DB = os.path.join("backend", "DB", "profile.db")

def fix_login_ids():
    conn = sqlite3.connect(LOGIN_DB)
    cursor = conn.cursor()
    cursor.execute("SELECT username FROM login WHERE id IS NULL")
    null_users = cursor.fetchall()
    if null_users:
        cursor.execute("SELECT MAX(CAST(id AS INTEGER)) FROM login WHERE id IS NOT NULL")
        max_id = cursor.fetchone()[0]
        current_id = 1 if max_id is None else max_id + 1
        for user in null_users:
            cursor.execute("UPDATE login SET id = ? WHERE username = ?", (current_id, user[0]))
            current_id += 1
        conn.commit()
    conn.close()

def sync_profiles():
    conn_login = sqlite3.connect(LOGIN_DB)
    cursor_login = conn_login.cursor()
    cursor_login.execute("SELECT username FROM login")
    logins = [row[0] for row in cursor_login.fetchall()]
    conn_login.close()

    conn_profile = sqlite3.connect(PROFILE_DB)
    cursor_profile = conn_profile.cursor()
    cursor_profile.execute("SELECT username FROM profile")
    existing_profiles = {row[0] for row in cursor_profile.fetchall()}

    for username in logins:
        if username not in existing_profiles:
            cursor_profile.execute(
                "INSERT INTO profile (username, bio, actual_badge, ProfilePicture, followers, following) VALUES (?, ?, NULL, NULL, 0, 0)",
                (username, f"Hi! my name is {username}")
            )
    conn_profile.commit()
    conn_profile.close()

def run_jobs():
    fix_login_ids()
    sync_profiles()

run_jobs()