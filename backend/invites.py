from flask import Blueprint, request, jsonify, g
import sqlite3
import os
import secrets

invites_bp = Blueprint("invites_bp", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")

if not os.path.exists(db_dir):
    os.makedirs(db_dir)

invites_file = os.path.join(db_dir, "invites.db")

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
        username TEXT UNIQUE,
        remaining INTEGER DEFAULT 10)""")
        
    cur.execute("""CREATE TABLE IF NOT EXISTS activies_invites(
        username TEXT,
        invite_code TEXT)""")
    conn.commit()
    conn.close()

create_db()

@invites_bp.route("/view-invites", methods=["POST"])
def view_invites():
    data = request.get_json() or {}
    username = data.get("username")

    if username and username == getattr(g, 'username', None):
        conn = get_db()
        conn.row_factory = sqlite3.Row 
        cur = conn.cursor()
        
        cur.execute("SELECT * FROM activies_invites WHERE username = ?", (username,))
        rows = cur.fetchall()
        
        result = [dict(row) for row in rows]
        
        cur.close()
        conn.close()

        return jsonify({
            "status": "success",
            "invites": result
        }), 200

    return jsonify({"status": "forbidden"}), 403

@invites_bp.route("/create-invite", methods=["POST"])
def generate_invite():
    data = request.get_json() or {}
    username = data.get("username")
    admin_key = request.headers.get("X-Admin-Key") or data.get("admin_key")
    MASTER_KEY = os.getenv("ADMIN_MASTER_KEY")
    if MASTER_KEY and admin_key == MASTER_KEY:
        try:
            quantity = int(data.get("quantity", 1))
            if quantity < 1:
                quantity = 1
        except (ValueError, TypeError):
            quantity = 1

        conn = get_db()
        cur = conn.cursor()
        
        created_invites = []
        
        for _ in range(quantity):
            invite = secrets.token_hex(8)
            if username:
                cur.execute("INSERT INTO activies_invites (username, invite_code) VALUES (?, ?)", (username, invite))
            else:
                cur.execute("INSERT INTO activies_invites (username, invite_code) VALUES (?, ?)", ("ADMIN", invite))
                
            cur.execute("INSERT INTO invites (invite_code, status) VALUES (?, 'NOT USED')", (invite,))
            created_invites.append(invite)

        conn.commit()
        conn.close()

        return jsonify({
            "status": "admin_invites_created",
            "count": len(created_invites),
            "invites": created_invites
        }), 200

    if username and username == getattr(g, 'username', None):
        conn = get_db()
        cur = conn.cursor()
        
        cur.execute("SELECT remaining FROM invites_remaining WHERE username = ?", (username,))
        result = cur.fetchone()
        
        if result is None:
            cur.execute("INSERT INTO invites_remaining (username, remaining) VALUES (?, 10)", (username,))
            conn.commit()
            remaining_count = 10
        else:
            try:
                remaining_count = int(result[0])
            except (ValueError, TypeError):
                remaining_count = 0

        if remaining_count > 0:
            invite = secrets.token_hex(8)
            
            cur.execute("INSERT INTO activies_invites (username, invite_code) VALUES (?, ?)", (username, invite))
            cur.execute("INSERT INTO invites (invite_code, status) VALUES (?, 'NOT USED')", (invite,))
            
            cur.execute("UPDATE invites_remaining SET remaining = ? WHERE username = ?", (remaining_count - 1, username))
            conn.commit()
            conn.close()
            
            return jsonify({
                "status": "invite created!",
                "invite": invite,
                "remaining": remaining_count - 1
            }), 200
        else:
            conn.close()
            return jsonify({"status": "You've run out of invitations; wait until next month."}), 400
            
    return jsonify({"status": "forbidden"}), 403