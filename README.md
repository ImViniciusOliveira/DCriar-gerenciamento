# Sistema de Estoque, Produção e Vendas

Projeto **full stack** para controle de estoque, produção e vendas, cobrindo o fluxo completo de uma operação que transforma matéria-prima em produto.

Ele foi construído para centralizar:
- entrada e controle de insumos
- produção com consumo e corte
- distribuição de estoque por canal
- vendas com impacto automático no saldo
- histórico operacional para auditoria

---

## Tecnologias Utilizadas

**Backend**
- Java 21
- Spring Boot 3.5.5
- Spring Data JPA / Hibernate
- Spring Validation
- Spring HATEOAS
- Springdoc / OpenAPI
- Flyway
- MapStruct
- MinIO SDK

**Frontend**
- Angular 21
- Angular Material

**Dados / Infra**
- PostgreSQL 14
- MinIO
- Docker Compose
- nginx com HTTPS em rede local

---

## Funcionalidades Principais

### Estoque
- cadastro de produtos e preços
- controle de matéria-prima por lote
- movimentações e histórico de estoque
- distribuição de saldo por canal
- ajustes operacionais

### Produção
- ordens de produção
- cálculo de consumo
- cálculo de corte
- geração de movimentações automáticas
- atualização do estoque

### Vendas
- vendas por canal
- precificação por item
- baixa automática de estoque
- dados do cliente com proteção no backend

---

## Destaques Técnicos

- API com regras de negócio explícitas
- validações customizadas e normalização de dados
- criptografia em repouso para dados sensíveis de vendas
- separação clara entre ambiente de desenvolvimento e produção
- deploy com HTTPS no frontend/proxy

---

## Estrutura Do Projeto

- `backend/`: API REST, regras de negócio, validações, migrações e integração com banco e MinIO
- `frontend/`: interface Angular para operação do sistema
- `docker-compose.dev.yml`: infraestrutura de desenvolvimento
- `docker-compose.prod.yml`: stack de produção

---

## Certificado HTTPS

Em produção, o HTTPS fica no `nginx`, usando certificado e chave montados fora da imagem.

Isso protege o tráfego entre navegador e servidor e deixa o deploy mais seguro e mais flexível.

Esse modelo funciona bem em rede local e também permite trocar depois para um certificado público real, caso a aplicação passe a usar um domínio de internet.

No servidor, os arquivos esperados são:
- `fullchain.crt`
- `private.key`

---

## Guias

- [Guia de Desenvolvimento](guides/README.dev.md)
- [Guia de Produção](guides/README.deploy.md)
- [Guia de Comandos](guides/README.comandos.md)
- [Guia de Certificado TLS](guides/README.certificado.md)
