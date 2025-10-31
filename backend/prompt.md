# Guia de Estilo e Arquitetura para o Projeto DCriar

Este documento define as convenções e padrões a serem seguidos pela IA ao trabalhar neste projeto.

## 1. Princípios Gerais

- **Clean Code:** Todos os princípios de Clean Code devem ser aplicados. Mantenha métodos curtos, nomes de variáveis claros e siga o Princípio da Responsabilidade Única (SRP).

## 2. Documentação e Comentários

- **Javadoc:** Nunca remova Javadoc existente. Apenas adicione ou melhore a documentação para refletir as novas mudanças. Todos os métodos públicos e classes devem ter Javadoc claro e conciso.
- **Comentários de Bloco:** Para métodos complexos ou blocos de código extensos, adicione comentários `//` que expliquem a lógica em um formato passo a passo (ex: `// 1. Buscar...`, `// 2. Mesclar...`).

## 3. Linguagem e Estilo de Código (Java)

- **Lombok:** Utilize as anotações do Lombok de forma extensiva para reduzir código boilerplate.
  - Sempre prefira o uso de `@Builder` para a construção de entidades e DTOs.
  - Use `@RequiredArgsConstructor` para injeção de dependência em services e controllers.

## 4. Arquitetura e Estrutura do Projeto

- **Estrutura de Pastas:** Antes de criar um novo arquivo (DTO, Exception, Service, etc.), utilize as ferramentas de busca (`find_files`, `grep`) para identificar a pasta correta com base na estrutura existente. Mantenha a consistência do projeto.
- **Camadas:** Siga estritamente a arquitetura em três camadas:
  1.  **Controller:** Responsável apenas por HTTP. Deve ser "enxuto" (thin).
  2.  **Service:** Contém toda a lógica de negócio.
  3.  **Repository:** Apenas para acesso a dados.

## 5. Commits e Git

- **Padrão:** Use o padrão **Conventional Commits** (`feat:`, `fix:`, `refactor:`, `docs:`, etc.).
- **Mensagens:** Escreva mensagens de commit claras e em português.

## 6. Validações

- Sempre utilize validação de classe nos DTOs de request, usando anotações como `@NotNull`, `@NotEmpty`, `@Positive`, etc.
- Evite validações em camadas erradas (ex: não validar dados no repository).
- Documente as validações no Swagger/OpenAPI para que o consumidor saiba os requisitos de cada campo.
- Utilize validações customizadas quando necessário (ex: anotação própria para regras de negócio).

## 7. Lombok e DTOs

- Sempre que criar ou atualizar entidades a partir de DTOs de request, utilize os métodos from() e updateFrom() na entidade para centralizar regras de negócio. O mapper deve ser usado apenas para conversão simples entre entidade e DTO de resposta, e caso precise editar o from() ou updateFrom() ou mapper sempre fazer isso.
- Prefira anotações específicas do Lombok como `@Getter`, `@Setter`, `@Builder`, `@AllArgsConstructor`, `@NoArgsConstructor` em vez de `@Data` para maior controle e clareza.
- Use `@EqualsAndHashCode` e `@ToString` apenas quando necessário.
- Documente todos os campos públicos com Javadoc.

## 8. HATEOAS

- Padronize o uso de HATEOAS para enriquecer as respostas da API com links de navegação.
- Documente no Swagger/OpenAPI os links retornados em cada endpoint.
- Mantenha a separação clara entre DTOs de dados e DTOs de resposta HATEOAS.

## 9. Padronização de APIs e Swagger

- Defina convenções para versionamento de endpoints (ex: `/api/v1/...`).
- Use OpenAPI/Swagger para gerar documentação automática e exemplos de payloads.
- Mantenha os exemplos de resposta e erro atualizados no Swagger.

## 10. Revisão de Código

- Exija code review antes do merge.
- Defina critérios mínimos para aprovação (ex: sem warnings, cobertura de testes, documentação atualizada).
- Recomende uso de ferramentas de análise estática (ex: SonarQube).

## 11. Testes Automatizados

- Recomende o uso de testes unitários e de integração (JUnit, Mockito).
- Defina padrões para cobertura mínima de testes.
- Sugira o uso de nomes claros para métodos de teste e organização dos arquivos de teste.

## 12. Tratamento de Erros

- Padronize o uso de exceptions customizadas para erros de negócio.
- Crie uma estrutura de resposta de erro JSON (ex: `timestamp`, `status`, `error`, `message`, `path`).
- Documente os erros esperados nos endpoints usando Swagger/OpenAPI.

## 13. Configuração

- Padronize o uso de arquivos de configuração (`application.yml`, `application.properties`) para variáveis de ambiente e parâmetros sensíveis.
- Recomende o uso de `@ConfigurationProperties` para mapear configurações em classes Java.
- Oriente sobre o versionamento e documentação das configurações.

## 14. Deprecação

- Defina um padrão para marcar métodos, endpoints e classes obsoletas usando a anotação `@Deprecated` e comentários explicativos.
- Documente no Swagger/OpenAPI quais endpoints estão obsoletos e a alternativa recomendada.
- Estabeleça um processo para remoção segura de código legado (ex: ciclo de vida de deprecação).

## 15. Performance

- Oriente sobre monitoramento de queries lentas (ex: logs SQL, uso de @Transactional).
- Sugira uso de cache para dados frequentemente acessados (ex: Spring Cache).
- Recomende profiling periódico da aplicação.
