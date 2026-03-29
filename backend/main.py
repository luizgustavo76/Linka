from flask import Flask
from post import post_bp
from chat import chat_bp
from github_auth import github_bp

app = Flask(__name__)

app.register_blueprint(post_bp)
app.register_blueprint(chat_bp)
app.register_blueprint(github_bp)

app.run(debug=True, port=5000)