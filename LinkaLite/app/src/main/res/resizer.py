#!/usr/bin/env python3
import sys
import os
from PIL import Image

def generate_android_assets():
    if len(sys.argv) < 2:
        print("Uso: python3 resizer.py <imagem>")
        print("Exemplo: python3 resizer.py ic_home.png")
        sys.exit(1)

    image_path = sys.argv[1]

    if not os.path.exists(image_path):
        print(f"Erro: Arquivo '{image_path}' não encontrado.")
        sys.exit(1)

    img = Image.open(image_path)
    orig_w, orig_h = img.size

    # Consideramos a imagem de entrada como XHDPI (2.0x) por padrão
    # Multiplicadores ajustados em relação ao XHDPI:
    scales = {
        'drawable-ldpi': 0.75 / 2.0,   # 0.375x da imagem original
        'drawable-mdpi': 1.0 / 2.0,    # 0.5x
        'drawable-hdpi': 1.5 / 2.0,    # 0.75x
        'drawable-xhdpi': 1.0,         # 1.0x (tamanho original)
        'drawable-xxhdpi': 3.0 / 2.0,  # 1.5x
        'drawable-xxxhdpi': 4.0 / 2.0  # 2.0x
    }

    filename = os.path.basename(image_path).lower().replace('-', '_')

    print(f"🖼️  Processando: {filename} ({orig_w}x{orig_h}px)")

    for folder, scale in scales.items():
        new_w = max(1, int(orig_w * scale))
        new_h = max(1, int(orig_h * scale))
        
        os.makedirs(folder, exist_ok=True)
        
        resized_img = img.resize((new_w, new_h), Image.Resampling.LANCZOS)
        output_path = os.path.join(folder, filename)
        resized_img.save(output_path)
        print(f"  [✓] {folder}/{filename} -> {new_w}x{new_h}px")

    print("\n🚀 Pronto! Pastas criadas sem ter de digitar nenhum número.")

if __name__ == "__main__":
    generate_android_assets()
