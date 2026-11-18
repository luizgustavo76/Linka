#!/bin/bash

# Diretório base dos layouts XML
LAYOUT_DIR="app/src/main/res/layout"

if [ ! -d "$LAYOUT_DIR" ]; then
    LAYOUT_DIR="res/layout" # Fallback para estrutura Eclipse/Ant legada
fi

echo "🔍 Substituindo cores hardcoded por ?attr/... em $LAYOUT_DIR"

# 1. Troca backgrounds brancos (#ffffff, #FFF, etc.) pelo fundo do tema
find "$LAYOUT_DIR" -type f -name "*.xml" -exec sed -i \
    -e 's/android:background="#ffffff"/android:background="?attr\/themeBackgroundColor"/gI' \
    -e 's/android:background="#fff"/android:background="?attr\/themeBackgroundColor"/gI' \
    -e 's/android:background="#FFFFFF"/android:background="?attr\/themeBackgroundColor"/gI' \
    {} +

# 2. Troca cores de texto pretas/escuras (#000000, #000, #333333) pelo texto do tema
find "$LAYOUT_DIR" -type f -name "*.xml" -exec sed -i \
    -e 's/android:textColor="#000000"/android:textColor="?attr\/themeTextColor"/gI' \
    -e 's/android:textColor="#000"/android:textColor="?attr\/themeTextColor"/gI' \
    -e 's/android:textColor="#333333"/android:textColor="?attr\/themeTextColor"/gI' \
    -e 's/android:textColor="#222222"/android:textColor="?attr\/themeTextColor"/gI' \
    {} +

# 3. Troca backgrounds de headers/barras (#E0E0E0, #EFEFEF) pelo header do tema
find "$LAYOUT_DIR" -type f -name "*.xml" -exec sed -i \
    -e 's/android:background="#E0E0E0"/android:background="?attr\/themeHeaderBg"/gI' \
    -e 's/android:background="#EFEFEF"/android:background="?attr\/themeHeaderBg"/gI' \
    {} +

echo "✅ Substituição concluída com sucesso!"
