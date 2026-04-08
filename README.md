# Sistema de Estoque, Produção e Vendas

Sistema web para controlar estoque, produção, vendas e distribuição por canal.

O projeto foi construído para resolver um fluxo completo: entrada de matéria-prima, transformação em produto acabado, movimentação de estoque, produção orientada por consumo e corte, e venda com impacto automático no saldo.

## O Que O Sistema Resolve

- controla produtos, preços e canais de venda
- controla lotes de matéria-prima e histórico de movimentações
- registra ordens de produção e consumo de insumos
- distribui estoque por canal e permite ajustes
- registra vendas e baixa o estoque automaticamente
- mantém histórico operacional para auditoria

## O Que Tem De Interessante Aqui

- backend em `Java 21 + Spring Boot 3.5.5`
- frontend em `Angular 21`
- PostgreSQL com `Flyway`
- MinIO para arquivos
- deploy com `Docker Compose`
- HTTPS no frontend/proxy via `nginx`
- validações customizadas no backend
- criptografia em repouso para dados sensíveis de venda

## Arquitetura

- `backend/`: API REST com regras de negócio, validações, migrações e integração com banco/MinIO
- `frontend/`: interface web Angular para operação diária
- `docker-compose.dev.yml`: infraestrutura local de desenvolvimento
- `docker-compose.prod.yml`: stack de produção

## Fluxos Principais

### 1. Estoque e matéria-prima
- cadastro de tipos de matéria-prima
- entrada de lotes
- movimentações e histórico
- saldo físico
- distribuição por canal

### 2. Produção
- ordem de produção por produto
- cálculo de consumo e corte
- geração de movimentações
- atualização de estoque acabado

### 3. Vendas
- venda por canal
- itens com preço padrão ou alterado
- baixa automática de estoque
- dados de cliente com exposição controlada

## Dados Sensíveis Em Vendas

A aplicação trata dados sensíveis de vendas com criptografia em repouso e exposição controlada na API.

## Stack Técnica

- Backend: Java 21, Spring Boot 3.5.5, Spring Data JPA, Validation, HATEOAS, Actuator, Springdoc, Flyway
- Frontend: Angular 21, Angular Material, ngx-mask
- Banco: PostgreSQL 14
- Storage: MinIO
- Infra: Docker Compose + nginx

## Guias Do Projeto

O projeto tem guias objetivos para executar e publicar a aplicação:

- desenvolvimento local: [Guia de Desenvolvimento](/home/viniciusdev/dcriar/dcriar-sistema-inventario/readme/README.dev.md)
- produção e deploy: [Guia de Produção](/home/viniciusdev/dcriar/dcriar-sistema-inventario/readme/README.deploy.md)
- comandos diretos de Docker e Docker Hub: [Guia de Comandos](/home/viniciusdev/dcriar/dcriar-sistema-inventario/readme/README.comandos.md)

Esses arquivos mostram o fluxo real do projeto com Docker Compose, backend pela IDE e deploy entre máquinas.

## Diferenciais Do Projeto

- fluxo completo de estoque, produção e venda no mesmo sistema
- regras de negócio explícitas no backend
- normalização e validações customizadas para manter consistência dos dados
- deploy com HTTPS no frontend/proxy
- separação clara entre ambiente local e produção
- proteção de dados sensíveis nas vendas
