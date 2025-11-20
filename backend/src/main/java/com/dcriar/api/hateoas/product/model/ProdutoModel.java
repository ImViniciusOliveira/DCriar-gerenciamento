package com.dcriar.api.hateoas.product.model;

import com.dcriar.api.dto.response.product.DimensoesResponseDTO;
import com.dcriar.api.dto.response.product.MateriaPrimaResponseDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.time.LocalDateTime;

/**
 * Modelo de representação HATEOAS para Produto na API.
 * <p>
 * Inclui informações detalhadas do produto e links para recursos relacionados.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonRootName(value = "produto")
@Relation(collectionRelation = "produtos", itemRelation = "produto")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ProdutoResponse", description = "Representação de um produto com links HATEOAS.")
public class ProdutoModel extends RepresentationModel<ProdutoModel> {

    @Schema(description = "ID único do produto.", example = "1")
    private Long id;

    @Schema(description = "Nome do produto.", example = "Etiqueta Adesiva Redonda 5x5cm Kraft")
    private String nome;

    @Schema(description = "Código único de produto (SKU).", example = "ETQ-KRAFT-RD5")
    private String sku;

    @Schema(description = "Descrição detalhada do produto.", example = "Etiqueta adesiva redonda de papel kraft 5x5cm")
    private String descricao;

    @Schema(description = "Cor principal do produto.", example = "Marrom")
    private String cor;

    @Schema(description = "Quantidade de unidades por produto vendido.", example = "100")
    private Integer unidadesPorProduto;

    @Schema(description = "Indica se o produto está ativo para venda.", example = "true")
    private boolean ativo;

    @Schema(description = "URL da imagem principal do produto.")
    private String fotoPrincipalUrl;

    @Schema(description = "A matéria-prima principal utilizada no produto.")
    private MateriaPrimaResponseDTO materiaPrima;

    @Schema(description = "Quantidade total em estoque (Estoque Mestre).", example = "150")
    private Integer estoqueFisicoTotal;

    @Schema(description = "Quantidade distribuída pelos canais de venda.", example = "100")
    private Integer estoqueDistribuidoTotal;

    @Schema(description = "Saldo de unidades disponíveis para alocação em canais de venda.", example = "50")
    private Integer estoqueDisponivelParaAlocar;

    @Schema(description = "Dimensões unitárias do produto.")
    private DimensoesResponseDTO dimensoes;

    @Schema(description = "Data e hora de criação do produto.")
    private LocalDateTime dataCriacao;

    @Schema(description = "Data e hora da última atualização do produto.")
    private LocalDateTime dataAtualizacao;
}
