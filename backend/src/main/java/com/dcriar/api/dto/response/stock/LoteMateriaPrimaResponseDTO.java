package com.dcriar.api.dto.response.stock;

import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Data Transfer Object (DTO) para a resposta detalhada de um Lote de Matéria-Prima.
 * <p>
 * Este DTO fornece uma visão completa de um lote físico de matéria-prima, incluindo
 * seu tipo, saldo em estoque, atributos específicos e rastreabilidade de origem.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LoteMateriaPrimaResponseDTO {

    @Schema(description = "ID único do lote.", example = "1")
    private Long id;

    @Schema(description = "ID do tipo de matéria-prima a que este lote pertence.", example = "10")
    private Long tipoMateriaPrimaId;

    @Schema(description = "Nome do tipo de matéria-prima a que este lote pertence.", example = "Adesivo Kraft Pardo")
    private String nomeTipoMateriaPrima;

    @Schema(description = "Unidade em que o saldo deste lote é medido.", example = "METRO_LINEAR")
    private UnidadeDeMedida unidadeDeEstoque;

    @Schema(description = "Unidade escolhida pelo usuário no cadastro do lote.", example = "METRO_LINEAR")
    private UnidadeDeMedida unidadeCadastroEstoque;

    @Schema(description = "Símbolo da unidade de medida (ex: m, un, kg).", example = "m")
    private String unidadeSimbolo;

    @Schema(description = "O saldo de estoque atual deste lote.", example = "49.0000")
    private BigDecimal saldoEstoque;

    @Schema(description = "Custo total do lote no momento da entrada.", example = "150.00")
    private BigDecimal custoTotalLote;

    @Schema(description = "Atributos flexíveis que descrevem as especificações deste lote físico.", example = "{\"larguraMm\": 610}")
    private Map<String, Object> atributos;

    @Schema(description = "ID do lote que deu origem a este (se for um retalho).", nullable = true, example = "1")
    private Long loteDeOrigemId;

    @Schema(description = "Motivo da criação ou entrada deste lote no estoque.", example = "Compra regular - Pedido #789")
    private String motivo;
}
