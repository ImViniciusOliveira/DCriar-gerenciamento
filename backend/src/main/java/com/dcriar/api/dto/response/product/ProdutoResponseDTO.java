package com.dcriar.api.dto.response.product;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "tipoProduto"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ProdutoDeCorteResponseDTO.class, name = "CORTE"),
    @JsonSubTypes.Type(value = ProdutoDeConsumoResponseDTO.class, name = "CONSUMO")
})
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public abstract class ProdutoResponseDTO {

    @Schema(description = "ID único do produto.", example = "1")
    private Long id;

    @Schema(description = "Tipo do produto.", example = "CORTE")
    private String tipoProduto;

    @Schema(description = "Nome do produto.", example = "Etiqueta Adesiva Redonda 5x5cm Kraft")
    private String nome;

    @Schema(description = "Código único de produto (SKU).", example = "ETQ-KRAFT-RD5")
    private String sku;

    @Schema(description = "Descrição detalhada do produto.", example = "Etiqueta adesiva redonda de papel kraft 5x5cm")
    private String descricao;

    @Schema(description = "Quantidade consumida por unidade do produto vendido.", example = "100")
    private BigDecimal unidadesPorProduto;

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

    @Schema(description = "Data e hora de criação do produto.")
    private LocalDateTime dataCriacao;

    @Schema(description = "Data e hora da última atualização do produto.")
    private LocalDateTime dataAtualizacao;
}
