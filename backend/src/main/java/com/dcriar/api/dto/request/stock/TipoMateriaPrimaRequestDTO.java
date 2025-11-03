package com.dcriar.api.dto.request.stock;

import com.dcriar.api.validation.annotation.ValidTipoMateriaPrimaRequest;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

/**
 * Data Transfer Object (DTO) para criar ou atualizar um Tipo de Matéria-Prima.
 * <p>
 * Este DTO é utilizado para definir um novo tipo de material que a empresa utiliza,
 * especificando seu nome e como ele é medido no consumo.
 * A validação dos campos é garantida pela anotação {@link ValidTipoMateriaPrimaRequest}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidTipoMateriaPrimaRequest
public class TipoMateriaPrimaRequestDTO {

    /**
     * O nome único do tipo de matéria-prima (ex: "Adesivo Vinil Branco Brilho").
     */
    @Schema(description = "Nome único do tipo de matéria-prima.", example = "Adesivo Vinil Branco Brilho", requiredMode = Schema.RequiredMode.REQUIRED)
    private String nome;

    /**
     * A unidade de medida padrão em que a "receita" de um produto consome este material.
     */
    @Schema(description = "Unidade em que a 'receita' de um produto consome este material.", example = "CENTIMETRO_QUADRADO", requiredMode = Schema.RequiredMode.REQUIRED)
    private UnidadeDeMedida unidadeDeConsumo;
}
