try:
    import login
    import post
    import chat
    import threading
    t1 = threading.Thread(target=chat.chat)
    t2 = threading.Thread(target=post.posts)
    t3 = threading.Thread(target=login.login)

    t1.start()
    t2.start()
    t3.start()

    

    t1.join()
    t2.join()
    t3.join()
    print('servidor iniciado com sucesso')
except Exception as e:
    print("servidor falhou ao iniciar tente reiniciar-lo")