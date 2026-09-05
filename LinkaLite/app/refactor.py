import os
import re

def process_java_file(filepath):
    with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
        content = f.read()

    original_content = content

    # 1. Identifica a classe principal do arquivo
    class_match = re.search(r'public\s+(?:final\s+)?class\s+(\w+)', content)
    if not class_match:
        return False, 0
    
    class_name = class_match.group(1)

    # 2. Injeta declaração de 'private String url;' e/ou 'private String username;' se ausentes
    fields_to_inject = []
    if re.search(r'\burl\s*=', content) and not re.search(r'\b(String|URL)\s+url\b', content):
        fields_to_inject.append("    private String url;")
    if re.search(r'\busername\s*=', content) and not re.search(r'\bString\s+username\b', content):
        fields_to_inject.append("    private String username;")

    if fields_to_inject:
        class_header_end = class_match.end()
        # Encontra a primeira chave '{' da classe
        brace_pos = content.find('{', class_header_end)
        if brace_pos != -1:
            injection = "\n" + "\n".join(fields_to_inject)
            content = content[:brace_pos + 1] + injection + content[brace_pos + 1:]

    # 3. Corrige instâncias de AppConfig(this) incorretas em classes internas, adapters e contextos estáticos
    lines = content.splitlines()
    new_lines = []
    in_static_method = False

    for line in lines:
        if re.search(r'\bstatic\b', line) and ('void' in line or 'class' in line or 'boolean' in line or 'String' in line):
            in_static_method = True

        if 'new AppConfig(this)' in line:
            if in_static_method:
                line = line.replace('new AppConfig(this)', 'new AppConfig(null)')
            elif 'Adapter' in class_name or 'Activity' in class_name or 'View' in class_name:
                line = line.replace('new AppConfig(this)', f'new AppConfig({class_name}.this)')

        # Corrige chamadas de métodos inexistentes
        if 'appConfig.getUrlAsObject()' in line:
            line = line.replace('appConfig.getUrlAsObject()', 'appConfig.getUrl()')

        new_lines.append(line)

    content = "\n".join(new_lines)

    # 4. Salva as alterações caso o arquivo tenha mudado
    if content != original_content:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True, len(fields_to_inject)

    return False, 0

def run():
    total_scanned = 0
    modified_files = 0
    total_fields_added = 0

    print("Iniciando a varredura nos arquivos Java...")

    for root, _, files in os.walk("."):
        for file in files:
            if file.endswith(".java") and file != "AppConfig.java":
                filepath = os.path.join(root, file)
                total_scanned += 1

                was_modified, fields_added = process_java_file(filepath)
                if was_modified:
                    modified_files += 1
                    total_fields_added += fields_added
                    print(f"[MODIFICADO] {file}")

    print("\n" + "=" * 55)
    print("              RELATÓRIO DE ALTERAÇÕES              ")
    print("=" * 55)
    print(f"• Arquivos Java verificados:      {total_scanned}")
    print(f"• Arquivos alterados com sucesso: {modified_files}")
    print(f"• Variáveis 'private' adicionadas: {total_fields_added}")
    print("=" * 55 + "\n")

if __name__ == "__main__":
    run()