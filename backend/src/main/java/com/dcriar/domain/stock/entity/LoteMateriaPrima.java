package com.dcriar.domain.stock.entity;

import com.dcriar.api.dto.request.stock.LoteMateriaPrimaRequestDTO;
import com.dcriar.domain.common.entity.AuditableEntity;
import com.dcriar.domain.common.util.MapStringValueTrimmer;
import com.dcriar.domain.common.util.TrimTextNormalizer;
import com.dcriar.domain.production.entity.OrdemDeProducao;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.Formula;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Representa um lote físico e rastreável de uma matéria-prima no estoque.
 * <p>
 * Cada instância corresponde a um item tangível, como um rolo de adesivo específico
 * ou um galão de tinta, com seus próprios atributos e histórico de movimentações.
 */
@Entity
@Table(name = "lotes_materia_prima")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"movimentacoes", "loteDeOrigem"})
@EqualsAndHashCode(of = "id", callSuper = false)
public class LoteMateriaPrima extends AuditableEntity {

    private static final String ZERO_NUMERIC_SQL = "0::numeric";
    private static final String SALDO_ATUAL_SQL =
            "(SELECT COALESCE(SUM(m.quantidade), " + ZERO_NUMERIC_SQL + ") FROM movimentacoes_estoque_lote m WHERE m.lote_id = id)";
    private static final String QTD_ENTRADA_COMPRA_SQL =
            "(SELECT COALESCE(SUM(m.quantidade), " + ZERO_NUMERIC_SQL + ") FROM movimentacoes_estoque_lote m WHERE m.lote_id = id AND m.tipo = 'ENTRADA_COMPRA')";
    private static final String QTD_ENTRADA_SOBRA_SQL =
            "(SELECT COALESCE(SUM(m.quantidade), " + ZERO_NUMERIC_SQL + ") FROM movimentacoes_estoque_lote m WHERE m.lote_id = id AND m.tipo = 'ENTRADA_SOBRA')";
    private static final String QTD_AJUSTE_POSITIVO_SQL =
            "(SELECT COALESCE(SUM(m.quantidade), " + ZERO_NUMERIC_SQL + ") FROM movimentacoes_estoque_lote m WHERE m.lote_id = id AND m.tipo = 'AJUSTE_INVENTARIO' AND m.quantidade > 0)";
    private static final String TEM_AJUSTE_OU_PERDA_SQL =
            "EXISTS (SELECT 1 FROM movimentacoes_estoque_lote m WHERE m.lote_id = id AND m.tipo IN ('AJUSTE_INVENTARIO', 'PERDA_DESCARTE'))";
    private static final String QUANTIDADE_BASE_COM_CUSTO_SQL =
            "(CASE " +
                    "WHEN " + QTD_ENTRADA_COMPRA_SQL + " > " + ZERO_NUMERIC_SQL + " THEN " + QTD_ENTRADA_COMPRA_SQL + " " +
                    "WHEN " + QTD_ENTRADA_SOBRA_SQL + " > " + ZERO_NUMERIC_SQL + " THEN " + QTD_ENTRADA_SOBRA_SQL + " " +
                    "WHEN " + QTD_AJUSTE_POSITIVO_SQL + " > " + ZERO_NUMERIC_SQL + " THEN " + QTD_AJUSTE_POSITIVO_SQL + " " +
                    "ELSE " + ZERO_NUMERIC_SQL + " END)";
    private static final String UNIDADE_CONSUMO_SQL =
            "(SELECT t.unidade_de_consumo FROM tipos_materia_prima t WHERE t.id = tipo_materia_prima_id)";
    private static final String SALDO_APRESENTACAO_SQL =
            "(CASE " +
                    "WHEN " + UNIDADE_CONSUMO_SQL + " = 'QUILOGRAMA' AND unidade_cadastro_estoque = 'QUILOGRAMA' " +
                    "THEN " + SALDO_ATUAL_SQL + " / 1000 " +
                    "WHEN " + UNIDADE_CONSUMO_SQL + " = 'LITRO' AND unidade_cadastro_estoque = 'LITRO' " +
                    "THEN " + SALDO_ATUAL_SQL + " / 1000 " +
                    "ELSE " + SALDO_ATUAL_SQL + " END)";
    private static final String VALOR_ATUAL_LOTE_SQL =
            "(CASE " +
                    "WHEN custo_total_lote IS NULL OR " + SALDO_ATUAL_SQL + " <= " + ZERO_NUMERIC_SQL + " THEN " + ZERO_NUMERIC_SQL + " " +
                    "WHEN " + TEM_AJUSTE_OU_PERDA_SQL + " THEN custo_total_lote " +
                    "WHEN " + QUANTIDADE_BASE_COM_CUSTO_SQL + " <= " + ZERO_NUMERIC_SQL + " THEN " + ZERO_NUMERIC_SQL + " " +
                    "ELSE custo_total_lote * " + SALDO_ATUAL_SQL + " / NULLIF(" + QUANTIDADE_BASE_COM_CUSTO_SQL + ", " + ZERO_NUMERIC_SQL + ") END)";
    private static final String CUSTO_UNITARIO_ATUAL_SQL =
            "(CASE " +
                    "WHEN " + SALDO_APRESENTACAO_SQL + " <= " + ZERO_NUMERIC_SQL + " THEN " + ZERO_NUMERIC_SQL + " " +
                    "ELSE " + VALOR_ATUAL_LOTE_SQL + " / NULLIF(" + SALDO_APRESENTACAO_SQL + ", " + ZERO_NUMERIC_SQL + ") END)";

