package com.dcriar.api.dto.response.production;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object (DTO) que encapsula detalhes específicos sobre a otimização de um corte.
 * <p>
 * Este DTO é um componente de outros DTOs de resposta, como {@link OrdemDeProducaoResponseDTO},
 * para fornecer informações adicionais sobre como o processo de corte foi calculado e executado.
 */
@Getter
@Setter
@Builder
public class DetalhesCorteResponseDTO {

    /**
     * Descreve a orientação da peça que resultou no melhor aproveitamento do material.
     * <p>
     * Ex: "Normal" ou "Rotacionada".
     */
    @Schema(description = "Descreve a orientação da peça que resultou no melhor aproveitamento do material.", example = "Rotacionada")
    private String orientacaoOtimizada;
}
