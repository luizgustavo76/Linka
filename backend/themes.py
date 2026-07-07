from flask import Flask, jsonify, request, Blueprint, make_response
import themeInterpreter

theme_bp = Blueprint("theme_bp", __name__)

@theme_bp.route("/convert-theme", methods=["POST"])
def convert_theme():
    data = request.get_json()
    input_data = data.get("input") 
    output = data.get("output")
    qss_compiled = themeInterpreter.convertTheme(input_data, output)
    response = make_response(qss_compiled)
    response.headers['Content-Type'] = 'text/plain; charset=utf-8'
    return response