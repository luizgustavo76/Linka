import requests
import os
import json
from flask import Flask, jsonify
data = {}
def connect(json):
    if not os.path.exists():
        print(f"LINKA ERROR:the json doesn`t exists {json}")
    else:
        with open(json, "r") as json_data:
            data = json.load(json_data)

