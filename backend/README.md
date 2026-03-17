# DCriar API

API de gerenciamento de estoque, produção, vendas e produtos para a DCriar.

---

## Visão Geral
A DCriar API é uma solução robusta para controle de estoque, produção, vendas e gestão de produtos, desenvolvida em Java 21 com Spring Boot. O projeto segue padrões profissionais de arquitetura, documentação e testes, facilitando integração, manutenção e escalabilidade.

---

## Principais Funcionalidades
- **Gestão de Produtos:** Cadastro, consulta, atualização, exclusão, controle de estoque e precificação.
- **Gestão de Estoque:** Movimentação, ajuste, consulta por canal de venda e estoque físico.
- **Gestão de Produção:** Ordens de produção, corte geométrico, consumo, margens e lotes de matéria-prima.
- **Gestão de Vendas:** Registro de vendas, cálculo de preços, validação de saldo, integração com estoque.
- **Relatórios e Simulações:** Simulação de corte, consumo, histórico de movimentações.
- **Padronização Javadoc:** Documentação completa e navegável via Maven.

---

## Estrutura do Projeto
```
DCriar/
├── src/
│   ├── main/java/com/dcriar/
│   │   ├── api/           # Controllers, DTOs, Hateoas, Validation, Mapper
│   │   ├── domain/        # Entities, Services, Repositories, Exceptions
│   │   ├── config/        # Configurações globais (Jackson, Flyway, OpenAPI)
│   ├── resources/         # application.yml, migrations, estáticos
│   ├── test/java/com/dcriar/ # Testes automatizados
├── pom.xml                # Configuração Maven
├── README.md              # Este arquivo
├── prompt.md              # Guia de padronização Javadoc
└── ...
```

---

## Instalação e Configuração
1. **Pré-requisitos:**
   - Java 21+
   - Maven 3.8+
   - PostgreSQL
2. **Configuração do banco de dados:**
   - Edite `src/main/resources/application.yml` com suas credenciais.
   - O Flyway gerencia as migrações automaticamente.
3. **Configuração do Jackson:**
   - O projeto já está configurado para envelopar respostas JSON:
     ```yaml
     jackson:
       serialization:
         wrap-root-value: true
     ```
4. **Instale as dependências:**
   ```bash
   mvn clean install
   ```

---

## Licença
Este projeto possui uma licença personalizada. O código pode ser utilizado apenas para fins de estudo, uso interno ou acadêmico. É proibida a venda, distribuição comercial ou qualquer uso com fins lucrativos sem autorização expressa do autor.

Para mais detalhes, consulte o arquivo LICENSE na raiz do projeto ou entre em contato pelo LinkedIn.

---

## Como Rodar
```bash
mvn spring-boot:run
```
A API estará disponível em `http://localhost:8080`.

---

## Documentação Javadoc
Gere a documentação HTML completa:
```bash
mvn clean javadoc:javadoc
```
Acesse em `target/site/apidocs/index.html`.

---

## Exemplos de Uso
### Cadastro de Produto
```http
POST /api/produtos
Content-Type: application/json
{
  "nome": "Etiqueta Personalizada",
  "sku": "ETQ-001",
  "descricao": "Etiqueta em vinil para personalização.",
  "cor": "Branco",
  "unidadesPorProduto": 100,
  "tipoMateriaPrimaId": 1
}
```
### Consulta de Estoque
```http
GET /api/estoque?produtoId=1&canalVendaId=2
```
### Registro de Venda
```http
POST /api/vendas
Content-Type: application/json
{
  "itens": [ ... ],
  "canalVendaId": 2
}
```

---

## Autenticação e Autorização
- Endpoints sensíveis podem exigir autenticação JWT.
- Consulte a documentação OpenAPI gerada para detalhes de segurança.

---

## Testes Automatizados
Execute todos os testes:
```bash
mvn test
```

---

## Convenções e Contribuição
- Siga o prompt de padronização (`prompt.md`) para documentação.
- Use padrões de código Java e Spring Boot.
- Para contribuir, abra uma issue ou pull request detalhando sua proposta.

---

## Contato e Suporte
- [ImViniciusOliveira/DCriar-Api](https://github.com/ImViniciusOliveira/DCriar-Api)
- Para dúvidas, sugestões ou bugs, utilize o GitHub Issues.
- [LinkedIn: Vinicius Oliveira](https://www.linkedin.com/in/imviniciusoliveira)

---

## Links Úteis
- [Documentação Javadoc HTML](target/site/apidocs/index.html)
- [Prompt de Javadoc](prompt.md)
- [Spring Boot](https://spring.io/projects/spring-boot)
- [OpenAPI](https://springdoc.org/)

---

> Projeto mantido e revisado profissionalmente. Para detalhes técnicos, consulte o guia de Javadoc e a documentação HTML gerada.
