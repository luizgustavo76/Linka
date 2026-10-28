from flask import Flask, jsonfiy, request, Blueprint, g
import os
import sqlite3
communit_bp = Blueprint("community_bp", __name__)
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
    cur.execute("""CREATE TABLE IF NOT EXISTS community_index(
                name TEXT,
                description TEXT,
                banner_image TEXT,
                icon_image TEXT,
                owner TEXT)""")
    cur.execute("""
            CREATE TABLE IF NOT EXISTS posts(
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
