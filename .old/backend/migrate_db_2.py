import os
import sqlite3
import psycopg2
from psycopg2.extras import execute_values
import dotenv

BASE_DIR = os.path.dirname(os.path.abspath(__file__))
env_path = os.path.join(BASE_DIR, "backend.env")

if os.path.exists(env_path):
    dotenv.load_dotenv(env_path)
else:
    dotenv.load_dotenv()

DATABASE_URL = os.getenv("DATABASE_URL")
DB_DIR = os.path.join(BASE_DIR, "DB")

if not DATABASE_URL:
    print("❌ ERRO: DATABASE_URL não encontrada!")
    exit(1)

pg_conn = psycopg2.connect(DATABASE_URL)
pg_cur = pg_conn.cursor()

def sqlite_to_pg_type(col_name, sqlite_type):
    col_name = col_name.lower()
    sqlite_type = sqlite_type.upper().strip()
    if "date" in col_name or "time" in col_name or "timestamp" in col_name:
        return "TEXT"
    if "INTEGER" in sqlite_type or "INT" in sqlite_type:
        return "INTEGER"
    if "REAL" in sqlite_type or "FLOAT" in sqlite_type or "DOUBLE" in sqlite_type:
        return "DOUBLE PRECISION"
    if "BLOB" in sqlite_type:
        return "BYTEA"
    return "TEXT"

def migrar_rapido(caminho_sqlite):
    nome_arquivo = os.path.basename(caminho_sqlite)
    print(f"\n📂 Lendo: {nome_arquivo}")

    sq_conn = sqlite3.connect(caminho_sqlite)
    sq_conn.row_factory = sqlite3.Row
    sq_cur = sq_conn.cursor()

    sq_cur.execute("SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%';")
    tabelas = [row[0] for row in sq_cur.fetchall()]

    for tabela in tabelas:
        if "token" in tabela.lower() or "sincronizer" in tabela.lower():
            print(f"  ⏩ Ignorando tabela pesada/temporária: '{tabela}'")
            continue

        # 1. Pega estrutura das colunas
        sq_cur.execute(f"PRAGMA table_info('{tabela}');")
        colunas_info = sq_cur.fetchall()
        
        col_names = []
        col_definitions = []
        has_id = False

        for col in colunas_info:
            c_name = col[1]
            c_type = col[2]
            c_pk = col[5]
            
            c_name_escaped = f'"{c_name}"' if c_name != c_name.lower() else c_name
            col_names.append(c_name_escaped)

            pg_type = sqlite_to_pg_type(c_name, c_type)

            if c_name.lower() == "id" and c_pk == 1:
                col_definitions.append(f'{c_name_escaped} SERIAL PRIMARY KEY')
                has_id = True
            elif c_pk == 1:
                col_definitions.append(f'{c_name_escaped} {pg_type} PRIMARY KEY')
            else:
                col_definitions.append(f'{c_name_escaped} {pg_type}')

        # 2. Cria tabela se não existir
        create_query = f"CREATE TABLE IF NOT EXISTS {tabela} ({', '.join(col_definitions)});"
        try:
            pg_cur.execute(create_query)
            pg_conn.commit()
        except Exception:
            pg_conn.rollback()

        # 3. Lê todos os dados do SQLite
        sq_cur.execute(f"SELECT * FROM {tabela};")
        rows = sq_cur.fetchall()

        if not rows:
            print(f"  ℹ️ Tabela '{tabela}' vazia.")
            continue

        # 4. Inserção em LOTE (Batch Insert instantâneo via execute_values)
        cols_joined = ", ".join(col_names)
        insert_query = f"INSERT INTO {tabela} ({cols_joined}) VALUES %s ON CONFLICT DO NOTHING;"
        
        valores = [[str(r[i]) if r[i] is not None else None for i in range(len(r))] for r in rows]

        try:
            execute_values(pg_cur, insert_query, valores, page_size=1000)
            pg_conn.commit()
            print(f"  ⚡ {len(rows)} registos enviados de uma vez para '{tabela}'!")
        except Exception as e:
            pg_conn.rollback()
            print(f"  ⚠️ Erro na inserção em lote de '{tabela}': {e}")

        if has_id:
            try:
                pg_cur.execute(f"SELECT setval('{tabela}_id_seq', COALESCE((SELECT MAX(id) FROM {tabela}), 1));")
                pg_conn.commit()
            except Exception:
                pg_conn.rollback()

    sq_conn.close()

if __name__ == "__main__":
    arquivos_db = [os.path.join(DB_DIR, f) for f in os.listdir(DB_DIR) if f.endswith(".db")]
    
    for caminho_db in arquivos_db:
        nome = os.path.basename(caminho_db).lower()
        if "token" in nome or "sincronizer" in nome:
            continue
        migrar_rapido(caminho_db)

    pg_cur.close()
    pg_conn.close()
    print("\n🚀 MIGRAÇÃO ULTRA-RÁPIDA CONCLUÍDA!")
