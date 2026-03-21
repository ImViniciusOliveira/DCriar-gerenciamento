package com.dcriar.api.hateoas.product.model;

import com.dcriar.api.dto.response.product.MateriaPrimaResponseDTO;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Modelo de representação HATEOAS para Produto na API.
 * <p>
 * Esta é uma classe abstrata que serve como base para os diferentes tipos de produtos.
 * A anotação {@code @JsonTypeInfo} garante que o JSON de resposta inclua um campo 'tipoProduto'
 * para que os clientes da API possam diferenciar entre as subclasses.
 * <p>
 * <b>Nota sobre o Builder:</b> Esta classe e suas subclasses não utilizam o {@code @SuperBuilder} do Lombok
 * devido a uma incompatibilidade com a classe pai {@link RepresentationModel}, que não foi projetada
 * para esse padrão. A instanciação é feita manualmente no {@link com.dcriar.api.mapper.product.ProdutoMapper}.
 */
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.PROPERTY,
    property = "tipoProduto"
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = ProdutoDeCorteModel.class, name = "CORTE"),
    @JsonSubTypes.Type(value = ProdutoDeConsumoModel.class, name = "CONSUMO")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = false)
@JsonRootName(value = "produto")
@Relation(collectionRelation = "produtos", itemRelation = "produto")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(name = "ProdutoResponse", description = "Representação de um produto com links HATEOAS.")
public abstract class ProdutoModel extends RepresentationModel<ProdutoModel> {

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

    @Schema(description = "Preço comercial atual do produto.", example = "19.90")
    private BigDecimal precoComercial;

    @Schema(description = "Data e hora de criação do produto.")
    private LocalDateTime dataCriacao;

    @Schema(description = "Data e hora da última atualização do produto.")
    private LocalDateTime dataAtualizacao;
}
