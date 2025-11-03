package com.dcriar.domain.product.service;

import com.dcriar.api.dto.request.product.CanalVendaRequestDTO;
import com.dcriar.api.dto.response.product.CanalVendaResponseDTO;
import java.util.List;

/**
 * Interface para operações de negócio relacionadas a CanalVenda.
 * <p>
 * Centraliza o contrato para criação, atualização, busca e listagem de canais de venda.
 */
public interface CanalVendaService {
    /**
     * Cria um novo canal de venda.
     * Utiliza o método {@code from} da entidade para centralizar regras de negócio de criação.
     * @param requestDTO DTO de request com os dados do canal de venda
     * @return DTO de resposta do canal criado
     */
    CanalVendaResponseDTO create(CanalVendaRequestDTO requestDTO);

    /**
     * Atualiza um canal de venda existente.
     * Utiliza o método {@code updateFrom} da entidade para centralizar regras de negócio de atualização.
     * @param id ID do canal de venda
     * @param requestDTO DTO de request com os dados para atualização
     * @return DTO de resposta do canal atualizado
     */
    CanalVendaResponseDTO update(Long id, CanalVendaRequestDTO requestDTO);

    /**
     * Busca um canal de venda pelo ID.
     * @param id ID do canal de venda
     * @return DTO de resposta do canal encontrado
     */
    CanalVendaResponseDTO findById(Long id);

    /**
     * Lista todos os canais de venda.
     * @return Lista de DTOs de resposta
     */
    List<CanalVendaResponseDTO> findAll();

    /**
     * Exclui um canal de venda pelo ID.
     * @param id O ID do canal de venda a ser excluído.
     */
    void deleteById(Long id);
}
