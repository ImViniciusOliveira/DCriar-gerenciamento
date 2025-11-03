package com.dcriar.domain.product.entity;

import com.dcriar.api.dto.request.product.EstoqueRequestDTO;
import jakarta.persistence.*;
import lombok.*;

/**
 * Representa o estoque de um produto acabado em um canal de venda específico.
 * <p>
 * Esta entidade liga um Produto a um Canal de Venda e armazena a quantidade
 * disponível nesse canal.
 */
@Entity
@Table(name = "estoques")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class Estoque {

    /**
     * O ID único do registro de estoque.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * O produto associado a este registro de estoque.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    /**
     * O canal de venda onde este estoque está alocado.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canal_venda_id", nullable = false)
    private CanalVenda canalVenda;

    /**
     * A quantidade de unidades do produto disponíveis neste canal de venda.
     */
    @Column(nullable = false)
    private Integer quantidade;

    /**
     * Cria uma instância de Estoque a partir do DTO de request, centralizando regras de negócio de criação.
     * <p>
     * Este método deve ser utilizado pela service para garantir consistência na criação de registros.
     *
     * @param dto        DTO de request contendo os dados para criação
     * @param produto    Produto já carregado da base
     * @param canalVenda Canal de venda já carregado da base
     * @return Nova instância de Estoque
     */
    public static Estoque from(EstoqueRequestDTO dto, Produto produto, CanalVenda canalVenda) {
        return Estoque.builder()
                .produto(produto)
                .canalVenda(canalVenda)
                .quantidade(dto.getQuantidade())
                .build();
    }

    /**
     * Atualiza os campos da entidade Estoque a partir do DTO de request, centralizando regras de negócio de atualização.
     * <p>
     * Este método deve ser utilizado pela service para garantir consistência na atualização de registros.
     *
     * @param dto        DTO de request contendo os dados para atualização
     * @param produto    Produto já carregado da base
     * @param canalVenda Canal de venda já carregado da base
     */
    public void updateFrom(EstoqueRequestDTO dto, Produto produto, CanalVenda canalVenda) {
        this.produto = produto;
        this.canalVenda = canalVenda;
        this.quantidade = dto.getQuantidade();
    }
}
