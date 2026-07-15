from flask import Flask, Blueprint
from posts import post_bp
app = Flask(__name__)
app.register_blueprint(post_bp)
if __name__ == "__main__":
    app.run(port=8000)