package com.dcriar.domain.sales.service;

import com.dcriar.api.dto.request.sales.VendaRequestDTO;
import com.dcriar.api.dto.response.sales.VendaResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface que define o contrato para a lógica de negócio de Vendas (Sales).
 */
public interface VendaService {

    /**
     * Regista uma nova venda no sistema e orquestra a baixa automática de estoque.
     */
    VendaResponseDTO registrarVenda(VendaRequestDTO requestDTO);

    /**
     * Lista todas as vendas registadas no sistema de forma paginada.
     *
     * @param pageable Objeto com as informações de paginação.
     * @return Uma página de DTOs de resposta de vendas.
     */
    Page<VendaResponseDTO> findAll(Pageable pageable);

    /**
     * Busca uma venda específica pelo seu ID.
     *
     * @param id O ID da venda a ser buscada.
     * @return O DTO de resposta da venda encontrada.
     */
    VendaResponseDTO findById(Long id);
}
