

from flask import Flask
from flask_cors import CORS 
from post import post_bp
from chat import chat_bp
from friends import friends_bp
from login import login_bp
from profiles import profile_bp
from search import search_bp
app = Flask(__name__)


CORS(app, resources={r"/*": {"origins": "*"}})
app.register_blueprint(search_bp)
app.register_blueprint(profile_bp)
app.register_blueprint(login_bp)
app.register_blueprint(post_bp)
app.register_blueprint(chat_bp)
app.register_blueprint(friends_bp)


if __name__ == "__main__":
    app.run(debug=True, port=5000)