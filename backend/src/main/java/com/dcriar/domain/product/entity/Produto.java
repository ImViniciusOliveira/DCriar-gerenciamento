package com.dcriar.domain.product.entity;

import com.dcriar.domain.common.entity.AuditableEntity;
import com.dcriar.domain.stock.entity.TipoMateriaPrima;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.Formula;

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

    @Column(columnDefinition = "TEXT")
    private String descricao;

    @Column(nullable = false)
    private Integer unidadesPorProduto;

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
}
