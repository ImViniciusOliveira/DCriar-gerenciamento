package com.dcriar.domain.product.service.impl;

import com.dcriar.api.dto.request.product.MovimentacaoEstoqueProdutoRequestDTO;
import com.dcriar.api.dto.response.product.MovimentacaoEstoqueProdutoResponseDTO;
import com.dcriar.domain.product.entity.MovimentacaoEstoqueProduto;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.repository.MovimentacaoEstoqueProdutoRepository;
import com.dcriar.domain.product.repository.ProdutoRepository;
import com.dcriar.domain.product.service.MovimentacaoEstoqueProdutoService;
import com.dcriar.exception.custom.MovimentacaoEstoqueProdutoNaoEncontradoException;
import com.dcriar.exception.custom.ProdutoNaoEncontradoException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service para o registro e consulta de movimentações de estoque de produtos acabados (o "livro-razão").
 * <p>
 * Esta classe tem a responsabilidade de registrar todas as entradas e saídas do estoque mestre de um produto,
 * servindo como um histórico auditável.
 * <p>
 * <b>Importante:</b> Este serviço <em>não</em> implementa regras de negócio de validação de saldo (como
 * impedir que o estoque fique negativo). Sua função é apenas registrar os eventos de movimentação.
 * A lógica de validação de saldo é de responsabilidade de serviços de nível superior, como
 * {@link com.dcriar.domain.sales.service.VendaService} ou {@link com.dcriar.domain.product.service.EstoqueProdutoService}.
 * <p>
 * A criação e atualização das entidades são delegadas aos métodos {@code from()} e {@code updateFrom()}
 * da entidade {@link MovimentacaoEstoqueProduto} para centralizar a lógica de construção.
 */
@Service
@RequiredArgsConstructor
public class MovimentacaoEstoqueProdutoServiceImpl implements MovimentacaoEstoqueProdutoService {

    private final MovimentacaoEstoqueProdutoRepository movimentacaoRepository;
    private final ProdutoRepository produtoRepository;

    @Transactional
    public MovimentacaoEstoqueProdutoResponseDTO registrarMovimentacao(MovimentacaoEstoqueProdutoRequestDTO requestDTO) {
        Produto produto = produtoRepository.findById(requestDTO.getProdutoId())
                .orElseThrow(() -> new ProdutoNaoEncontradoException(requestDTO.getProdutoId()));
        MovimentacaoEstoqueProduto movimentacao = MovimentacaoEstoqueProduto.from(requestDTO, produto);
        movimentacaoRepository.save(movimentacao);
        return toResponseDTO(movimentacao);
    }

    @Transactional
    public MovimentacaoEstoqueProdutoResponseDTO atualizarMovimentacao(Long id, MovimentacaoEstoqueProdutoRequestDTO requestDTO) {
        MovimentacaoEstoqueProduto movimentacao = movimentacaoRepository.findById(id)
                .orElseThrow(() -> new MovimentacaoEstoqueProdutoNaoEncontradoException(id));
        Produto produto = produtoRepository.findById(requestDTO.getProdutoId())
                .orElseThrow(() -> new ProdutoNaoEncontradoException(requestDTO.getProdutoId()));
        movimentacao.updateFrom(requestDTO, produto);
        movimentacaoRepository.save(movimentacao);
        return toResponseDTO(movimentacao);
    }

    public List<MovimentacaoEstoqueProdutoResponseDTO> listarPorProduto(Long produtoId) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(produtoId));
        return movimentacaoRepository.findAllByProduto(produto).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    private MovimentacaoEstoqueProdutoResponseDTO toResponseDTO(MovimentacaoEstoqueProduto movimentacao) {
        return MovimentacaoEstoqueProdutoResponseDTO.builder()
                .id(movimentacao.getId())
                .produtoId(movimentacao.getProduto().getId())
                .data(movimentacao.getData())
                .tipo(movimentacao.getTipo())
                .quantidade(movimentacao.getQuantidade())
                .motivo(movimentacao.getMotivo())
                .build();
    }
}
