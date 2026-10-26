from flask import Flask, jsonify, request, g
import sqlite3
import os
base_dir = os.path.dirname(os.path.abspath(__file__))
db_dir = os.path.join(base_dir, "DB")
def get_db():
    conn = sqlite3.connect(db_dir + "/bookmarks.db")
    return conn
