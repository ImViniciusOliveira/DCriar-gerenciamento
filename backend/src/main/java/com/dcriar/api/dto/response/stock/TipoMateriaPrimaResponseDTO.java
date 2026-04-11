package com.dcriar.api.dto.response.stock;

import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

/**
 * Data Transfer Object (DTO) que representa a resposta de um Tipo de Matéria-Prima.
 * <p>
 * Este DTO fornece os detalhes essenciais de um tipo de matéria-prima, como seu nome
 * e a unidade de medida utilizada para consumo no processo produtivo.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TipoMateriaPrimaResponseDTO {

    /**
     * O ID único do tipo de matéria-prima.
     */
    @Schema(description = "ID único do tipo de matéria-prima.", example = "1")
    private Long id;

    /**
     * O nome descritivo do tipo de matéria-prima.
     */
    @Schema(description = "Nome do tipo de matéria-prima.", example = "Adesivo Vinil Branco Brilho")
    private String nome;

    /**
     * A unidade de medida em que o material é consumido durante a produção.
     */
    @Schema(description = "Unidade em que o material é consumido.", example = "CENTIMETRO_QUADRADO")
    private UnidadeDeMedida unidadeDeConsumo;

    /**
     * Descrição amigável da unidade de medida (ex: "Metro Quadrado", "Unidade").
     */
    @Schema(description = "Descrição da unidade de medida.", example = "Metro Quadrado")
    private String unidadeDescricao;

    @Schema(description = "Quantidade mínima crítica para análise consolidada de estoque, na unidade principal do tipo.", example = "10.0000")
    private BigDecimal estoqueCritico;

    /**
     * Data e hora de criação do tipo de matéria-prima.
     */
    @Schema(description = "Data e hora de criação do tipo de matéria-prima.")
    private LocalDateTime dataCriacao;

    /**
     * Data e hora da última atualização do tipo de matéria-prima.
     */
    @Schema(description = "Data e hora da última atualização do tipo de matéria-prima.")
    private LocalDateTime dataAtualizacao;

    @Schema(description = "Campos atualmente bloqueados para edição no frontend.", example = "[\"unidadeDeConsumo\"]")
    private Set<String> camposBloqueados;

    @Schema(description = "Motivos por campo bloqueado, para orientar a UI.", example = "{\"unidadeDeConsumo\":\"Tipo de matéria-prima já utilizado por produtos ou lotes.\"}")
    private Map<String, String> motivosBloqueio;
}
