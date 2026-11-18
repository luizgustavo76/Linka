import os
import secrets
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, request, jsonify, g
import dotenv

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
env_path = os.path.join(BASE_DIR, "backend.env")
if os.path.exists(env_path):
    dotenv.load_dotenv(env_path)

invites_bp = Blueprint("invites_bp", __name__)

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def create_db():
    conn = get_db()
    cur = conn.cursor()
    
    cur.execute("""
        CREATE TABLE IF NOT EXISTS invites (
            id SERIAL PRIMARY KEY,
            invite_code TEXT UNIQUE,
            status TEXT DEFAULT 'NOT USED'
        )
    """)
    
    cur.execute("""
        CREATE TABLE IF NOT EXISTS invites_remaining (
            id SERIAL PRIMARY KEY,
            username TEXT UNIQUE,
            remaining INTEGER DEFAULT 10
        )
    """)
        
    cur.execute("""
        CREATE TABLE IF NOT EXISTS activies_invites (
            id SERIAL PRIMARY KEY,
            username TEXT,
            invite_code TEXT
        )
    """)
    conn.commit()
    cur.close()
    conn.close()

@invites_bp.route("/view-invites", methods=["POST"])
def view_invites():
    data = request.get_json(silent=True) or {}
    username = data.get("username")

    if username and username == getattr(g, "username", None):
        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute("SELECT username, invite_code FROM activies_invites WHERE username = %s", (username,))
            rows = cur.fetchall()
            
            result = [dict(row) for row in rows]
            
            return jsonify({
                "status": "success",
                "invites": result
            }), 200
        finally:
            cur.close()
            conn.close()

    return jsonify({"status": "forbidden"}), 403

@invites_bp.route("/create-invite", methods=["POST"])
def generate_invite():
    data = request.get_json(silent=True) or {}
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
        
        try:
            for _ in range(quantity):
                invite = secrets.token_hex(8)
                target_user = username if username else "ADMIN"
                
                cur.execute("INSERT INTO activies_invites (username, invite_code) VALUES (%s, %s)", (target_user, invite))
                cur.execute("INSERT INTO invites (invite_code, status) VALUES (%s, 'NOT USED')", (invite,))
                created_invites.append(invite)

            conn.commit()
            return jsonify({
                "status": "admin_invites_created",
                "count": len(created_invites),
                "invites": created_invites
            }), 200
        except Exception as e:
            conn.rollback()
            return jsonify({"status": "error", "message": str(e)}), 500
        finally:
            cur.close()
            conn.close()

    if username and username == getattr(g, "username", None):
        conn = get_db()
        cur = conn.cursor()
        try:
            cur.execute("SELECT remaining FROM invites_remaining WHERE username = %s", (username,))
            result = cur.fetchone()
            
            if result is None:
                cur.execute("INSERT INTO invites_remaining (username, remaining) VALUES (%s, 10)", (username,))
                conn.commit()
                remaining_count = 10
            else:
                try:
                    remaining_count = int(result["remaining"])
                except (ValueError, TypeError, KeyError):
                    remaining_count = 0

            if remaining_count > 0:
                invite = secrets.token_hex(8)
                
                cur.execute("INSERT INTO activies_invites (username, invite_code) VALUES (%s, %s)", (username, invite))
                cur.execute("INSERT INTO invites (invite_code, status) VALUES (%s, 'NOT USED')", (invite,))
                
                cur.execute("UPDATE invites_remaining SET remaining = %s WHERE username = %s", (remaining_count - 1, username))
                conn.commit()
                
                return jsonify({
                    "status": "invite created!",
                    "invite": invite,
                    "remaining": remaining_count - 1
                }), 200
            else:
                return jsonify({"status": "You've run out of invitations; wait until next month."}), 400
        except Exception as e:
            conn.rollback()
            return jsonify({"status": "error", "message": str(e)}), 500
        finally:
            cur.close()
            conn.close()

    return jsonify({"status": "forbidden"}), 403