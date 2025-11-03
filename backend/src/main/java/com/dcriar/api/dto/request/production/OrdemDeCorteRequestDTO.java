package com.dcriar.api.dto.request.production;

import com.dcriar.api.validation.annotation.ValidOrdemDeCorteRequest;
import com.dcriar.domain.production.enums.ModoCalculo;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.math.BigDecimal;

/**
 * Data Transfer Object (DTO) para criar uma nova Ordem de Produção por Corte Geométrico.
 * <p>
 * Utilizado para produtos cuja matéria-prima é medida e cortada geometricamente (ex: metros, cm²).
 * Suporta dois modos de operação: AUTOMATICO, onde o sistema otimiza o corte, e MANUAL,
 * onde o usuário informa as dimensões exatas do material consumido.
 * A validação das regras de negócio é garantida pela anotação {@link ValidOrdemDeCorteRequest}.
 */
@Getter
@Setter
@ToString
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidOrdemDeCorteRequest
public class OrdemDeCorteRequestDTO {

    /**
     * O ID do produto a ser fabricado (deve ser um produto de matéria-prima geométrica).
     */
    @Schema(description = "ID do produto a ser fabricado (deve ser um produto de matéria-prima geométrica).", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long produtoId;

    /**
     * O ID do lote de matéria-prima principal a ser consumido.
     */
    @Schema(description = "ID do lote de matéria-prima principal a ser consumido.", example = "2", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long lotePrincipalId;

    /**
     * O ID do canal de venda de destino do estoque (opcional). Se fornecido, o estoque produzido será alocado neste canal.
     */
    @Schema(description = "ID do canal de venda de destino do estoque (opcional). Se fornecido, o estoque produzido será alocado neste canal.", example = "5")
    private Long canalVendaDestinoId;

    /**
     * A quantidade de unidades do produto a serem produzidas.
     */
    @Schema(description = "Quantidade de unidades do produto a serem produzidas.", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer quantidadeProduzida;

    /**
     * O modo de cálculo para o corte (AUTOMATICO ou MANUAL).
     */
    @Schema(description = "Modo de cálculo para o corte.", example = "AUTOMATICO", requiredMode = Schema.RequiredMode.REQUIRED)
    private ModoCalculo modoCalculo;

    /**
     * As margens de segurança (em cm) a serem aplicadas. Relevante no modo AUTOMATICO, mas opcional (padrão zero se não fornecido).
     */
    @Schema(description = "Margens de segurança (em cm) a serem aplicadas. Relevante no modo AUTOMATICO, mas opcional (padrão zero se não fornecido).")
    private MargensRequestDTO margens;

    /**
     * A largura final do corte em cm (obrigatório no modo MANUAL).
     */
    @Schema(description = "Largura final do corte em cm (obrigatório no modo MANUAL).", example = "80.0")
    private BigDecimal larguraFinalCm;

    /**
     * O comprimento final do corte em cm (obrigatório no modo MANUAL).
     */
    @Schema(description = "Comprimento final do corte em cm (obrigatório no modo MANUAL).", example = "120.0")
    private BigDecimal comprimentoFinalCm;

    /**
     * Um motivo ou referência para a ordem.
     */
    @Schema(description = "Motivo ou referência para a ordem.", example = "Pedido Cliente #456")
    private String motivo;
}
