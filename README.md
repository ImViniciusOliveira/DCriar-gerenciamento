# DCriar - Sistema de Gestão de Estoque e Produção

## Visão Geral

O DCriar é um sistema completo para gestão de estoque e produção, projetado para pequenas e médias empresas que trabalham com a transformação de matéria-prima em produtos acabados. O sistema permite o controle total do ciclo de vida do produto, desde a entrada da matéria-prima até a venda final em diversos marketplaces.

O fluxo principal do sistema é o seguinte:

1.  **Cadastro de Matéria-Prima:** Cadastre os tipos de matéria-prima que você utiliza, como rolos de adesivo, tecidos, etc.
2.  **Entrada de Lotes:** Registre a entrada de lotes de matéria-prima no estoque, com informações como quantidade, dimensões e fornecedor.
3.  **Cadastro de Produtos:** Crie os "moldes" dos seus produtos acabados, definindo suas dimensões, a matéria-prima utilizada e outras características.
4.  **Ordens de Produção:** Crie ordens de produção para fabricar seus produtos. O sistema calcula o consumo de matéria-prima com base nos lotes disponíveis, otimizando o uso e minimizando o desperdício.
5.  **Controle de Estoque:** O sistema atualiza automaticamente o estoque de matéria-prima e de produtos acabados após cada ordem de produção.
6.  **Gestão de Vendas:** Registre as vendas realizadas em diferentes canais de venda (marketplaces), como Shopee, Mercado Livre, etc.

## Principais Funcionalidades

*   **Controle de Estoque de Matéria-Prima:**
    *   Cadastro de tipos de matéria-prima.
    *   Entrada de lotes com rastreabilidade.
    *   Cálculo de saldo em tempo real.
    *   Suporte a atributos flexíveis por lote (ex: largura, gramatura).

*   **Gestão de Produtos:**
    *   Cadastro de produtos com SKU, descrição, dimensões e imagem.
    *   Associação de produtos a tipos de matéria-prima.

*   **Produção Inteligente:**
    *   Criação de ordens de produção.
    *   Cálculo de consumo de matéria-prima com base em algoritmos de otimização de corte.
    *   Geração de retalhos (sobras de matéria-prima) para reaproveitamento.

*   **Gestão de Vendas:**
    *   Cadastro de canais de venda (marketplaces).
    *   Registro de vendas e itens vendidos.
    *   Atualização automática do estoque de produtos acabados.

## Estrutura do Projeto

O projeto é dividido em duas partes principais:

*   **Backend:** Uma aplicação Spring Boot que implementa toda a lógica de negócio e a API REST.
*   **Frontend:** Uma aplicação (a ser desenvolvida) que consome a API do backend e fornece a interface para o usuário.

## Como Começar

Para executar o projeto em seu ambiente de desenvolvimento, siga os passos abaixo:

1.  **Clone o repositório:**
    ```bash
    git clone https://github.com/seu-usuario/dcriar-sistema-inventario.git
    cd dcriar-sistema-inventario
    ```

2.  **Configure o ambiente de desenvolvimento:**
    *   Consulte o arquivo `readme/README.dev.md` para instruções detalhadas sobre como configurar o ambiente de desenvolvimento, incluindo o banco de dados, o MinIO e as variáveis de ambiente.

3.  **Execute o backend:**
    *   Você pode executar o backend diretamente pela sua IDE (IntelliJ, VSCode, etc.) ou como um container Docker. Consulte o `readme/README.dev.md` para mais detalhes.

4.  **Execute o frontend:**
    *   (Instruções a serem adicionadas quando o frontend for desenvolvido)

## Contribuição

Contribuições são bem-vindas! Se você tiver alguma ideia para melhorar o sistema, sinta-se à vontade para abrir uma issue ou enviar um pull request.
