MinIO init (desenvolvimento)

Este diretório contém uma pequena imagem usada apenas durante o desenvolvimento para inicializar o MinIO:
- instala o cliente MinIO (`mc`)
- executa um script Python que aguarda o MinIO ficar disponível
- cria o bucket configurado (se não existir)
- aplica uma política pública ao bucket

Quando usar
- No ambiente de desenvolvimento local, para evitar ter scripts montados do host e garantir comportamento reprodutível.

Como usar
- Na raiz do projeto, execute (reconstruindo a imagem init):

```bash
docker compose -f docker-compose.dev.yml up --build postgres-dev minio-dev minio-init
```

Variáveis de ambiente utilizadas
- MINIO_ROOT_USER, MINIO_ROOT_PASSWORD, MINIO_BUCKET_NAME (obrigatórias)
- MINIO_HOST, MINIO_PORT (padrões: minio-dev, 9000)
- WAIT_TIMEOUT (segundos, padrão: 60)

Observações
- Substitui o uso do `scripts/wait-for-minio.sh` no compose de desenvolvimento. Caso prefira manter o script shell como referência, ele pode ser recuperado do histórico do repositório.
