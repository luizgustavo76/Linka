from flask import Flask, jsonfiy, request, Blueprint, g
import os
import sqlite3
community_bp = Blueprint("community_bp", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = (base_dir + "/DB")
def get_db():
    conn = sqlite3.connect(db_dir + "/community.db")
    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA synchronous=NORMAL;")
    conn.execute("PRAGMA cache_size=-10000;")
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""CREATE TABLE IF NOT EXISTS community_index (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            name TEXT NOT NULL UNIQUE,
            description TEXT,
            banner_image TEXT,
            icon_image TEXT,
            owner TEXT NOT NULL,
            members INTEGER DEFAULT 1,
            created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
        )""")
    cur.execute("""
            CREATE TABLE IF NOT EXISTS posts(
                community_name TEXT,
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                username TEXT,
                media_type TEXT,
                width TEXT,
                heigth TEXT,
                text_post TEXT,
                datetime TEXT
            )
        """)
    cur.execute("""CREATE TABLE IF NOT EXISTS comments(
                community_name TEXT,
                username TEXT,
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                media_type TEXT,
                width TEXT,
                heigth TEXT,
                text_comment TEXT,
                datetime TEXT,
                comments_level INTEGER DEFAULT 0,
                parent_comment INTEGER)""")
    conn.commit()
    conn.close()
create_db()
@community_bp.route("/new-community", methods=["POST"])
def new_community():
    data = request.get_json()
    username = data.get("username")
    if username == g.username:
        community_name = data.get("community_name")
        description = data.get("description")
        banner_image = data.get("banner_image")
        icon_image = data.get("icon_image")
        if None in (community_name, description):
            return jsonfiy({"status":"missing data"}),400
        conn = get_db()
        cur = conn.cursor()
        cur.execute("INSERT INTO commmunity_index(name, description, banner_image, icon_image, owner) VALUES(?,?,?,?,?)",(community_name, description, banner_image, icon_image, username))
        conn.commit()
        conn.close()
        return jsonfiy({"status":"community created"}),200
    else:
        return jsonfiy({"status":"forbidden"}),403

