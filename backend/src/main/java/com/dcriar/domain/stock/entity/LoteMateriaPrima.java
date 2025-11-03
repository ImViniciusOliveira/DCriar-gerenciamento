package com.dcriar.domain.stock.entity;

import com.dcriar.api.dto.request.stock.LoteMateriaPrimaRequestDTO;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

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
@EqualsAndHashCode(of = "id")
public class LoteMateriaPrima {

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
                .atributos(dto.getAtributos())
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
        }
        if (dto.getAtributos() != null) {
            this.atributos = dto.getAtributos();
        }
        // Atualize outros campos conforme necessário
    }
}
