# Controllers não implementados

## Sugestão de futuro controller para MovimentacaoEstoqueProduto

- Criar um controller dedicado para MovimentacaoEstoqueProduto, permitindo:
  - Consulta de movimentações por produto, período, tipo, etc.
  - Criação manual de movimentações (ajustes, correções, integrações externas).
  - Atualização de movimentações (casos excepcionais).
  - Exposição de extrato de movimentações para auditoria/relatórios.
- Seguir o padrão do projeto: usar DTOs, métodos from/updateFrom na entidade, e mapper apenas para conversão simples.
- Documentar endpoints e exemplos no Swagger/OpenAPI.

## Sugestão de futuro controller para CanalVenda

- Criar um controller REST dedicado para CanalVenda, permitindo:
  - Cadastro de novos canais de venda (usando CanalVendaRequestDTO e método CanalVenda.from)
  - Edição/atualização de canais existentes (usando CanalVendaRequestDTO e método CanalVenda.updateFrom)
  - Consulta de canal por ID
  - Listagem de todos os canais de venda
  - Exclusão de canal (se aplicável, com validação de uso)
- Utilizar DTOs de request/response conforme padrão do projeto
- Centralizar regras de negócio na entidade CanalVenda
- Documentar endpoints e exemplos no Swagger/OpenAPI
- Implementar validações e tratamento de erros padronizados

## Sugestão de futuro controller para Preco

- Criar um controller REST dedicado para Preco, permitindo:
  - Cadastro de novos preços para produtos (usando PrecoRequestDTO e método Preco.from)
  - Edição/atualização de preços existentes (usando PrecoRequestDTO e método Preco.updateFrom)
  - Consulta de preço por ID, produto e tipo
  - Listagem de todos os preços
  - Exclusão de preço (se aplicável, com validação de uso)
- Utilizar DTOs de request/response conforme padrão do projeto
- Centralizar regras de negócio na entidade Preco
- Documentar endpoints e exemplos no Swagger/OpenAPI
- Implementar validações e tratamento de erros padronizados

## Sugestão de futuro controller para CorteRealizado

- Criar um controller REST dedicado para CorteRealizado, permitindo:
  - Cadastro de cortes realizados (usando CorteRealizadoRequestDTO e método CorteRealizado.from)
  - Edição/atualização de cortes realizados (usando CorteRealizadoRequestDTO e método CorteRealizado.updateFrom)
  - Consulta de corte realizado por ID
  - Listagem de cortes realizados por ordem de produção
  - Listagem de todos os cortes realizados
  - Exclusão de corte realizado
- Utilizar DTOs de request/response conforme padrão do projeto
- Centralizar regras de negócio na entidade CorteRealizado
- Documentar endpoints e exemplos no Swagger/OpenAPI
- Implementar validações e tratamento de erros padronizados
