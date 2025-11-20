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

    @Override
    @Transactional
    public PrecoResponseDTO findById(Long precoId) {
        Preco preco = precoRepository.findById(precoId)
                .orElseThrow(() -> new PrecoNaoEncontradoException(precoId));
        return toResponseDTO(preco);
    }

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

    @Override
    @Transactional
    public List<PrecoResponseDTO> findAll() {
        return precoRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long precoId) {
        precoRepository.deleteById(precoId);
    }

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
