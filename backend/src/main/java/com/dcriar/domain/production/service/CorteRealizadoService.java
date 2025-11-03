package com.dcriar.domain.production.service;

import com.dcriar.api.dto.request.production.CorteRealizadoRequestDTO;
import com.dcriar.api.dto.response.production.CorteRealizadoResponseDTO;
import java.util.List;

/**
 * Interface para operações de negócio relacionadas à entidade CorteRealizado.
 * <p>
 * Define métodos para cadastro, atualização, consulta, listagem e exclusão de cortes realizados.
 * Todos os métodos devem centralizar regras de negócio na entidade CorteRealizado, utilizando os métodos from e updateFrom.
 * Os retornos são sempre DTOs de resposta, garantindo encapsulamento dos dados e padronização da API.
 */
public interface CorteRealizadoService {
    /**
     * Cadastra um novo corte realizado.
     *
     * @param dto DTO com os dados do corte realizado
     * @return DTO de resposta do corte realizado cadastrado
     */
    CorteRealizadoResponseDTO create(CorteRealizadoRequestDTO dto);

    /**
     * Atualiza um corte realizado existente.
     *
     * @param id  ID do corte realizado
     * @param dto DTO com os dados para atualização
     * @return DTO de resposta do corte realizado atualizado
     */
    CorteRealizadoResponseDTO update(Long id, CorteRealizadoRequestDTO dto);

    /**
     * Consulta um corte realizado pelo ID.
     *
     * @param id ID do corte realizado
     * @return DTO de resposta do corte realizado encontrado
     */
    CorteRealizadoResponseDTO findById(Long id);

    /**
     * Lista todos os cortes realizados de uma ordem de produção.
     *
     * @param ordemDeProducaoId ID da ordem de produção
     * @return Lista de DTOs de resposta dos cortes realizados
     */
    List<CorteRealizadoResponseDTO> findByOrdemDeProducao(Long ordemDeProducaoId);

    /**
     * Lista todos os cortes realizados cadastrados no sistema.
     *
     * @return Lista de DTOs de resposta de todos os cortes realizados
     */
    List<CorteRealizadoResponseDTO> findAll();

    /**
     * Exclui um corte realizado pelo ID.
     *
     * @param id ID do corte realizado a ser excluído
     */
    void delete(Long id);
}
