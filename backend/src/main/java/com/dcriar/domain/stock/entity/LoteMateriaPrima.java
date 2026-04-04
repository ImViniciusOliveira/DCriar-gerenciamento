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
    @Formula("(SELECT COALESCE(SUM(m.quantidade), 0) FROM movimentacoes_estoque_lote m WHERE m.lote_id = id)")
    private BigDecimal saldoAtual;

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
