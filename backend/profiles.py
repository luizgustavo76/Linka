from flask import Flask, request, jsonify, Blueprint, send_from_directory, g
import os
import uuid
import sqlite3
#pasta atual
base_dir = os.path.dirname(os.path.abspath(__file__))
#pasta dos bancos de dados
db_dir = (base_dir + "/DB")
#arquivo de banco de dado de post
profile_dir = (db_dir + "/profile.db")
profile_bp = Blueprint("profile", __name__)
def get_db():
    conn = sqlite3.connect(profile_dir)
    cursor = conn.cursor()
    conn.execute("PRAGMA journal_mode=WAL;")
    conn.execute("PRAGMA synchronous=NORMAL;")
    conn.execute("PRAGMA cache_size=-10000;")
    return conn
def create_table():
    conn = get_db()
    cur = conn.cursor()
    cur.execute("""
        CREATE TABLE IF NOT EXISTS profile (
            username TEXT PRIMARY KEY,
            bio TEXT,
            ProfilePicture TEXT,
            followers INTEGER,
            following INTEGER
        )
        """)
    conn.commit()
    conn.close()
create_table()
@profile_bp.route("/edit", methods=["POST"])
def edit():
    data = request.get_json()
    edit_mode = data.get("edit-mode")
    content = data.get("content")
    username = data.get("username")
    if username == g.username:
        if edit_mode == "bio":
            conn = get_db()
            cur = conn.cursor()
            cur.execute("UPDATE profile SET bio = ? WHERE username = ?", (content, username))
            conn.commit()
            conn.close()
            return jsonify({"status":"edit is sucessful"}),200
    else:
        return jsonify({"status":"forbidden"}),403
@profile_bp.route("/create_profile",methods=["post"])
def create():
    data = request.get_json()
    username = data.get("username")
    bio_default = f"Hi! my name is {username} and im new in the Linka, let be friends?"
    conn = get_db()
    cur = conn.cursor()
    cur.execute("INSERT INTO profile (username, bio) VALUES (?,?)", (username, bio_default))
    conn.commit()
    conn.close()
    return jsonify({"status":"Ok"})

        
@profile_bp.route("/view_profile/<username>")
def view(username):
    conn = get_db()
    cur = conn.cursor()
    cur.execute("SELECT * FROM profile WHERE username = ?",(username,))
    response = cur.fetchone()
    conn.close()
    row = {
        "username":response[0],
        "bio":response[1]
    }
    return jsonify(row), 200
UPLOAD_FOLDER = "../profile-pictures"
os.makedirs(UPLOAD_FOLDER, exist_ok=True)

@profile_bp.route("/upload_profile_pic", methods=["POST"])
def upload_profile_pic():

    username = request.form.get("username")
    if username == g.username:
        if not username:
            return jsonify({"error": "Username is missing"}), 400

        if "file" not in request.files:
            return jsonify({"error": "nothing image is sent"}), 400

        file = request.files["file"]

        if file.filename == "":
            return jsonify({"error": "Archive empty"}), 400

        ext = file.filename.split(".")[-1].lower()

        if ext not in ["png", "jpg", "jpeg", "webp"]:
            return jsonify({"error": "Formato inválido"}), 400

        filename = f"{uuid.uuid4()}.{ext}"
        file_path = os.path.join(UPLOAD_FOLDER, filename)

        file.save(file_path)

        conn = get_db()
        cur = conn.cursor()

        cur.execute(
            "UPDATE profile SET ProfilePicture = ? WHERE username = ?",
            (filename, username)
        )

        conn.commit()
        conn.close()
        return jsonify({"success": True, "filename": filename}), 200
    else:
        return jsonify({"status":"forbidden"}),403


@profile_bp.route("/profile_pics/<filename>")
def get_profile_pic(filename):
    return send_from_directory(UPLOAD_FOLDER, filename)
    
