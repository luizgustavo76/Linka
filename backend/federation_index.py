from flask import Flask, request, Blueprint, jsonify
import sqlite3
import os
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
                description TEXT)""")
    conn.commit()
    conn.close()
create_table()
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
            "description":i[2]
        })
    return jsonify(formatted_index)
@federation_index_bp.route("/register-federation",methods=["POST"])
def register_federation():
    data = request.get_json()
    name = data.get("name")
    url = data.get("url")
    description = data.get("description")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("INSERT INTO (name, url, description) FROM federations_index VALUES (?, ?, ?)",(name, url, description))
    conn.commit()
    conn.close()
    return jsonify({"status":"the federations has registred with sucess"}),200