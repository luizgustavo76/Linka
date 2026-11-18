import os
import re

# Regras de checagem para arquivos XML
XML_RULES = [
    (r'android:contentDescription=', "Atributo 'contentDescription' só existe a partir do Android 1.6 (API 4). Remova no 1.5."),
    (r'android:onClick=', "Atributo 'onClick' no XML só foi adicionado no Android 1.6 (API 4). Use setOnClickListener no Java."),
    (r'android:(padding|layout_margin)(Start|End)=', "Padding/Margin Start/End só existem na API 17 (Android 4.2). Use Left/Right."),
    (r'android:(alpha|elevation|translationX|translationY)=', "Propriedades de animação/elevação só existem em APIs mais novas (API 11+ / 21+)."),
    (r'\b\d+dp\b', "Unidade 'dp' pode falhar no Android 1.5. Substitua por 'dip' (ex: 16dip)."),
    (r'<(androidx\.|android\.support\.)', "Bibliotecas AndroidX ou Support Library não funcionam nativamente no Android 1.5."),
    (r'<(fragment|GridLayout|Space|Toolbar)', "Tags de layout modernas (Fragment, GridLayout, Space, Toolbar) não existem no Android 1.5.")
]

# Regras de checagem para arquivos JAVA
JAVA_RULES = [
    (r'\.isEmpty\(\)', "MÉTODO PROIBIDO: 'String.isEmpty()' só existe a partir do Android 2.3 (API 9). Use '.length() == 0'."),
    (r'Build\.VERSION\.SDK_INT', "'Build.VERSION.SDK_INT' só foi adicionado no Android 1.6 (API 4). No 1.5, use 'Integer.parseInt(Build.VERSION.SDK)'."),
    (r'Notification\.Builder', "'Notification.Builder' só existe a partir do Android 3.0 (API 11). Use 'new Notification(...)' clássico."),
    (r'Objects\.equals\(', "'Objects.equals' só existe a partir do Android 4.4 (API 19)."),
    (r'import\s+(androidx\.|android\.support\.)', "Import de biblioteca Support/AndroidX detectado. Incompatível com Android 1.5."),
    (r'\.setAlpha\(|\.setTranslation', "Métodos de visualização moderna (setAlpha, setTranslationX, etc.) exigem API 11+."),
    (r'->', "Sintaxe de Lambda (Java 8) não é suportada na Dalvik nativa do Android 1.5.")
]

def scan_file(filepath):
    issues = []
    is_xml = filepath.endswith('.xml')
    is_java = filepath.endswith('.java')
    
    rules = XML_RULES if is_xml else (JAVA_RULES if is_java else [])
    if not rules:
        return issues

    try:
        with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
            lines = f.readlines()
            for line_num, line in enumerate(lines, 1):
                for pattern, message in rules:
                    if re.search(pattern, line):
                        issues.append((line_num, message, line.strip()))
    except Exception as e:
        print(f"Erro ao ler {filepath}: {e}")

    return issues

def main():
    print("=" * 65)
    print("  INSPECTING PROJECT FOR ANDROID 1.5 (API 3) COMPATIBILITY")
    print("=" * 65)
    
    root_dir = "."
    total_issues = 0

    for dirpath, _, filenames in os.walk(root_dir):
        for filename in filenames:
            if filename.endswith('.java') or filename.endswith('.xml'):
                filepath = os.path.join(dirpath, filename)
                issues = scan_file(filepath)
                if issues:
                    print(f"\n[!] {filepath}")
                    for line_num, msg, code in issues:
                        total_issues += 1
                        print(f"   Line {line_num}: {msg}")
                        print(f"      > {code}")

    print("\n" + "=" * 65)
    if total_issues == 0:
        print(" SUCCESS: Nenhum problema óbvio de compatibilidade com o 1.5 encontrado!")
    else:
        print(f" ATENÇÃO: Encontradas {total_issues} incompatibilidades potenciais.")
    print("=" * 65)

if __name__ == "__main__":
    main()
