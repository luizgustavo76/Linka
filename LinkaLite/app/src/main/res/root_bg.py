import os
import re

ROOT_DIR = "."
NEW_COLOR = "#282c37"

modified_files = 0

def update_root_tag(tag_str):
    if re.search(r'android:background="[^"]*"', tag_str):
        return re.sub(r'android:background="[^"]*"', f'android:background="{NEW_COLOR}"', tag_str)
    else:
        return re.sub(r'<\s*LinearLayout', f'<LinearLayout\n    android:background="{NEW_COLOR}"', tag_str, count=1)

for root, dirs, files in os.walk(ROOT_DIR):
    for file in files:
        if file.endswith(".xml"):
            file_path = os.path.join(root, file)
            
            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    content = f.read()

                # Busca a primeira tag do arquivo (desconsiderando decl e comentários XML)
                match = re.search(r'<\s*LinearLayout\b[^>]*>', content, flags=re.DOTALL)
                if match:
                    preceding_text = content[:match.start()]
                    clean_preceding = re.sub(r'<\?xml.*?\?>|<!--.*?-->', '', preceding_text, flags=re.DOTALL).strip()
                    
                    # Se for a tag pai (raiz) do XML
                    if clean_preceding == "":
                        old_tag = match.group(0)
                        new_tag = update_root_tag(old_tag)
                        
                        if old_tag != new_tag:
                            new_content = content[:match.start()] + new_tag + content[match.end():]
                            with open(file_path, "w", encoding="utf-8") as f:
                                f.write(new_content)
                            print(f"Modificado: {file_path}")
                            modified_files += 1

            except Exception as e:
                print(f"Erro em {file_path}: {e}")

print(f"\nFinalizado! Total de arquivos XML alterados: {modified_files}")
