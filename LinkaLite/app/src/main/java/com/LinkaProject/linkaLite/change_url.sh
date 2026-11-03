#!/bin/bash

# Define as URLs antiga e nova
OLD_URL="linkaProject.pythonanywhere.com"
NEW_URL="linka-ztg3.onrender.com"

echo "🔍 Buscando ocorrências de '$OLD_URL'..."

# Encontra e substitui em todos os arquivos de código/configuração
# Ignora diretórios ocultos como .git, .venv e arquivos binários/banco de dados
find . -type f \( -name "*.py" -o -name "*.json" -o -name "*.env*" -o -name "*.txt" -o -name "*.md" \) \
  ! -path "*/.git/*" \
  ! -path "*/.venv/*" \
  ! -path "*/__pycache__/*" \
  -exec sed -i "s/$OLD_URL/$NEW_URL/g" {} +

echo "✅ Substituição concluída com sucesso!"
echo "📌 Alterações realizadas nos seguintes arquivos:"

# Mostra quais arquivos foram modificados recentemente
git status --short 2>/dev/null || grep -rnw . -e "$NEW_URL" --exclude-dir={.git,.venv,__pycache__}
