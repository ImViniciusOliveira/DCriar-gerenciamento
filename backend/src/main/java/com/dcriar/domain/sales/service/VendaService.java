package com.dcriar.domain.sales.service;

import com.dcriar.api.dto.request.sales.VendaRequestDTO;
import com.dcriar.api.dto.response.sales.VendaResponseDTO;

import java.util.List;

/**
 * Interface que define o contrato para a lógica de negócio de Vendas (Sales).
 */
public interface VendaService {

    /**
     * Regista uma nova venda no sistema e orquestra a baixa automática de estoque.
     */
    VendaResponseDTO registrarVenda(VendaRequestDTO requestDTO);

    /**
     * Lista todas as vendas registadas no sistema.
     *
     * @return Uma lista com os DTOs de resposta de todas as vendas.
     */
    List<VendaResponseDTO> findAll();

    /**
     * Busca uma venda específica pelo seu ID.
     *
     * @param id O ID da venda a ser buscada.
     * @return O DTO de resposta da venda encontrada.
     */
    VendaResponseDTO findById(Long id);
}

