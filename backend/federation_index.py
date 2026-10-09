from flask import Flask, request, Blueprint, jsonify, g
import sqlite3
import os
from dotenv import load_dotenv
load_dotenv("backend.env")
ADMIN_MASTER_KEY = os.getenv("ADMIN_MASTER_KEY")
federation_index_bp = Blueprint("federation_index_bp", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = (base_dir + "/DB")
def get_db():
    conn = sqlite3.connect(db_dir + "/federation_index.db")
    return conn
def create_table():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""CREATE TABLE IF NOT EXISTS federation_index(
                name TEXT,
                url TEXT,
                description TEXT,
                cover_image TEXT,
                theme TEXT)""")
    cur.execute("""CREATE TABLE IF NOT EXISTS themes_timeline(
                name_theme TEXT UNIQUE,
                description TEXT)""")
    conn.commit()
    conn.close()
create_table()
@federation_index_bp.route("/add-theme", methods=["post"])
def add_theme():
    data = request.get_json()
    username = data.get("username")
    name = data.get("name")
    description = data.get("description")
    if username == g.username:
        admin_master_key = data.get("admin_master_key")
        if admin_master_key == ADMIN_MASTER_KEY:
            conn = get_db()
            cur = conn.cursor()
            cur.execute("INSERT INTO themes (name, description) VALUES(?, ?)",(name, description))
            conn.commit()
            conn.close()
            return jsonify({"status":"theme created"}),200
        else:
            return jsonify({"status":"forget admin key.... or you are a invasor.. nice to meet you"}),400
    else:
        return jsonify({"status":"forbidden"}),403
@federation_index_bp.route("/view-index")
def view_index():
    conn = get_db()
    formatted_index = []
    cur = conn.cursor()
    cur.execute("SELECT * FROM federation_index")
    result = cur.fetchall()
    for i in result:
        formatted_index.append({
            "name":i[0],
            "url":i[1],
            "description":i[2],
            "cover_image":i[3],
            "theme":i[4]
        })
    return jsonify(formatted_index)
@federation_index_bp.route("/view-index-themes")
def view_index_themes():
    conn = get_db()
    formatted_index = []
    cur = conn.cursor()
    cur.execute("SELECT * FROM themes_timeline")
    result = cur.fetchall()
    for i in result:
        formatted_index.append({
            "name_theme":i[0],
            "description":i[1]
        })
    return jsonify(formatted_index)
@federation_index_bp.route("/register-federation",methods=["POST"])
def register_federation():
    data = request.get_json()
    name = data.get("name")
    url = data.get("url")
    description = data.get("description")
    cover_image = data.get("cover_image")
    theme = data.get("theme")
    conn = get_db()
    cur = conn.cursor()
    cur.execute(
        "INSERT INTO federation_index (name, url, description, cover_image, theme) VALUES (?, ?, ?, ?, ?)",
        (name, url, description, cover_image, theme)
    )
    conn.commit()
    conn.close()
    return jsonify({"status":"the federations has registred with sucess"}),200