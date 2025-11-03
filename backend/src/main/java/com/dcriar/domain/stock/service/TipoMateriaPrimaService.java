package com.dcriar.domain.stock.service;

import com.dcriar.api.dto.request.stock.TipoMateriaPrimaRequestDTO;
import com.dcriar.api.dto.response.stock.TipoMateriaPrimaResponseDTO;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Interface que define o contrato para a lógica de negócio de gerenciamento de Tipos de Matéria-Prima.
 * <p>
 * Abstrai as operações de CRUD e outras regras de negócio, desacoplando o controller da implementação.
 */
public interface TipoMateriaPrimaService {

    /**
     * Retorna uma lista paginada de todos os tipos de matéria-prima, com possibilidade de filtro.
     *
     * @param nome              Filtro para buscar tipos de matéria-prima por nome (busca parcial, case-insensitive).
     * @param unidadeDeConsumo  Filtro para buscar tipos de matéria-prima por unidade de consumo.
     * @param pageable          Informações de paginação e ordenação.
     * @return Uma página ({@link Page}) de {@link TipoMateriaPrimaResponseDTO}.
     */
    Page<TipoMateriaPrimaResponseDTO> findAll(String nome, UnidadeDeMedida unidadeDeConsumo, Pageable pageable);

    /**
     * Busca um tipo de matéria-prima pelo seu ID.
     *
     * @param id O ID do tipo de matéria-prima.
     * @return O {@link TipoMateriaPrimaResponseDTO} correspondente.
     */
    TipoMateriaPrimaResponseDTO findById(Long id);

    /**
     * Cria um novo tipo de matéria-prima.
     *
     * @param requestDTO O DTO com os dados para a criação.
     * @return O {@link TipoMateriaPrimaResponseDTO} do tipo recém-criado.
     */
    TipoMateriaPrimaResponseDTO create(TipoMateriaPrimaRequestDTO requestDTO);

    /**
     * Atualiza um tipo de matéria-prima existente.
     *
     * @param id O ID do tipo a ser atualizado.
     * @param requestDTO O DTO com os novos dados.
     * @return O {@link TipoMateriaPrimaResponseDTO} do tipo atualizado.
     */
    TipoMateriaPrimaResponseDTO update(Long id, TipoMateriaPrimaRequestDTO requestDTO);

    /**
     * Deleta um tipo de matéria-prima pelo seu ID.
     *
     * @param id O ID do tipo a ser deletado.
     */
    void deleteById(Long id);

}
