import os
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint, request, jsonify, g
feeds_slots_bp = Blueprint("feeds_slots", __name__)

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
    CREATE TABLE IF NOT EXISTS feeds_slots(
        username TEXT,
        id SERIAL PRIMARY KEY,
        feed_url TEXT,
        feed_name TEXT,
        slot INTEGER)""")
    cur.commit()
    conn.close()
    cur.close()
create_db()
@feeds_slots_bp.route("/new-slot",methods=["POST"])
def new_slot():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        feed_url = data.get("feed_url")
        feed_name = data.get("feed_name")
        if None in (feed_url, feed_name):
            return jsonify({"status":"feed_url or feed_name are missing"}),400
        try:
            conn = get_db()
            cur = conn.cursor()
            cur.execute("")