    /**
     * O ID único do lote de matéria-prima.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * O tipo de matéria-prima a que este lote pertence.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tipo_materia_prima_id", nullable = false)
    private TipoMateriaPrima tipoMateriaPrima;

    /**
     * A unidade de medida em que este lote é armazenado e medido fisicamente.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_de_estoque", nullable = false, length = 30)
    private UnidadeDeMedida unidadeDeEstoque;

    /**
     * A unidade escolhida pelo usuário no cadastro do lote.
     * <p>
     * No estado atual ela coincide com {@code unidadeDeEstoque}, mas fica persistida separadamente
     * para permitir futura normalização interna sem perder a unidade de apresentação escolhida.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_cadastro_estoque", nullable = false, length = 30)
    private UnidadeDeMedida unidadeCadastroEstoque;

    /**
     * O custo total do lote no momento da entrada.
     */
    @Column(name = "custo_total_lote", nullable = false, precision = 10, scale = 4)
    private BigDecimal custoTotalLote;

    /**
     * Saldo atual do lote calculado diretamente no banco a partir das movimentações.
     * <p>
     * Este campo é somente leitura e serve para suportar listagem e ordenação server-side
     * sem depender de uma consulta adicional por lote.
     */
    @Formula(SALDO_ATUAL_SQL)
    private BigDecimal saldoAtual;

    /**
     * Saldo atual na unidade de apresentação do lote.
     * <p>
     * Campo somente leitura usado em respostas e listagens sem depender de enriquecimento manual.
     */
    @Formula(SALDO_APRESENTACAO_SQL)
    private BigDecimal saldoEstoque;

    /**
     * Valor econômico atual do saldo remanescente do lote.
     * <p>
     * Campo somente leitura usado para listagem e ordenação server-side.
     */
    @Formula(VALOR_ATUAL_LOTE_SQL)
    private BigDecimal valorAtualLote;

    /**
     * Custo unitário atual do lote na unidade de apresentação.
     * <p>
     * Campo somente leitura usado para listagem e ordenação server-side.
     */
    @Formula(CUSTO_UNITARIO_ATUAL_SQL)
    private BigDecimal custoUnitarioAtual;

    /**
     * O motivo da criação ou entrada deste lote no estoque.
     */
    @Column(name = "motivo", nullable = false, length = 255)
    private String motivo;

    /**
     * Campo JSONB para armazenar atributos flexíveis do lote, como 'larguraMm' para rolos.
     * <p>
     * Permite adicionar informações específicas do lote sem a necessidade de alterar o esquema do banco de dados.
     */
    @Type(JsonBinaryType.class)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> atributos;

