package com.dcriar.domain.product.service;

import com.dcriar.api.dto.request.product.MovimentacaoEstoqueProdutoRequestDTO;
import com.dcriar.api.dto.response.product.MovimentacaoEstoqueProdutoResponseDTO;

import java.util.List;

/**
 * Contrato para manipulação de movimentações de estoque de produto acabado.
 * <p>
 * Centraliza regras de negócio e uso dos métodos from/updateFrom da entidade.
 */
public interface MovimentacaoEstoqueProdutoService {

    /**
     * Registra uma nova movimentação de estoque para um produto.
     *
     * @param requestDTO DTO de request com os dados da movimentação
     * @return DTO de resposta da movimentação registrada
     */
    MovimentacaoEstoqueProdutoResponseDTO registrarMovimentacao(MovimentacaoEstoqueProdutoRequestDTO requestDTO);

    /**
     * Atualiza uma movimentação existente.
     *
     * @param id ID da movimentação
     * @param requestDTO DTO de request com os dados atualizados
     * @return DTO de resposta da movimentação atualizada
     */
    MovimentacaoEstoqueProdutoResponseDTO atualizarMovimentacao(Long id, MovimentacaoEstoqueProdutoRequestDTO requestDTO);

    /**
     * Lista todas as movimentações de estoque de um produto.
     *
     * @param produtoId ID do produto
     * @return Lista de DTOs de resposta das movimentações
     */
    List<MovimentacaoEstoqueProdutoResponseDTO> listarPorProduto(Long produtoId);
}

