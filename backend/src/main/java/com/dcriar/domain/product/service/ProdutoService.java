package com.dcriar.domain.product.service;

import com.dcriar.api.dto.request.product.ProdutoRequestDTO;
import com.dcriar.api.dto.response.product.ProdutoResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * Interface que define o contrato para a lógica de negócio de Produtos.
 * Desacopla o controller da implementação do serviço, permitindo maior flexibilidade
 * e facilitando os testes.
 */
public interface ProdutoService {

    /**
     * Busca todos os produtos cadastrados de forma paginada, com opção de filtro por nome/SKU.
     *
     * @param nome Termo de busca para filtrar por nome ou SKU (opcional).
     * @param pageable Objeto com as informações de paginação (página, tamanho, ordenação).
     * @return Uma página de DTOs de resposta de produtos.
     */
    Page<ProdutoResponseDTO> findAll(String nome, Pageable pageable);

    /**
     * Busca um produto específico pelo seu ID.
     *
     * @param id O ID do produto a ser buscado.
     * @return O DTO de resposta do produto encontrado.
     */
    ProdutoResponseDTO findById(Long id);

    /**
     * Cria um novo produto no sistema.
     *
     * @param requestDTO O DTO com os dados para a criação.
     * @return O DTO de resposta do produto recém-criado.
     */
    ProdutoResponseDTO create(ProdutoRequestDTO requestDTO);

    /**
     * Atualiza um produto existente a partir de um DTO completo.
     *
     * @param id O ID do produto a ser atualizado.
     * @param requestDTO O DTO com os novos dados.
     * @return O DTO de resposta do produto atualizado.
     */
    ProdutoResponseDTO update(Long id, ProdutoRequestDTO requestDTO);

    /**
     * Atualiza parcialmente um produto existente a partir de um mapa de campos.
     *
     * @param id O ID do produto a ser atualizado.
     * @param fields Um mapa contendo os nomes dos campos e seus novos valores.
     * @return O DTO de resposta do produto atualizado.
     */
    ProdutoResponseDTO patch(Long id, Map<String, Object> fields);

    /**
     * Deleta um produto pelo seu ID.
     *
     * @param id O ID do produto a ser deletado.
     */
    void deleteById(Long id);

    /**
     * Busca produtos filtrando por tipo e estoque.
     * Utilizado pelo endpoint /by-tipo para Production.
     *
     * @param tipoProduto Tipo do produto ("CORTE" ou "CONSUMO_DIRETO").
     * @param estoqueValor Valor de estoque para comparação.
     * @param estoqueOperador "GTE" para ≥ ou "LTE" para ≤.
     * @param nome Termo de busca para filtrar por nome ou SKU (opcional).
     * @param pageable Objeto com as informações de paginação.
     * @return Uma página de DTOs de resposta de produtos.
     */
    Page<ProdutoResponseDTO> findByTipoAndEstoque(
            String tipoProduto,
            Integer estoqueValor,
            String estoqueOperador,
            String nome,
            Pageable pageable
    );

    /**
     * Realiza o upload de uma foto para um produto específico, associando-a a ele.
     * Este método orquestra o armazenamento do arquivo e a atualização da entidade Produto.
     *
     * @param produtoId O ID do produto ao qual a foto será associada.
     * @param file O arquivo de imagem a ser enviado.
     * @return O DTO de resposta do produto atualizado com a nova foto.
     */
    ProdutoResponseDTO uploadFoto(Long produtoId, MultipartFile file);
}
