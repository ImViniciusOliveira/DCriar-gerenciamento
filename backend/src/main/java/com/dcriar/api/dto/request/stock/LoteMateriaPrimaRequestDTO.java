package com.dcriar.api.dto.request.stock;

import com.dcriar.api.validation.annotation.ValidLoteMateriaPrimaRequest;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Data Transfer Object (DTO) para registrar um novo Lote de Matéria-Prima.
 * <p>
 * Este DTO é utilizado para dar entrada de novos materiais no estoque, como um novo rolo
 * de adesivo ou uma nova chapa de material. A validação dos campos é garantida
 * pela anotação {@link ValidLoteMateriaPrimaRequest}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidLoteMateriaPrimaRequest
public class LoteMateriaPrimaRequestDTO {

    /**
     * O ID do Tipo de Matéria-Prima ao qual este lote pertence.
     */
    @Schema(description = "ID do Tipo de Matéria-Prima ao qual este lote pertence.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long tipoMateriaPrimaId;

    /**
     * A unidade de medida em que este lote é armazenado e medido fisicamente.
     */
    @Schema(description = "Unidade de medida em que este lote é armazenado e medido fisicamente.", example = "METRO_LINEAR", requiredMode = Schema.RequiredMode.REQUIRED)
    private UnidadeDeMedida unidadeDeEstoque;

    /**
     * A quantidade inicial de material que está dando entrada no estoque (ex: 50 metros, 100 kg).
     */
    @Schema(description = "A quantidade inicial de material que está dando entrada no estoque (ex: 50 metros, 100 kg).", example = "50.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal quantidadeInicial;

    /**
     * O valor total pago por este lote, usado para calcular o custo médio do material.
     */
    @Schema(description = "O valor total pago por este lote (usado para calcular o custo médio).", example = "150.00", requiredMode = Schema.RequiredMode.REQUIRED)
    private BigDecimal custoTotalLote;

    /**
     * Atributos flexíveis (chave-valor) do lote, como largura, fornecedor, etc.
     * <p>
     * As chaves devem ser strings e os valores, números ou strings.
     */
    @Schema(description = "Atributos flexíveis do lote, como largura, fornecedor, etc. As chaves devem ser strings e os valores, números ou strings.",
            example = "{ \"larguraMm\": 600, \"fornecedor\": \"Adesivos Premium\", \"notaFiscal\": \"NF-12345\" }")
    private Map<String, Object> atributos;

    /**
     * Um motivo opcional para a movimentação de entrada (ex: Compra regular, Devolução de cliente).
     */
    @Schema(description = "Motivo opcional para a movimentação de entrada (ex: Compra regular, Devolução de cliente).", example = "Compra regular - Pedido #789")
    private String motivo;
}
