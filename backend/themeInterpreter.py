import json
import os

def convertTheme(input_theme_data, output_format):
    if output_format.lower() == "qss":
        base_path = "themeParser/themeQSS.json" 
        if not os.path.exists(base_path):
            raise FileNotFoundError(f"Arquivo de base nao encontrado em: {base_path}")
            
        with open(base_path, 'r', encoding='utf-8') as f:
            base_data = json.load(f)
            
        theme_data = input_theme_data
        if not isinstance(theme_data, dict):
            raise ValueError("O input do tema precisa ser um objeto JSON valido (dict).")
            
        widgets_map = base_data["widgets"]
        tokens = base_data["colors"]
        qss_lines = []

        def replace_tokens(val_str):
            """Substitui apelidos de cores pelos hexadecimais correspondentes"""
            if val_str in tokens:
                return tokens[val_str]
            for token_name, token_value in tokens.items():
                if token_name in val_str:
                    val_str = val_str.replace(token_name, token_value)
            return val_str
        for abstract_widget, blocks in theme_data.items():
            qt_selector = widgets_map.get(abstract_widget, abstract_widget)
            
            if "base" in blocks:
                qss_lines.append(f"{qt_selector} {{")
                for prop, value in blocks["base"].items():
                    actual_value = replace_tokens(value)
                    qss_lines.append(f"    {prop}: {actual_value};")
                qss_lines.append("}\n")
                
            if "states" in blocks:
                for state, props in blocks["states"].items():
                    qss_lines.append(f"{qt_selector}:{state} {{")
                    for prop, value in props.items():
                        actual_value = replace_tokens(value)
                        qss_lines.append(f"    {prop}: {actual_value};")
                    qss_lines.append("}\n")
                    
            if "subcontrols" in blocks:
                for sub, props in blocks["subcontrols"].items():
                    if ":" in sub:
                        sub_name, sub_state = sub.split(":", 1)
                        full_selector = f"{qt_selector}::{sub_name}:{sub_state}"
                    else:
                        full_selector = f"{qt_selector}::{sub}"
                        
                    qss_lines.append(f"{full_selector} {{")
                    for prop, value in props.items():
                        actual_value = replace_tokens(value)
                        qss_lines.append(f"    {prop}: {actual_value};")
                    qss_lines.append("}\n")
                    
        return "\n".join(qss_lines)
        
    else:
        raise ValueError(f"Formato de saida '{output_format}' nao suportado.")