from flask import Blueprint, request, jsonify, g
import sqlite3
import os
import secrets
invites_bp = Blueprint("invites_bp", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = (base_dir + "/DB")
invites_file = (db_dir + "/invites.db")
def get_db():
    conn = sqlite3.connect(invites_file)
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""CREATE TABLE IF NOT EXISTS invites(
        invite_code TEXT UNIQUE,
        status TEXT DEFAULT 'NOT USED')""")
    cur.execute("""CREATE TABLE IF NOT EXISTS invites_remaining(
        username TEXT,
        remaining TEXT DEFAULT '10')""")
    cur.execute("""CREATE TABLE IF NOT EXISTS activies_invites(
        username TEXT,
        invite_code TEXT)""")
    conn.commit()
    conn.close()
create_db()

@invites_bp.route("/generate-invite", methods=["POST"])
def generate_invite():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        conn = get_db()
        cur = conn.cursor()
        cur.execute("SELECT username FROM invites_remaining WHERE username = ? AND remaining > 0", (username,))
        result = cur.fetchone()
        if result:
            invite = secrets.token_bytes(8)
            cur.execute("INSERT INTO activies_invites (username, invite_code) VALUES(?, ?)",(username, invite))
            conn.commit()
            cur.execute("UPDATE invites_remaining SET remaining = remaining - 1 WHERE username = ?", (username,))
            conn.commit()
            return jsonify({"status":"invite created!", "invite":invite}),200
        else:
            return jsonify({"status":"You've run out of invitations; wait until next month."}),400
    else:
        return jsonify({"status":"forbidden"}),403