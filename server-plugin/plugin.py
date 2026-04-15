from flask import Flask, request, jsonify
import os
import sqlite3
app = Flask(__name__)
app.route("/meta/api")
def api():
    with open("server.json", "r") as f:
        print(f)
with open("server.json", "r") as f:
        print(f.read())