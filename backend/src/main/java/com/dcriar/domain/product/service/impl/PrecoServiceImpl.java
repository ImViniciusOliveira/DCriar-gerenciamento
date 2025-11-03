package com.dcriar.domain.product.service.impl;

import com.dcriar.api.dto.request.product.PrecoRequestDTO;
import com.dcriar.api.dto.response.product.PrecoResponseDTO;
import com.dcriar.domain.product.entity.Preco;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.repository.PrecoRepository;
import com.dcriar.domain.product.repository.ProdutoRepository;
import com.dcriar.domain.product.service.PrecoService;
import com.dcriar.exception.custom.PrecoNaoEncontradoException;
import com.dcriar.exception.custom.ProdutoNaoEncontradoException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementação das operações de negócio para a gestão de Preços.
 * <p>
 * Este serviço gerencia o ciclo de vida das entidades {@link Preco}, garantindo a aplicação
 * de regras de negócio, como a unicidade de um tipo de preço por produto.
 * <p>
 * A lógica de criação e atualização é centralizada nos métodos {@code from} e {@code updateFrom} da entidade Preco.
 */
@Service
@RequiredArgsConstructor
public class PrecoServiceImpl implements PrecoService {

    private final PrecoRepository precoRepository;
    private final ProdutoRepository produtoRepository;

    /**
     * Cria um novo preço para um produto.
     * <p>
     * <b>Regras de negócio:</b>
     * <ul>
     *     <li>Um produto não pode ter mais de um preço com o mesmo {@code TipoPreco}.
     *     Esta regra é garantida por uma restrição a nível de banco de dados.</li>
     * </ul>
     * A lógica de construção da entidade é delegada ao método {@link Preco#from(PrecoRequestDTO, Produto)}.
     *
     * @param produtoId O ID do produto ao qual o preço será associado.
     * @param dto O DTO com os dados do novo preço.
     * @return O DTO de resposta do preço criado.
     * @throws ProdutoNaoEncontradoException se o produto não for encontrado.
     * @throws DataIntegrityViolationException se a regra de unicidade (produto/tipo de preço) for violada.
     */
    @Override
    @Transactional
    public PrecoResponseDTO create(Long produtoId, PrecoRequestDTO dto) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(produtoId));
        // A validação de preço duplicado é delegada a uma constraint do banco de dados
        // para garantir atomicidade e evitar condições de corrida (race conditions).
        Preco preco = Preco.from(dto, produto);
        Preco salvo = precoRepository.save(preco);
        return toResponseDTO(salvo);
    }

    /**
     * Atualiza um preço existente.
     * <p>
     * <b>Regras de negócio:</b>
     * <ul>
     *     <li>Ao alterar, a combinação de produto e {@code TipoPreco} deve permanecer única.</li>
     * </ul>
     * A lógica de atualização é delegada ao método {@link Preco#updateFrom(PrecoRequestDTO, Produto)}.
     *
     * @param precoId O ID do preço a ser atualizado.
     * @param dto O DTO com os dados para atualização.
     * @return O DTO de resposta do preço atualizado.
     * @throws PrecoNaoEncontradoException se o preço não for encontrado.
     * @throws ProdutoNaoEncontradoException se o produto associado não for encontrado.
     * @throws DataIntegrityViolationException se a regra de unicidade (produto/tipo de preço) for violada.
     */
    @Override
    @Transactional
    public PrecoResponseDTO update(Long precoId, PrecoRequestDTO dto) {
        Preco preco = precoRepository.findById(precoId)
                .orElseThrow(() -> new PrecoNaoEncontradoException(precoId));
        Produto produto = produtoRepository.findById(dto.getProdutoId())
                .orElseThrow(() -> new ProdutoNaoEncontradoException(dto.getProdutoId()));
        preco.updateFrom(dto, produto);
        Preco atualizado = precoRepository.save(preco);
        return toResponseDTO(atualizado);
    }

    /**
     * Busca um preço pelo seu ID.
     *
     * @param precoId O ID do preço a ser buscado.
     * @return O DTO de resposta do preço encontrado.
     * @throws PrecoNaoEncontradoException se o preço não for encontrado.
     */
    @Override
    @Transactional
    public PrecoResponseDTO findById(Long precoId) {
        Preco preco = precoRepository.findById(precoId)
                .orElseThrow(() -> new PrecoNaoEncontradoException(precoId));
        return toResponseDTO(preco);
    }

    /**
     * Busca todos os preços associados a um produto específico.
     *
     * @param produtoId O ID do produto.
     * @return Uma lista de DTOs de resposta dos preços encontrados.
     * @throws ProdutoNaoEncontradoException se o produto não for encontrado.
     */
    @Override
    @Transactional
    public List<PrecoResponseDTO> findByProduto(Long produtoId) {
        Produto produto = produtoRepository.findById(produtoId)
            .orElseThrow(() -> new ProdutoNaoEncontradoException(produtoId));
        return precoRepository.findByProduto(produto)
            .stream()
            .map(this::toResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Lista todos os preços cadastrados no sistema.
     *
     * @return Uma lista de DTOs de resposta de todos os preços.
     */
    @Override
    @Transactional
    public List<PrecoResponseDTO> findAll() {
        return precoRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Exclui um preço pelo seu ID.
     * <p>
     * <b>Atenção:</b> Esta é uma operação de exclusão física (hard delete).
     * Nenhuma verificação é feita para saber se o preço está em uso. A exclusão
     * de um preço de varejo, por exemplo, pode impedir novas vendas daquele produto
     * até que um novo preço seja definido.
     *
     * @param precoId O ID do preço a ser excluído.
     */
    @Override
    @Transactional
    public void delete(Long precoId) {
        precoRepository.deleteById(precoId);
    }

    /**
     * Converte a entidade Preco para o DTO de resposta.
     * <p>
     * Realiza apenas mapeamento simples dos campos, sem lógica de negócio.
     *
     * @param preco Entidade Preco.
     * @return DTO de resposta.
     */
    private PrecoResponseDTO toResponseDTO(Preco preco) {
        return PrecoResponseDTO.builder()
                .id(preco.getId())
                .produtoId(preco.getProduto().getId())
                .tipoPreco(preco.getTipoPreco().name())
                .valor(preco.getValor())
                .valorPromocional(preco.getValorPromocional())
                .promocaoAtiva(preco.isPromocaoAtiva())
                .build();
    }
}
