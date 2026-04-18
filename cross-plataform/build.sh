#!/bin/bash

echo "Compilando login.cpp com Qt..."

g++ login.cpp -o login $(pkg-config --cflags --libs Qt6Widgets Qt6Network)

if [ $? -eq 0 ]; then
    echo "OK!"
    ./login
else
    echo "Erro na compilação!"
fi