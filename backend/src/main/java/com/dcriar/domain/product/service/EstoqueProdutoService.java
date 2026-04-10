package com.dcriar.domain.product.service;

import com.dcriar.api.dto.request.product.AjusteEstoqueProdutoRequestDTO;
import com.dcriar.api.dto.request.product.AjusteEstoqueRequestDTO;
import com.dcriar.api.dto.response.product.AjusteEstoqueCanalResumoDTO;
import com.dcriar.api.dto.response.product.AjusteEstoqueProdutoResumoDTO;
import com.dcriar.api.dto.response.product.ConsultaEstoqueResponseDTO;
import com.dcriar.api.dto.response.product.EstoqueProdutoResumoDTO;
import com.dcriar.api.dto.response.product.EstoqueResponseDTO;
import com.dcriar.api.dto.response.product.HistoricoEstoqueConsolidadoResponseDTO;
import com.dcriar.api.dto.response.product.ProdutoEstoqueResponseDTO;
import com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

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
     * Busca resumida de estoque filtrada por canal e nome do produto.
     *
     * @param canalId ID do canal de venda.
     * @param nomeProduto Parte do nome ou SKU do produto (opcional).
     * @param apenasComSaldo Se true, retorna apenas registros com quantidade > 0.
     * @param pageable Paginação.
     * @return Página de DTOs de resumo.
     */
    Page<EstoqueProdutoResumoDTO> buscarEstoqueResumido(Long canalId, String nomeProduto, boolean apenasComSaldo, Pageable pageable);

    /**
     * Lista produtos prontos para ajuste físico, com busca textual e filtro opcional por tipo.
     */
    Page<AjusteEstoqueProdutoResumoDTO> listarProdutosParaAjuste(String nomeProduto, String tipoProduto, Pageable pageable);

    /**
     * Lista distribuições por canal prontas para ajuste, com busca textual e filtro opcional por canal.
     */
    Page<AjusteEstoqueCanalResumoDTO> listarCanaisParaAjuste(String nomeProduto, Long canalVendaId, Pageable pageable);

    /**
     * Lista consultas de estoque em um contrato único para a tela de consultas.
     * Permite combinar produto, nome/SKU, canal e filtro de saldo em uma resposta paginada.
     */
    Page<ConsultaEstoqueResponseDTO> listarConsultasEstoque(
            Long produtoId,
            String nomeProduto,
            Long canalVendaId,
            boolean apenasComSaldo,
            Pageable pageable
    );

    /**
     * Lista o histórico consolidado das movimentações de estoque de produtos acabados.
     * Este endpoint é pensado para consultas paginadas e ordenáveis em telas de auditoria
     * e acompanhamento operacional, podendo ser filtrado por período, produto e tipo de movimentação.
     *
     * @param periodo Filtro rápido de período (ex: 1d, 1m, 6m, 1a, all).
     * @param produtoId ID do produto para filtro específico (opcional).
     * @param nomeProduto Parte do nome ou SKU do produto para busca textual (opcional).
     * @param tipoMovimentacao Tipo da movimentação para filtro específico (opcional).
     * @param pageable Configuração de paginação e ordenação.
     * @return Página com o histórico consolidado de movimentações.
     */
    Page<HistoricoEstoqueConsolidadoResponseDTO> listarHistoricoConsolidado(
            String periodo,
            Long produtoId,
            String nomeProduto,
            TipoMovimentacaoProduto tipoMovimentacao,
            Pageable pageable
    );

    /**
     * Lista o estoque de múltiplos produtos, agrupados por canal de venda.
     * Este método é otimizado para o frontend, que precisa buscar os dados de vários produtos de uma só vez.
     *
     * @param produtoIds A lista de IDs de produtos a serem consultados.
     * @return Uma lista de DTOs, onde cada DTO contém o ID do produto e uma lista de seus estoques por canal.
     */
    List<ProdutoEstoqueResponseDTO> listarEstoquePorListaDeProdutos(List<Long> produtoIds);
}