    /**
     * O histórico de movimentações deste lote.
     * <p>
     * Usamos LAZY fetch para otimizar a performance, garantindo que o histórico
     * só seja carregado do banco de dados quando for explicitamente necessário.
     * {@code CascadeType.ALL} e {@code orphanRemoval = true} garantem a integridade referencial.
     */
    @OneToMany(mappedBy = "lote", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<MovimentacaoEstoqueLote> movimentacoes = new ArrayList<>();

    /**
     * Relação opcional que liga um lote de sobra (retalho) ao seu lote de origem.
     * <p>
     * Essencial para a rastreabilidade da produção. Será nulo para lotes principais (entradas por compra).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_de_origem_id")
    private LoteMateriaPrima loteDeOrigem;

    /**
     * A ordem de produção que gerou este lote (se for um retalho).
     * Permite rastrear quais lotes foram criados por uma ordem específica.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_producao_origem_id")
    private OrdemDeProducao ordemDeProducaoOrigem;

    /**
     * Campo transiente para expor o saldo calculado.
     * <p>
     * Este valor é calculado sob demanda pelo serviço a partir das movimentações.
     * A anotação {@code @Transient} impede que o JPA tente criar uma coluna para ele no banco.
     */
    @Transient
    private BigDecimal saldoCalculado;

    /**
     * Cria uma instância de {@link LoteMateriaPrima} a partir de um DTO de request e do tipo de matéria-prima.
     * <p>
     * Este método deve ser utilizado pela camada de serviço para centralizar regras de negócio de criação.
     * Garante que toda lógica relacionada à criação do lote fique encapsulada na entidade.
     *
     * @param dto DTO de request contendo os dados para criação do lote
     * @param tipoMateriaPrima Tipo de matéria-prima já validado e recuperado
     * @return Nova instância de {@link LoteMateriaPrima} pronta para persistência
     */
    public static LoteMateriaPrima from(LoteMateriaPrimaRequestDTO dto, TipoMateriaPrima tipoMateriaPrima) {
        // Centralize regras de negócio aqui (ex: normalização, validação extra, defaults)
        return LoteMateriaPrima.builder()
                .tipoMateriaPrima(tipoMateriaPrima)
                .unidadeDeEstoque(dto.getUnidadeDeEstoque())
                .unidadeCadastroEstoque(dto.getUnidadeCadastroEstoque() != null ? dto.getUnidadeCadastroEstoque() : dto.getUnidadeDeEstoque())
                .atributos(MapStringValueTrimmer.trimObjectStringValues(dto.getAtributos()))
                .custoTotalLote(dto.getCustoTotalLote())
                .motivo(TrimTextNormalizer.trimToNull(dto.getMotivo()))
                // Adicione outros campos conforme necessário
                .build();
    }

    /**
     * Atualiza os campos da entidade a partir de um DTO de request e do tipo de matéria-prima.
     * <p>
     * Este método deve ser utilizado pela camada de serviço para centralizar regras de negócio de atualização.
     * Apenas campos presentes no DTO serão atualizados, mantendo a lógica encapsulada na entidade.
     *
     * @param dto DTO de request contendo os dados para atualização
     * @param tipoMateriaPrima Tipo de matéria-prima já validado e recuperado (pode ser nulo)
     */
    public void updateFrom(LoteMateriaPrimaRequestDTO dto, TipoMateriaPrima tipoMateriaPrima) {
        // Centralize regras de negócio para atualização
        if (tipoMateriaPrima != null) {
            this.tipoMateriaPrima = tipoMateriaPrima;
        }
        if (dto.getUnidadeDeEstoque() != null) {
            this.unidadeDeEstoque = dto.getUnidadeDeEstoque();
            this.unidadeCadastroEstoque = dto.getUnidadeCadastroEstoque() != null ? dto.getUnidadeCadastroEstoque() : dto.getUnidadeDeEstoque();
        } else if (dto.getUnidadeCadastroEstoque() != null) {
            this.unidadeCadastroEstoque = dto.getUnidadeCadastroEstoque();
        }
        if (dto.getAtributos() != null) {
            this.atributos = MapStringValueTrimmer.trimObjectStringValues(dto.getAtributos());
        }
        if (dto.getCustoTotalLote() != null) {
            this.custoTotalLote = dto.getCustoTotalLote();
        }
        if (dto.getMotivo() != null) {
            this.motivo = TrimTextNormalizer.trimToNull(dto.getMotivo());
        }
    }
}
