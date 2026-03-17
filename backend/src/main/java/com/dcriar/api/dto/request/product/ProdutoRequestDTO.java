package com.dcriar.api.dto.request.product;

import com.dcriar.api.validation.annotation.ValidProdutoRequest;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.*;

import java.math.BigDecimal;
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

    @Schema(description = "Tipo do produto. 'CORTE' para produtos com dimensões, 'CONSUMO' para outros.", example = "CONSUMO", requiredMode = Schema.RequiredMode.REQUIRED)
    private String tipoProduto;

    @Schema(description = "Nome descritivo e único do produto.", example = "Refil de Tinta Eco-Solvente Ciano 1500ml", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nome;

    @Schema(description = "Código único de produto (Stock Keeping Unit).", example = "TIN-CYA-ES-1500ML", requiredMode = Schema.RequiredMode.REQUIRED)
    private String sku;

    @Schema(description = "Descrição detalhada sobre o produto, seu material e uso.", example = "Refil de tinta ciano para impressoras eco-solvente, com 1500 ml por unidade.")
    private String descricao;

    @Schema(description = "Quantidade consumida por unidade do produto. Para consumo, aceita decimal e pode ser informada na unidade principal ou na unidade menor compatível.", example = "1500", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal unidadesPorProduto;

    @Schema(description = "URL da imagem principal do produto para exibição no catálogo.", example = "https://cdn.dcriar.com/images/ETQ-KFT-RD5.jpg")
    private String fotoPrincipalUrl;

    @Schema(description = "Define se o produto está ativo e disponível para operações de venda e produção. Se não for fornecido, o padrão é 'true'.", example = "true")
    private Boolean ativo;

    @Schema(description = "ID do Tipo de Matéria-Prima principal que este produto consome. No seed padrão, 6 representa 'Tinta Eco-Solvente Preta'.", example = "6", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long tipoMateriaPrimaId;

    // --- Campos para ProdutoDeCorte ---
    @Schema(description = "Cor principal do produto (apenas para produtos de corte).", example = "Marrom")
    private String cor;

    @Valid
    @Schema(description = "As dimensões de uma única unidade do produto (apenas para produtos de corte).")
    private DimensoesRequestDTO dimensoes;

    // --- Campos para ProdutoDeConsumo ---
    @Schema(description = "Código do produto fornecido pelo fabricante (apenas para produtos de consumo).", example = "INK-CYA-ES-1500ML")
    private String codigoFabricante;

    @Schema(description = "Unidade usada para informar 'unidadesPorProduto' no cadastro de consumo. Se não for enviada, o backend assume a unidade principal da matéria-prima. Exemplos compatíveis: LITRO/MILILITRO, QUILOGRAMA/GRAMA.", example = "MILILITRO")
    private UnidadeDeMedida unidadeCadastroConsumo;

    @Schema(description = "Mapa flexível para especificações técnicas (apenas para produtos de consumo).", example = "{\"tipo_tinta\": \"Eco-Solvente\", \"cor_pantone\": \"Cyan\", \"volume_ml\": 1500}")
    private Map<String, String> especificacoes;
}
