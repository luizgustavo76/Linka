import os
import re

def fix_xml_content(content):
    content = re.sub(r'(\d+)dp\b', r'\1dip', content)
    content = re.sub(r'\s*android:contentDescription="[^"]*"', '', content)
    content = re.sub(r'android:layout_marginStart=', 'android:layout_marginLeft=', content)
    content = re.sub(r'android:layout_marginEnd=', 'android:layout_marginRight=', content)
    return content

def fix_java_content(content):
    # Fix 1: !expr.isEmpty() -> expr.length() > 0
    content = re.sub(r'!\s*([a-zA-Z0-9_().]+)\.isEmpty\(\)', r'\1.length() > 0', content)
    
    # Fix 2: expr.isEmpty() -> expr.length() == 0 (Apenas se não houver '!' antes)
    content = re.sub(r'(?<![!\w])([a-zA-Z0-9_().]+)\.isEmpty\(\)', r'\1.length() == 0', content)
    
    # Fix 3: Build.VERSION.SDK_INT -> Integer.parseInt(Build.VERSION.SDK)
    content = re.sub(r'Build\.VERSION\.SDK_INT', r'Integer.parseInt(Build.VERSION.SDK)', content)
    
    return content

def process_files():
    target_dir = os.path.join(".", "app", "src", "main")
    fixed_files = 0

    for root, _, files in os.walk(target_dir):
        for file in files:
            filepath = os.path.join(root, file)
            
            if file.endswith('.xml'):
                with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
                    old_content = f.read()
                new_content = fix_xml_content(old_content)
                if old_content != new_content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"[FIXED XML] {filepath}")
                    fixed_files += 1

            elif file.endswith('.java'):
                with open(filepath, 'r', encoding='utf-8', errors='ignore') as f:
                    old_content = f.read()
                new_content = fix_java_content(old_content)
                if old_content != new_content:
                    with open(filepath, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"[FIXED JAVA] {filepath}")
                    fixed_files += 1

    print(f"\nConcluído! {fixed_files} arquivos foram corrigidos.")

if __name__ == "__main__":
    process_files()
