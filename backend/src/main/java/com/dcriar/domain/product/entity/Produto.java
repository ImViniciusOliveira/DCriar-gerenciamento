package com.dcriar.domain.product.entity;

import com.dcriar.api.dto.request.product.ProdutoRequestDTO;
import com.dcriar.domain.stock.entity.TipoMateriaPrima;
import jakarta.persistence.*;
import lombok.*;

/**
 * Representa um produto final (o "molde") no sistema da DCriar.
 * <p>
 * Esta entidade armazena as características e especificações de um produto
 * que pode ser produzido e vendido, incluindo suas dimensões, o tipo de
 * matéria-prima principal que utiliza e informações de marketing.
 */
@Entity
@Table(name = "produtos")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(of = "id")
public class Produto {

    /**
     * O ID único do produto.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * O nome comercial do produto. Deve ser único.
     */
    @Column(nullable = false, unique = true, length = 150)
    private String nome;

    /**
     * O código único de produto (SKU - Stock Keeping Unit). Deve ser único.
     */
    @Column(nullable = false, unique = true, length = 50)
    private String sku;

    /**
     * A descrição detalhada sobre o produto, seu material e uso.
     */
    @Column(columnDefinition = "TEXT")
    private String descricao;

    /**
     * A cor principal do produto.
     */
    @Column(nullable = false, length = 50)
    private String cor;

    /**
     * A quantidade de itens que compõem uma unidade do produto vendido (ex: 100 etiquetas por pacote).
     */
    @Column(nullable = false)
    private Integer unidadesPorProduto;

    /**
     * Indica se o produto está ativo e disponível para venda e produção.
     */
    @Column(nullable = false)
    private boolean ativo;

    /**
     * A URL da imagem principal do produto para exibição no catálogo.
     */
    @Column(name = "foto_principal_url")
    private String fotoPrincipalUrl;

    /**
     * Ligação direta ao tipo de matéria-prima que este produto consome.
     * <p>
     * Simplifica o modelo, assumindo que cada produto é feito de um material principal.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_materia_prima_id")
    private TipoMateriaPrima tipoMateriaPrima;

    /**
     * As dimensões de uma única unidade deste produto (o "molde").
     * <p>
     * Armazenado como um objeto embutido {@link Dimensoes}.
     */
    @Embedded
    private Dimensoes dimensoes;

    /**
     * Método de fábrica estático para criar uma nova instância de {@link Produto} a partir de um {@link ProdutoRequestDTO}.
     * <p>
     * A responsabilidade de buscar e definir o {@link TipoMateriaPrima} é do serviço.
     *
     * @param request O DTO de requisição contendo os dados do novo produto.
     * @return Uma nova instância de {@link Produto}.
     */
    public static Produto from(ProdutoRequestDTO request) {
        Dimensoes dimensoesObj = null;
        if (request.getDimensoes() != null) {
            dimensoesObj = Dimensoes.builder()
                    .larguraCm(request.getDimensoes().getLarguraCm())
                    .comprimentoCm(request.getDimensoes().getComprimentoCm())
                    .build();
        }

        return Produto.builder()
                .nome(request.getNome())
                .sku(request.getSku())
                .descricao(request.getDescricao())
                .cor(request.getCor())
                .unidadesPorProduto(request.getUnidadesPorProduto())
                .fotoPrincipalUrl(request.getFotoPrincipalUrl())
                .ativo(request.getAtivo() != null ? request.getAtivo() : false)
                .dimensoes(dimensoesObj)
                .build();
    }

    /**
     * Atualiza os dados do produto a partir de um {@link ProdutoRequestDTO}.
     * <p>
     * Este método permite a atualização de campos como nome, SKU, descrição, cor,
     * unidades por produto, URL da foto principal, status de ativo e dimensões.
     * A atualização do {@link TipoMateriaPrima} deve ser gerenciada separadamente pelo serviço.
     *
     * @param request O DTO de requisição contendo os novos dados para atualização do produto.
     */
    public void updateFrom(ProdutoRequestDTO request) {
        this.nome = request.getNome();
        this.sku = request.getSku();
        this.descricao = request.getDescricao();
        this.cor = request.getCor();
        this.unidadesPorProduto = request.getUnidadesPorProduto();
        this.fotoPrincipalUrl = request.getFotoPrincipalUrl();
        this.ativo = request.getAtivo() != null ? request.getAtivo() : this.ativo;
        if (request.getDimensoes() != null) {
            this.dimensoes = Dimensoes.builder()
                    .larguraCm(request.getDimensoes().getLarguraCm())
                    .comprimentoCm(request.getDimensoes().getComprimentoCm())
                    .build();
        }
    }
}
