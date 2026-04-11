#!/bin/bash

echo "Compilando login.cpp com Qt..."

g++ -fPIC login.cpp -o login \
`pkg-config --cflags --libs Qt5Widgets Qt5Gui Qt5Core`

if [ $? -eq 0 ]; then
    echo "OK!"
    ./login
else
    echo "Erro na compilação!"
fi