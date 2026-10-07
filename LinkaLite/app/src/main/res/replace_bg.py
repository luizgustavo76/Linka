import os

# Altere para o caminho da sua pasta 'res/layout' se quiser rodar de fora,
# ou deixe '.' para rodar no diretório atual e subpastas.
ROOT_DIR = "."

TARGET = 'android:background="#121212"'
REPLACEMENT = 'android:background="#282c37"'

modified_files = 0

for root, dirs, files in os.walk(ROOT_DIR):
    for file in files:
        if file.endswith(".xml"):
            file_path = os.path.join(root, file)
            
            try:
                with open(file_path, "r", encoding="utf-8") as f:
                    content = f.read()

                if TARGET in content:
                    new_content = content.replace(TARGET, REPLACEMENT)
                    
                    with open(file_path, "w", encoding="utf-8") as f:
                        f.write(new_content)
                        
                    print(f"Modificado: {file_path}")
                    modified_files += 1

            except Exception as e:
                print(f"Erro ao ler/gravar {file_path}: {e}")

print(f"\nFinalizado! Total de arquivos XML alterados: {modified_files}")
