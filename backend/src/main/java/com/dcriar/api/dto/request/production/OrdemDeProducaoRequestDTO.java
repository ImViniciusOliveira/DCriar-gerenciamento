package com.dcriar.api.dto.request.production;

import com.dcriar.api.validation.annotation.ValidOrdemDeProducaoRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
import java.util.Set;

/**
 * DTO para requisições de criação/atualização de OrdemDeProducao.
 * Centraliza validações e documentação dos campos necessários.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ValidOrdemDeProducaoRequest
public class OrdemDeProducaoRequestDTO {
    /**
     * ID do produto final a ser fabricado.
     */
    @Schema(description = "ID do produto final a ser fabricado.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long produtoId;

    /**
     * IDs dos lotes de matéria-prima consumidos.
     */
    @Schema(description = "IDs dos lotes de matéria-prima consumidos.", example = "[1, 2]", requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<Long> lotesConsumidosIds;

    /**
     * ID do canal de venda destino (opcional).
     */
    @Schema(description = "ID do canal de venda para o qual o estoque produzido será destinado (opcional).", example = "3")
    private Long canalVendaDestinoId;

    /**
     * Quantidade produzida.
     */
    @Schema(description = "Quantidade de unidades do produto a serem produzidas.", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidadeProduzida;

    /**
     * Modo de cálculo utilizado.
     */
    @Schema(description = "Modo de cálculo utilizado na produção (ex: AUTOMATICO, MANUAL, CONSUMO_DIRETO).", example = "AUTOMATICO", requiredMode = Schema.RequiredMode.REQUIRED)
    private String modoCalculo;

    /**
     * Margens de segurança (opcional).
     */
    @Schema(description = "Margens de segurança a serem aplicadas no corte (relevante no modo AUTOMATICO).")
    private MargensRequestDTO margens;

    /**
     * Largura final do corte em cm (opcional).
     */
    @Schema(description = "Largura final do material consumido em cm (relevante no modo MANUAL).", example = "80.5")
    private BigDecimal larguraFinalCm;

    /**
     * Comprimento final do corte em cm (opcional).
     */
    @Schema(description = "Comprimento final do material consumido em cm (relevante no modo MANUAL).", example = "120.0")
    private BigDecimal comprimentoFinalCm;

    /**
     * Motivo ou observação da ordem de produção.
     */
    @Schema(description = "Motivo ou observação para a ordem de produção.", example = "Produção para pedido #5521")
    private String motivo;

    /**
     * Indica se o produto foi rotacionado para otimização do corte.
     */
    @Schema(description = "Indica se o produto foi rotacionado para otimização do corte (geralmente informado pela simulação).", example = "true")
    private boolean rotacionado;
}
