# Importa a classe do arquivo onde salvou a lib (ex: linkafederations.py)
# Se instalou via 'pip install -e .', basta fazer: from LinkaFederations import LinkaFederations
from linkaFederations import LinkaFederations

# 1. Inicializa o motor de federação (ele cria o banco e carrega o cache sozinho)
linka = LinkaFederations()

# 2. Prepara o payload do Linka Protocol que vai trafegar na rede
dados_da_mensagem = {
    "username":"opa",
    "text_post":"hhddiddihdh"
}

# 3. Dispara o payload usando o LinkaFederations!
# Você pode passar o slug que está no cache...
resposta = linka.sendPayload(dados_da_mensagem, "http://127.0.0.1:5000/new")

# ...ou passar a URL direta da outra instância se o cache falhar
# resposta = linka.sendPayload(dados_da_mensagem, "https://instancia2.linka.net/api/receive")

if resposta:
    print("Sucesso! Mensagem federada com sucesso.")
else:
    print("A instância destino recusou ou está offline.")