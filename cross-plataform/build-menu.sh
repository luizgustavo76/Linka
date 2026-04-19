#!/bin/bash

echo "Compilando menu.cpp com Qt..."

g++ menu.cpp -o menu $(pkg-config --cflags --libs Qt6Widgets Qt6Network)

if [ $? -eq 0 ]; then
    echo "OK!"
    ./menu
else
    echo "Erro na compilação!"
fi