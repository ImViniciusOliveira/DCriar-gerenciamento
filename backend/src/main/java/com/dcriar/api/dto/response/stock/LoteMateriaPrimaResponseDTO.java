package com.dcriar.api.dto.response.stock;

import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Set;

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

    @Schema(description = "Saldo técnico atual do lote na unidade interna de cálculo.", example = "490000.0000")
    private BigDecimal saldoInternoAtual;

    @Schema(description = "Custo total do lote no momento da entrada.", example = "150.00")
    private BigDecimal custoTotalLote;

    @Schema(description = "Valor econômico atual do saldo remanescente do lote.", example = "125.00")
    private BigDecimal valorAtualLote;

    @Schema(description = "Custo unitário atual do lote na unidade de apresentação.", example = "5.00")
    private BigDecimal custoUnitarioAtual;

    @Schema(description = "Atributos flexíveis que descrevem as especificações deste lote físico.", example = "{\"larguraMm\": 610}")
    private Map<String, Object> atributos;

    @Schema(description = "ID do lote que deu origem a este (se for um retalho).", nullable = true, example = "1")
    private Long loteDeOrigemId;

    @Schema(description = "Identificador público do lote para exibição ao usuário.", example = "LT-000015")
    private String identificadorPublico;

    @Schema(description = "Identificador público da origem imediata, quando este item for um retalho.", nullable = true, example = "RT-000013")
    private String identificadorOrigemPublico;

    @Schema(description = "Tipo estrutural do item na árvore de lotes.", example = "RETALHO")
    private String tipoEstrutural;

    @Schema(description = "Motivo da criação ou entrada deste lote no estoque.", example = "Compra regular - Pedido #789")
    private String motivo;

    @Schema(description = "Data e hora de criação do lote.")
    private LocalDateTime dataCriacao;

    @Schema(description = "Data e hora da última atualização do lote.")
    private LocalDateTime dataAtualizacao;

    @Schema(description = "Campos atualmente bloqueados para edição no frontend.", example = "[\"tipoMateriaPrimaId\", \"custoTotalLote\", \"atributos.larguraMm\"]")
    private Set<String> camposBloqueados;

    @Schema(description = "Motivos por campo bloqueado, para orientar a UI.", example = "{\"custoTotalLote\":\"Lote já utilizado em produção ou ajuste.\"}")
    private Map<String, String> motivosBloqueio;
}
