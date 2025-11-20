package com.dcriar.api.dto.response.product;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Data Transfer Object (DTO) que representa a resposta completa de um Produto.
 * <p>
 * Este DTO fornece uma visão detalhada de um produto, incluindo suas informações básicas,
 * status, totais de estoque e dimensões.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProdutoResponseDTO {

    /**
     * O ID único do produto.
     */
    @Schema(description = "ID único do produto.", example = "1")
    private Long id;

    /**
     * O nome comercial do produto.
     */
    @Schema(description = "Nome do produto.", example = "Etiqueta Adesiva Redonda 5x5cm Kraft")
    private String nome;

    /**
     * O código único de produto (SKU - Stock Keeping Unit).
     */
    @Schema(description = "Código único de produto (SKU).", example = "ETQ-KRAFT-RD5")
    private String sku;

    /**
     * A descrição detalhada do produto, exibida em catálogos e páginas de detalhes.
     */
    @Schema(description = "Descrição detalhada do produto.", example = "Etiqueta adesiva redonda de papel kraft 5x5cm")
    private String descricao;

    /**
     * A cor principal do produto.
     */
    @Schema(description = "Cor principal do produto.", example = "Marrom")
    private String cor;

    /**
     * A quantidade de itens individuais que compõem uma unidade de venda do produto.
     * <p>
     * Por exemplo, um pacote de etiquetas pode conter 100 unidades.
     */
    @Schema(description = "Quantidade de unidades por produto vendido.", example = "100")
    private Integer unidadesPorProduto;

    /**
     * Indica se o produto está ativo e disponível para venda.
     */
    @Schema(description = "Indica se o produto está ativo para venda.", example = "true")
    private boolean ativo;

    /**
     * A URL da imagem principal do produto.
     */
    @Schema(description = "URL da imagem principal do produto.")
    private String fotoPrincipalUrl;

    /**
     * A matéria-prima principal utilizada no produto.
     */
    @Schema(description = "A matéria-prima principal utilizada no produto.")
    private MateriaPrimaResponseDTO materiaPrima;

    /**
     * A quantidade total de unidades do produto no estoque físico (Estoque Mestre).
     * <p>
     * Representa o total de unidades produzidas e disponíveis.
     */
    @Schema(description = "Quantidade total em estoque (Estoque Mestre).", example = "150")
    private Integer estoqueFisicoTotal;

    /**
     * A quantidade total de unidades do produto que já foram distribuídas
     * entre os diferentes canais de venda.
     */
    @Schema(description = "Quantidade distribuída pelos canais de venda.", example = "100")
    private Integer estoqueDistribuidoTotal;

    /**
     * O saldo de unidades do produto que ainda estão disponíveis no Estoque Mestre
     * e podem ser alocadas para os canais de venda.
     * <p>
     * Calculado como: {@code estoqueFisicoTotal - estoqueDistribuidoTotal}.
     */
    @Schema(description = "Saldo de unidades disponíveis para alocação em canais de venda.", example = "50")
    private Integer estoqueDisponivelParaAlocar;

    /**
     * As dimensões unitárias do produto (largura, altura, etc.).
     */
    @Schema(description = "Dimensões unitárias do produto.")
    private DimensoesResponseDTO dimensoes;

    /**
     * A data e hora em que o produto foi criado.
     */
    @Schema(description = "Data e hora de criação do produto.")
    private LocalDateTime dataCriacao;

    /**
     * A data e hora da última atualização do produto.
     */
    @Schema(description = "Data e hora da última atualização do produto.")
    private LocalDateTime dataAtualizacao;
}
