from flask import Flask, Blueprint
from slug import slug_bp
from linkaGate import linkaGate_bp
app = Flask(__name__)
app.register_blueprint(slug_bp)
app.register_blueprint(linkaGate_bp)
if __name__ == "__main__":
    app.run(port=8000)