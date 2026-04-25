from flask import Flask, Blueprint, jsonify, request
import os
import sqlite3
community_bp = Blueprint("community", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = (base_dir + "/DB")
community_file = (db_dir + "/community.db")
def get_db():
    conn = sqlite3.connect(community_file)
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("CREATE TABLE IF NOT EXISTS community(name TEXT, owner TEXT UNIQUE, members INTEGER, bio TEXT)")
    cur.execute("CREATE TABLE IF NOT EXISTS posts(post_id INTEGER PRIMARY KEY AUTOINCREMENT,username TEXT UNIQUE, post_text TEXT, datetime TEXT)")
    cur.execute("CREATE TABLE IF NOT EXISTS stars(post_id INTEGER UNIQUE, username TEXT)")
    cur.execute("CREATE TABLE IF NOT EXISTS comments(comment_id INTEGER PRIMARY KEY AUTOINCREMENT, username TEXT, comment_text TEXT, datetime TEXT)")
    cur.execute("CREATE TABLE IF NOT EXISTS members(username TEXT UNIQUE)")
    conn.commit()
    conn.close()
create_db()
