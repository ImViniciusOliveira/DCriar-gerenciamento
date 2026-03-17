package com.dcriar.api.dto.request.production;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import java.math.BigDecimal;
import java.util.Set;

/**
 * DTO para requisições de criação/atualização de OrdemDeProducao.
 * <p>
 * Este DTO não usa validador personalizado próprio porque é interno ao backend
 * e serve como ponte para montar a entidade de ordem já calculada.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrdemDeProducaoRequestDTO {
    /**
     * ID do produto final a ser fabricado.
     */
    @Schema(description = "ID do produto final a ser fabricado. No seed padrão, use 1 para 'Cartão de Visita Premium'.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long produtoId;

    /**
     * IDs dos lotes de matéria-prima consumidos.
     */
    @Schema(description = "IDs dos lotes de matéria-prima consumidos. No seed padrão, a ordem do produto 1 pode consumir o lote 1.", example = "[1]", requiredMode = Schema.RequiredMode.REQUIRED)
    private Set<Long> lotesConsumidosIds;

    /**
     * ID do canal de venda destino (opcional).
     */
    @Schema(description = "ID do canal de venda para o qual o estoque produzido será destinado (opcional). No seed padrão, use 3 para 'Site Próprio'.", example = "3")
    private Long canalVendaDestinoId;

    /**
     * Quantidade produzida.
     */
    @Schema(description = "Quantidade de unidades do produto a serem produzidas. No seed padrão, 100 unidades do produto 1 formam um caso coerente.", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidadeProduzida;

    /**
     * Modo de cálculo utilizado.
     */
    @Schema(description = "Modo de cálculo utilizado na produção (ex: AUTOMATICO, MANUAL, CONSUMO).", example = "AUTOMATICO", requiredMode = Schema.RequiredMode.REQUIRED)
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
    @Schema(description = "Motivo ou observação para a ordem de produção.", example = "Produção para pedido do Site Próprio #5521")
    private String motivo;

    /**
     * Indica se o produto foi rotacionado para otimização do corte.
     */
    @Schema(description = "Indica se o produto foi rotacionado para otimização do corte (geralmente informado pela simulação).", example = "true")
    private boolean rotacionado;
}
