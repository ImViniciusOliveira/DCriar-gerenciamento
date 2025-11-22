package com.dcriar.api.dto.request.product;

import com.dcriar.api.validation.annotation.ValidProdutoRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.*;

import java.util.Map;

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

    @Schema(description = "Tipo do produto. 'CORTE' para produtos com dimensões, 'CONSUMO_DIRETO' para outros.", example = "CORTE", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tipoProduto;

    @Schema(description = "Nome descritivo e único do produto.", example = "Etiqueta Adesiva Redonda 5x5cm Kraft", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nome;

    @Schema(description = "Código único de produto (Stock Keeping Unit).", example = "ETQ-KFT-RD5", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sku;

    @Schema(description = "Descrição detalhada sobre o produto, seu material e uso.", example = "Etiquetas de papel Kraft para embalagens artesanais.")
    private String descricao;

    @Schema(description = "Quantidade de itens que compõem uma unidade do produto vendido (ex: 100 etiquetas por pacote).", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer unidadesPorProduto;

    @Schema(description = "URL da imagem principal do produto para exibição no catálogo.", example = "https://cdn.dcriar.com/images/ETQ-KFT-RD5.jpg")
    private String fotoPrincipalUrl;

    @Schema(description = "Define se o produto está ativo e disponível para operações de venda e produção. Se não for fornecido, o padrão é 'true'.", example = "true")
    private Boolean ativo;

    @Schema(description = "ID do Tipo de Matéria-Prima principal que este produto consome.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long tipoMateriaPrimaId;

    // --- Campos para ProdutoDeCorte ---
    @Schema(description = "Cor principal do produto (apenas para produtos de corte).", example = "Marrom")
    private String cor;

    @Valid
    @Schema(description = "As dimensões de uma única unidade do produto (apenas para produtos de corte).")
    private DimensoesRequestDTO dimensoes;

    // --- Campos para ProdutoDeConsumoDireto ---
    @Schema(description = "Código do produto fornecido pelo fabricante (apenas para produtos de consumo direto).", example = "INK-BLK-ES-1L")
    private String codigoFabricante;

    @Schema(description = "Mapa flexível para especificações técnicas (apenas para produtos de consumo direto).", example = "{\"tipo_tinta\": \"Eco-Solvente\", \"volume_ml\": 1000}")
    private Map<String, String> especificacoes;
}
