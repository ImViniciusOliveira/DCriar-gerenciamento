package com.dcriar.domain.product.entity;

import com.dcriar.api.dto.request.product.MovimentacaoEstoqueProdutoRequestDTO;
import com.dcriar.domain.production.entity.OrdemDeProducao;
import com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

/**
 * Entidade que representa um único registro no "Livro-Razão" do estoque de produtos acabados.
 * <p>
 * Cada instância desta classe é um evento imutável que descreve uma mudança
 * na quantidade de um Produto específico. A soma de todas as
 * movimentações de um produto resulta no seu saldo físico total.
 */
@Entity
@Table(name = "movimentacoes_estoque_produto")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
public class MovimentacaoEstoqueProduto {

    /**
     * O ID único da movimentação de estoque do produto.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * O produto ao qual esta movimentação está associada.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    /**
     * A ordem de produção que originou esta movimentação (se aplicável).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_producao_id")
    private OrdemDeProducao ordemDeProducao;

    /**
     * A data e hora em que a movimentação foi registrada.
     * Gerado automaticamente no momento da criação.
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime data;

    /**
     * O tipo da movimentação (ex: ENTRADA_PRODUCAO, SAIDA_VENDA).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private TipoMovimentacaoProduto tipo;

    /**
     * Quantidade movimentada. Positiva para entradas, negativa para saídas.
     */
    @Column(nullable = false)
    private Integer quantidade;

    /**
     * O motivo ou observação registrado para a movimentação.
     */
    @Column(length = 254)
    private String motivo;

    /**
     * Cria uma instância de MovimentacaoEstoqueProduto a partir do DTO de request, centralizando regras de negócio de criação.
     * <p>
     * Este método deve ser utilizado pela service para garantir consistência na criação de registros.
     *
     * @param dto     DTO de request contendo os dados para criação
     * @param produto Produto já carregado da base
     * @return Nova instância de MovimentacaoEstoqueProduto
     */
    public static MovimentacaoEstoqueProduto from(MovimentacaoEstoqueProdutoRequestDTO dto, Produto produto) {
        return MovimentacaoEstoqueProduto.builder()
                .produto(produto)
                .tipo(TipoMovimentacaoProduto.valueOf(dto.getTipo()))
                .quantidade(dto.getQuantidade())
                .motivo(dto.getMotivo())
                .build();
    }

    /**
     * Atualiza os campos da movimentação a partir do DTO de request, centralizando regras de negócio de atualização.
     * <p>
     * Este método deve ser utilizado pela service para garantir consistência na atualização de registros.
     *
     * @param dto     DTO de request contendo os dados para atualização
     * @param produto Produto já carregado da base
     */
    public void updateFrom(MovimentacaoEstoqueProdutoRequestDTO dto, Produto produto) {
        this.produto = produto;
        this.tipo = TipoMovimentacaoProduto.valueOf(dto.getTipo());
        this.quantidade = dto.getQuantidade();
        this.motivo = dto.getMotivo();
    }
}
