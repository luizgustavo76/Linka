# test.py
import linkaBotsSdk

# 1. Instancia a classe que está dentro do pacote
bot = linkaBotsSdk.LinkaBot()

# 2. Define as variáveis do seu bot na instância criada
bot.bot_name = "bot_do_linka"
bot.url = "http://127.0.0.1:5000"
bot.bot_password = "08102012"
bot.bot_token = "fe3ec510c3219916f35503822a925687"

# 3. Dispara a função (ela já usa o token interno do self)
bot.sendMessage("hello", "linka")