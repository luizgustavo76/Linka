from flask import Flask, Blueprint, request, jsonify
import sqlite3
import os
slug_bp = Blueprint("slug", __name__)
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
if not os.path.exists(db_dir):
    os.makedirs(db_dir)
def get_db():
    conn = sqlite3.connect(db_dir + "slug.db")
    return conn
def create_db():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""CREATE IF NOT EXISTS slug (
                name_server TEXT,
                url TEXT,
                owner TEXT,
                profile_linka TEXT,
                email TEXT,
                id TEXT)""")
    conn.commit()
    conn.close()
create_db()
@slug_bp.route("/consult", methods=["post"])
def consult():
    data = request.get_json()
    name_server = data.get("name_server")
    if name_server == "":
        return jsonify({"status":"name_server is empty"}),400
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT * FROM slug WHERE name_server = ?",(name_server,))
    result = cur.fetchall()
    row = {
        "name_server":result[0],
        "url":result[1],
        "owner":result[2],
        "id":result[3]
    }
    if result == None:
        return jsonify({"status":"server not exists"}),400
    return jsonify(row)
@slug_bp.route("/register-instance", methods=["POST"])
def register_instance():
    data = request.get_json()
    name_server = data.get("name_server")
    url = data.get("url")
    owner = data.get("owner")
    profile_linka = data.get("profile_linka")
    email = data.get("email")
    conn = get_db()
    cur = conn.cursor()
    cur.execute("INSERT INTO slug (name_server, url, owner, profile_linka) VALUES(?, ?, ?, ?)",(name_server, url, owner, profile_linka))
    conn.commit()
    conn.close()
    return jsonify({"status":"instance register is sucessful"}),200