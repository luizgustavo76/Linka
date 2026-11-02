import os
import psycopg2
from psycopg2.extras import RealDictCursor
from flask import Blueprint

jobs_bp = Blueprint("jobs_bp", __name__)

DATABASE_URL = os.getenv("DATABASE_URL")

def get_db():
    return psycopg2.connect(DATABASE_URL, cursor_factory=RealDictCursor)

def sync_profiles():
    """ 
    Sincroniza a tabela 'profile' criando um registro para todo usuário 
    da tabela 'login' que ainda não possua um perfil criado.
    Substitui os dois cursores do SQLite por uma query nativa no PostgreSQL.
    """
    if not DATABASE_URL:
        print("[ERRO] DATABASE_URL não foi configurada nas variáveis de ambiente.")
        return

    conn = get_db()
    cur = conn.cursor()
    
    try:
        # Insere em 'profile' selecionando os usuários de 'login' que não existem em 'profile'
        query = """
            INSERT INTO profile (username, bio, actual_badge, "ProfilePicture", followers, following)
            SELECT 
                l.username, 
                CONCAT('Hi! my name is ', l.username), 
                NULL, 
                NULL, 
                0, 
                0
            FROM login l
            LEFT JOIN profile p ON l.username = p.username
            WHERE p.username IS NULL AND l.username IS NOT NULL AND l.username != '';
        """
        cur.execute(query)
        inserted_count = cur.rowcount  # Retorna exatamente quantas linhas foram inseridas
        conn.commit()
        print(f"[OK] {inserted_count} novos perfis sincronizados no PostgreSQL!")
    except Exception as e:
        conn.rollback()
        print(f"[ERRO] Falha ao sincronizar perfis: {e}")
    finally:
        cur.close()
        conn.close()

def run_jobs():
    print("--- INICIANDO SYNC DE BANCOS NO NEON.TECH ---")
    sync_profiles()
    print("--- CONCLUÍDO ---")

if __name__ == "__main__":
    run_jobs()