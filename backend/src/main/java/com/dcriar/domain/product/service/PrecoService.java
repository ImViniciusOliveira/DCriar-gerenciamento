package com.dcriar.domain.product.service;

import com.dcriar.api.dto.request.product.PrecoRequestDTO;
import com.dcriar.api.dto.response.product.PrecoResponseDTO;
import java.util.List;

/**
 * Interface para operações de negócio relacionadas à entidade Preco.
 * <p>
 * Define os métodos para cadastro, atualização, consulta, listagem e exclusão de preços de produtos.
 * Todos os métodos devem centralizar regras de negócio na entidade Preco, utilizando os métodos from e updateFrom.
 * Os retornos são sempre DTOs de resposta, garantindo encapsulamento dos dados e padronização da API.
 */
public interface PrecoService {
    /**
     * Cadastra um novo preço para um produto.
     * Utiliza o método Preco.from para centralizar regras de negócio de criação.
     *
     * @param produtoId ID do produto ao qual o preço será associado
     * @param dto DTO com os dados do preço
     * @return DTO de resposta do preço cadastrado
     */
    PrecoResponseDTO create(Long produtoId, PrecoRequestDTO dto);

    /**
     * Atualiza um preço existente.
     * Utiliza o método Preco.updateFrom para centralizar regras de negócio de atualização.
     *
     * @param precoId ID do preço a ser atualizado
     * @param dto DTO com os dados para atualização
     * @return DTO de resposta do preço atualizado
     */
    PrecoResponseDTO update(Long precoId, PrecoRequestDTO dto);

    /**
     * Consulta um preço pelo ID.
     *
     * @param precoId ID do preço
     * @return DTO de resposta do preço encontrado
     */
    PrecoResponseDTO findById(Long precoId);

    /**
     * Lista todos os preços de um produto.
     *
     * @param produtoId ID do produto
     * @return Lista de DTOs de resposta dos preços do produto
     */
    List<PrecoResponseDTO> findByProduto(Long produtoId);

    /**
     * Lista todos os preços cadastrados no sistema.
     *
     * @return Lista de DTOs de resposta de todos os preços
     */
    List<PrecoResponseDTO> findAll();

    /**
     * Exclui um preço pelo ID.
     *
     * @param precoId ID do preço a ser excluído
     */
    void delete(Long precoId);
}
