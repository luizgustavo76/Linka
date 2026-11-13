#!/usr/bin/env python3
import sys
import os
from PIL import Image

VALID_EXTS = ('.png', '.jpg', '.jpeg', '.webp')

def generate_android_assets():
    if len(sys.argv) < 2:
        print("Uso: ./resizer.py <imagem1.png> [imagem2.png ...]")
        print("Exemplo: ./resizer.py *.png")
        sys.exit(1)

    scales = {
        'drawable-ldpi': 0.75 / 2.0,   # 0.375x da imagem original
        'drawable-mdpi': 1.0 / 2.0,    # 0.5x
        'drawable-hdpi': 1.5 / 2.0,    # 0.75x
        'drawable-xhdpi': 1.0,         # 1.0x (tamanho original)
        'drawable-xxhdpi': 3.0 / 2.0,  # 1.5x
        'drawable-xxxhdpi': 4.0 / 2.0  # 2.0x
    }

    for image_path in sys.argv[1:]:
        if not os.path.isfile(image_path) or not image_path.lower().endswith(VALID_EXTS):
            continue

        # .convert("RGBA") evita o crash do LANCZOS em PNGs com paleta indexada (modo P)
        img = Image.open(image_path).convert("RGBA")
        orig_w, orig_h = img.size
        filename = os.path.basename(image_path).lower().replace('-', '_')

        print(f"🖼  Processando: {filename} ({orig_w}x{orig_h}px)")

        for folder, scale in scales.items():
            new_w = max(1, int(orig_w * scale))
            new_h = max(1, int(orig_h * scale))

            os.makedirs(folder, exist_ok=True)

            resized_img = img.resize((new_w, new_h), Image.Resampling.LANCZOS)
            output_path = os.path.join(folder, filename)
            # Salva sempre como PNG válido (mesmo se a entrada era .jpeg como o reddit.jpeg)
            out_name = os.path.splitext(filename)[0] + ".png"
            resized_img.save(os.path.join(folder, out_name), "PNG")
            print(f"  [✓] {folder}/{out_name} -> {new_w}x{new_h}px")

    print("\n🚀 Pronto!")

if __name__ == "__main__":
    generate_android_assets()
