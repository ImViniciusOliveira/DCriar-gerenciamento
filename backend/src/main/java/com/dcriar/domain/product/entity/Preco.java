package com.dcriar.domain.product.entity;

import com.dcriar.domain.common.entity.AuditableEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Representa o preço comercial único de um produto.
 */
@Entity
@Table(name = "precos")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
@EqualsAndHashCode(of = "id", callSuper = false)
public class Preco extends AuditableEntity {

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
     * O valor base do preço do produto.
     */
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    /**
     * Cria uma instância de {@link Preco} a partir do valor comercial informado e do produto associado.
     * <p>
     * Mantém o padrão do projeto de centralizar na própria entidade a criação do estado persistente,
     * deixando a camada de serviço responsável por resolver dependências e orquestrar o fluxo.
     *
     * @param valor   Preço comercial do produto
     * @param produto Produto associado ao preço
     * @return Nova instância de {@link Preco}
     */
    public static Preco from(BigDecimal valor, Produto produto) {
        return Preco.builder()
                .produto(produto)
                .valor(valor)
                .build();
    }

    /**
     * Atualiza os dados do preço comercial associado ao produto.
     * <p>
     * Mantém a mesma convenção adotada pelas demais entidades do projeto para encapsular mutações
     * simples dentro do domínio.
     *
     * @param valor   Novo preço comercial do produto
     * @param produto Produto associado ao preço
     */
    public void updateFrom(BigDecimal valor, Produto produto) {
        this.produto = produto;
        this.valor = valor;
    }
}
