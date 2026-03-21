package com.dcriar.domain.sales.entity;

import com.dcriar.api.dto.request.sales.ItemVendaRequestDTO;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.sales.entity.enums.TipoPrecoAplicado;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Representa um item individual dentro de uma Venda.
 * <p>
 * Cada instância desta classe é uma "linha do recibo", detalhando qual produto
 * foi vendido, a quantidade e o preço unitário pago no momento da transação.
 */
@Entity
@Table(name = "itens_venda")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EqualsAndHashCode(of = "id")
public class ItemVenda {

    /**
     * O ID único do item da venda.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * A venda à qual este item pertence.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venda_id", nullable = false)
    private Venda venda;

    /**
     * O produto que foi vendido neste item.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    /**
     * A quantidade de unidades do produto vendidas.
     */
    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "preco_comercial_original", nullable = false, precision = 19, scale = 2)
    private BigDecimal precoComercialOriginal;

    /**
     * O preço unitário do produto no momento da venda.
     * É crucial armazenar este valor para garantir a integridade histórica
     * dos dados financeiros, mesmo que o preço do produto mude no futuro.
     */
    @Column(name = "preco_unitario", nullable = false, precision = 19, scale = 4)
    private BigDecimal precoUnitario;

    /**
     * O preço total para este item (quantidade * preço unitário).
     * Armazenado para facilitar consultas e relatórios.
     */
    @Column(name = "preco_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal precoTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_preco_aplicado", nullable = false, length = 30)
    private TipoPrecoAplicado tipoPrecoAplicado;

    @Column(name = "motivo_alteracao_preco", length = 255)
    private String motivoAlteracaoPreco;

    /**
     * Cria uma instância de ItemVenda a partir do DTO de request e do Produto resolvido.
     * <p>
     * Este método centraliza regras de negócio de criação do item, como cálculo do preço total.
     * Deve ser utilizado pela service para garantir consistência.
     *
     * @param dto DTO de request do item
     * @param produto Produto resolvido
     * @param precoComercialOriginal Preço comercial padrão do produto no momento da venda
     * @param precoUnitario Preço unitário efetivamente aplicado
     * @param tipoPrecoAplicado Como o preço foi aplicado na venda
     * @return ItemVenda criado
     */
    public static ItemVenda from(
            ItemVendaRequestDTO dto,
            Produto produto,
            BigDecimal precoComercialOriginal,
            BigDecimal precoUnitario,
            BigDecimal precoTotal,
            TipoPrecoAplicado tipoPrecoAplicado
    ) {
        return ItemVenda.builder()
                .produto(produto)
                .quantidade(dto.getQuantidade())
                .precoComercialOriginal(precoComercialOriginal)
                .precoUnitario(precoUnitario)
                .precoTotal(precoTotal)
                .tipoPrecoAplicado(tipoPrecoAplicado)
                .motivoAlteracaoPreco(dto.getMotivoAlteracaoPreco())
                .build();
    }

    /**
     * Atualiza os campos do ItemVenda existente a partir do DTO de request e do Produto resolvido.
     * <p>
     * Este método centraliza regras de negócio de atualização do item, como recálculo do preço total.
     * Deve ser utilizado pela service para garantir consistência.
     *
     * @param dto DTO de request do item
     * @param produto Produto resolvido
     * @param precoComercialOriginal Preço comercial padrão do produto no momento da venda
     * @param precoUnitario Preço unitário efetivamente aplicado
     * @param tipoPrecoAplicado Como o preço foi aplicado na venda
     */
    public void updateFrom(
            ItemVendaRequestDTO dto,
            Produto produto,
            BigDecimal precoComercialOriginal,
            BigDecimal precoUnitario,
            BigDecimal precoTotal,
            TipoPrecoAplicado tipoPrecoAplicado
    ) {
        this.produto = produto;
        this.quantidade = dto.getQuantidade();
        this.precoComercialOriginal = precoComercialOriginal;
        this.precoUnitario = precoUnitario;
        this.precoTotal = precoTotal;
        this.tipoPrecoAplicado = tipoPrecoAplicado;
        this.motivoAlteracaoPreco = dto.getMotivoAlteracaoPreco();
    }
}
