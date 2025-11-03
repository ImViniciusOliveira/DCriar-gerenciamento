package com.dcriar.domain.product.entity;

import com.dcriar.api.dto.request.product.PrecoRequestDTO;
import com.dcriar.domain.product.entity.enums.TipoPreco;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Representa o preço de um produto para um determinado tipo de precificação.
 * <p>
 * Permite que um único produto tenha múltiplos preços, como um para varejo
 * e outro para revenda.
 */
@Entity
@Table(name = "precos")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class Preco {

    /**
     * O ID único do preço.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * O produto ao qual este preço está associado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    /**
     * O tipo de preço (ex: VAREJO, ATACADO).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_preco", nullable = false, length = 50)
    private TipoPreco tipoPreco;

    /**
     * O valor base do preço do produto.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    /**
     * O valor promocional do produto, se houver uma promoção ativa.
     */
    @Column(name = "valor_promocional", precision = 19, scale = 2)
    private BigDecimal valorPromocional;

    /**
     * Indica se a promoção para este preço está ativa.
     */
    @Column(name = "promocao_ativa", nullable = false)
    private boolean promocaoAtiva;

    /**
     * Cria uma instância de Preco a partir de um PrecoRequestDTO, centralizando regras de negócio de criação.
     *
     * @param dto     DTO de request com os dados do preço
     * @param produto Produto associado ao preço
     * @return Instância de Preco
     */
    public static Preco from(PrecoRequestDTO dto, Produto produto) {
        return Preco.builder()
                .produto(produto)
                .tipoPreco(com.dcriar.domain.product.entity.enums.TipoPreco.valueOf(dto.getTipoPreco()))
                .valor(dto.getValor())
                .valorPromocional(dto.getValorPromocional())
                .promocaoAtiva(Boolean.TRUE.equals(dto.getPromocaoAtiva()))
                .build();
    }

    /**
     * Atualiza os campos da instância de Preco a partir de um PrecoRequestDTO, centralizando regras de negócio de
     * atualização.
     *
     * @param dto     DTO de request com os dados do preço
     * @param produto Produto associado ao preço
     */
    public void updateFrom(PrecoRequestDTO dto, Produto produto) {
        this.produto = produto;
        this.tipoPreco = com.dcriar.domain.product.entity.enums.TipoPreco.valueOf(dto.getTipoPreco());
        this.valor = dto.getValor();
        this.valorPromocional = dto.getValorPromocional();
        this.promocaoAtiva = Boolean.TRUE.equals(dto.getPromocaoAtiva());
    }
}
