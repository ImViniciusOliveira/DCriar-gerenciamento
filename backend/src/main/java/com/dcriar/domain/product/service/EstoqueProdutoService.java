package com.dcriar.domain.product.service;

import com.dcriar.api.dto.request.product.AjusteEstoqueProdutoRequestDTO;
import com.dcriar.api.dto.request.product.AjusteEstoqueRequestDTO;
import com.dcriar.api.dto.response.product.EstoqueResponseDTO;
import com.dcriar.api.dto.response.product.MovimentacaoProdutoResponseDTO;
import com.dcriar.api.dto.response.product.ProdutoEstoqueResponseDTO;

import java.util.List;

/**
 * Interface que define o contrato para a lógica de negócio relacionada ao Estoque de Produtos Acabados.
 * Esta camada de serviço encapsula as regras e operações de manipulação de estoque.
 */
public interface EstoqueProdutoService {

    /**
     * Ajusta o estoque de um produto acabado em um canal de venda específico (distribuição).
     * Esta operação pode ser uma entrada ou uma saída, dependendo do tipo de ajuste.
     *
     * @param requestDTO O DTO contendo as informações para o ajuste de estoque.
     * @return O DTO do estoque atualizado após o ajuste.
     */
    EstoqueResponseDTO ajustarEstoque(AjusteEstoqueRequestDTO requestDTO);

    /**
     * Ajusta o Estoque Físico Total de um produto, também conhecido como "Estoque Mestre".
     * Esta é uma operação de ajuste manual que define a quantidade total de um produto.
     *
     * @param requestDTO O DTO contendo o ID do produto e a nova quantidade física.
     */
    void ajustarEstoqueFisico(AjusteEstoqueProdutoRequestDTO requestDTO);

    /**
     * Consulta o estoque de um produto específico em um determinado canal de venda.
     *
     * @param produtoId    O ID do produto a ser consultado.
     * @param canalVendaId O ID do canal de venda onde o estoque será verificado.
     * @return O DTO com as informações do estoque para o produto e canal especificados.
     */
    EstoqueResponseDTO consultarEstoque(Long produtoId, Long canalVendaId);

    /**
     * Lista todo o histórico de movimentações (o "Livro-Razão") do Estoque Físico Total de um produto.
     * Isso permite rastrear todas as entradas e saídas que compõem o estoque atual.
     *
     * @param produtoId O ID do produto cujo histórico de movimentações será consultado.
     * @return Uma lista de DTOs, cada um representando uma movimentação de estoque (entrada, saída, ajuste).
     */
    List<MovimentacaoProdutoResponseDTO> listarMovimentacoesPorProduto(Long produtoId);

    /**
     * Lista o estoque de todos os produtos, agrupados por produto e seus respectivos canais de venda.
     * Este método é otimizado para o frontend, que precisa exibir o estoque de vários produtos de uma só vez.
     *
     * @return Uma lista de DTOs, onde cada DTO contém o ID do produto e uma lista de seus estoques por canal.
     */
    List<ProdutoEstoqueResponseDTO> listarEstoqueDeTodosOsProdutosPorCanal();

    /**
     * Lista o estoque de múltiplos produtos, agrupados por canal de venda.
     * Este método é otimizado para o frontend, que precisa buscar os dados de vários produtos de uma só vez.
     *
     * @param produtoIds A lista de IDs de produtos a serem consultados.
     * @return Uma lista de DTOs, onde cada DTO contém o ID do produto e uma lista de seus estoques por canal.
     */
    List<ProdutoEstoqueResponseDTO> listarEstoquePorListaDeProdutos(List<Long> produtoIds);
}
