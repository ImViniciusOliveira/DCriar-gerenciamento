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

    /**
     * Registra uma nova movimentação de estoque para um produto.
     * <p>
     * Este método cria um registro de transação (entrada ou saída) no histórico do produto.
     * A lógica de construção da entidade é delegada para o método {@link MovimentacaoEstoqueProduto#from(MovimentacaoEstoqueProdutoRequestDTO, Produto)}.
     * <p>
     * <b>Atenção:</b> Nenhuma validação de saldo de estoque é realizada aqui. O método apenas
     * persiste o registro da movimentação.
     *
     * @param requestDTO DTO de request com os dados da movimentação.
     * @return DTO de resposta da movimentação registrada.
     * @throws ProdutoNaoEncontradoException se o produto associado não for encontrado.
     */
    @Transactional
    public MovimentacaoEstoqueProdutoResponseDTO registrarMovimentacao(MovimentacaoEstoqueProdutoRequestDTO requestDTO) {
        Produto produto = produtoRepository.findById(requestDTO.getProdutoId())
                .orElseThrow(() -> new ProdutoNaoEncontradoException(requestDTO.getProdutoId()));
        MovimentacaoEstoqueProduto movimentacao = MovimentacaoEstoqueProduto.from(requestDTO, produto);
        movimentacaoRepository.save(movimentacao);
        return toResponseDTO(movimentacao);
    }

    /**
     * Atualiza uma movimentação de estoque existente.
     * <p>
     * A lógica de atualização dos campos da entidade é delegada para o método
     * {@link MovimentacaoEstoqueProduto#updateFrom(MovimentacaoEstoqueProdutoRequestDTO, Produto)}.
     * <p>
     * <b>Atenção:</b> Esta operação altera diretamente um registro histórico. Não há recalculo
     * ou validação de saldos subsequentes.
     *
     * @param id ID da movimentação a ser atualizada.
     * @param requestDTO DTO de request com os dados atualizados.
     * @return DTO de resposta da movimentação atualizada.
     * @throws MovimentacaoEstoqueProdutoNaoEncontradoException se a movimentação não for encontrada.
     * @throws ProdutoNaoEncontradoException se o produto associado não for encontrado.
     */
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

    /**
     * Lista todas as movimentações de estoque de um produto.
     *
     * @param produtoId ID do produto.
     * @return Lista de DTOs de resposta das movimentações.
     * @throws ProdutoNaoEncontradoException se o produto não for encontrado.
     */
    public List<MovimentacaoEstoqueProdutoResponseDTO> listarPorProduto(Long produtoId) {
        Produto produto = produtoRepository.findById(produtoId)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(produtoId));
        return movimentacaoRepository.findAllByProduto(produto).stream()
                .map(this::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Converte a entidade MovimentacaoEstoqueProduto para o DTO de resposta.
     * <p>
     * Conversão simples, sem lógica de negócio.
     *
     * @param movimentacao Entidade movimentação.
     * @return DTO de resposta.
     */
    private MovimentacaoEstoqueProdutoResponseDTO toResponseDTO(MovimentacaoEstoqueProduto movimentacao) {
        return MovimentacaoEstoqueProdutoResponseDTO.builder()
                .id(movimentacao.getId())
                .produtoId(movimentacao.getProduto().getId())
                .data(movimentacao.getData())
                .tipo(movimentacao.getTipo().name())
                .quantidade(movimentacao.getQuantidade())
                .motivo(movimentacao.getMotivo())
                .build();
    }
}
