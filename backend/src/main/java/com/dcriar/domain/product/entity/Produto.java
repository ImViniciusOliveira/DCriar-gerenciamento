package com.dcriar.domain.product.entity;

import com.dcriar.domain.common.entity.AuditableEntity;
import com.dcriar.domain.stock.entity.TipoMateriaPrima;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;

@Entity
@Table(name = "produtos")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_produto", discriminatorType = DiscriminatorType.STRING)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@SuperBuilder(toBuilder = true)
@ToString
@EqualsAndHashCode(of = "id", callSuper = false)
public abstract class Produto extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 150)
    private String nome;

    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    /**
     * Valor do preço comercial atual do produto, carregado diretamente do banco via subconsulta.
     * <p>
     * Este campo é somente leitura e existe para permitir listagem e ordenação server-side
     * sem depender de enriquecimento posterior na camada de serviço.
     */
    @Formula("(SELECT pr.valor FROM precos pr WHERE pr.produto_id = id)")
    private BigDecimal precoComercial;

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false, precision = 14, scale = 4)
    private BigDecimal unidadesPorProduto;

    @Column(nullable = false)
    private boolean ativo;

    @Column(name = "foto_principal_url")
    private String fotoPrincipalUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_materia_prima_id")
    private TipoMateriaPrima tipoMateriaPrima;

    @Formula("(SELECT COALESCE(SUM(CASE WHEN mep.tipo LIKE 'ENTRADA%' THEN mep.quantidade ELSE -mep.quantidade END), 0) " +
             "FROM movimentacoes_estoque_produto mep WHERE mep.produto_id = id)")
    private Integer estoqueFisicoTotal;

    /**
     * Total atualmente distribuído nos canais de venda.
     * <p>
     * Campo somente leitura usado para listagem e ordenação server-side.
     */
    @Formula("(SELECT COALESCE(SUM(e.quantidade), 0) FROM estoques e WHERE e.produto_id = id)")
    private Integer estoqueDistribuidoTotal;

    /**
     * Saldo ainda disponível para alocação após descontar o total já distribuído.
     * <p>
     * Campo somente leitura usado para listagem e ordenação server-side.
     */
    @Formula("(" +
            "(SELECT COALESCE(SUM(CASE WHEN mep.tipo LIKE 'ENTRADA%' THEN mep.quantidade ELSE -mep.quantidade END), 0) " +
            " FROM movimentacoes_estoque_produto mep WHERE mep.produto_id = id)" +
            " - " +
            "(SELECT COALESCE(SUM(e.quantidade), 0) FROM estoques e WHERE e.produto_id = id)" +
            ")")
    private Integer estoqueDisponivelParaAlocar;

    /**
     * Tipo persistido do produto conforme o discriminator da hierarquia.
     * <p>
     * Exposto como campo somente leitura para permitir ordenação server-side de recursos
     * que dependem do tipo do produto, como ordens de produção.
     */
    @Formula("tipo_produto")
    private String tipoProdutoPersistido;

    /**
     * Retorna o tipo funcional do produto para uso em mapeamentos e regras de negócio.
     */
    public abstract String getTipoProduto();
}
