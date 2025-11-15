package com.dcriar.api.dto.request.product;

import com.dcriar.api.validation.annotation.ValidProdutoRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.*;

/**
 * Data Transfer Object (DTO) para criar ou atualizar um Produto.
 * <p>
 * Este DTO contém todos os campos necessários para definir ou modificar um produto,
 * servindo como "molde" para a produção e venda. A validação dos dados é garantida
 * pela anotação customizada {@link ValidProdutoRequest}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidProdutoRequest
public class ProdutoRequestDTO {

    /**
     * O nome descritivo e único do produto.
     */
    @Schema(description = "Nome descritivo e único do produto.", example = "Etiqueta Adesiva Redonda 5x5cm Kraft", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nome;

    /**
     * O código único de produto (SKU - Stock Keeping Unit).
     */
    @Schema(description = "Código único de produto (Stock Keeping Unit).", example = "ETQ-KFT-RD5", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sku;

    /**
     * A descrição detalhada sobre o produto, seu material e uso.
     */
    @Schema(description = "Descrição detalhada sobre o produto, seu material e uso.", example = "Etiquetas de papel Kraft para embalagens artesanais.")
    private String descricao;

    /**
     * A cor principal do produto.
     */
    @Schema(description = "Cor principal do produto.", example = "Marrom", requiredMode = Schema.RequiredMode.REQUIRED)
    private String cor;

    /**
     * A quantidade de itens que compõem uma unidade do produto vendido (ex: 100 etiquetas por pacote).
     */
    @Schema(description = "Quantidade de itens que compõem uma unidade do produto vendido (ex: 100 etiquetas por pacote).", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer unidadesPorProduto;

    /**
     * A URL da imagem principal do produto para exibição no catálogo.
     */
    @Schema(description = "URL da imagem principal do produto para exibição no catálogo.", example = "https://cdn.dcriar.com/images/ETQ-KFT-RD5.jpg")
    private String fotoPrincipalUrl;

    /**
     * Define se o produto está ativo e disponível para operações de venda e produção.
     * Se não for fornecido, o padrão pode ser 'true'.
     */
    @Schema(description = "Define se o produto está ativo e disponível para operações de venda e produção. Se não for fornecido, o padrão é 'true'.", example = "true")
    private Boolean ativo;

    /**
     * O ID do Tipo de Matéria-Prima principal que este produto consome.
     */
    @Schema(description = "ID do Tipo de Matéria-Prima principal que este produto consome.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long tipoMateriaPrimaId;

    /**
     * As dimensões de uma única unidade do produto.
     */
    @Valid
    @Schema(description = "As dimensões de uma única unidade do produto.", requiredMode = Schema.RequiredMode.REQUIRED)
    private DimensoesRequestDTO dimensoes;
}
