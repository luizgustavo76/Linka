import sys
from PyQt6.QtWidgets import QApplication, QWidget

def main():
    # Cria a aplicação (obrigatório)
    app = QApplication(sys.argv)

    # Cria a janela principal
    window = QWidget()
    window.setWindowTitle("Faça login no Linka!")
    window.resize(400, 300) 
    window.show()
    sys.exit(app.exec())

if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        print(f"Erro ao executar a aplicação: {e}")
