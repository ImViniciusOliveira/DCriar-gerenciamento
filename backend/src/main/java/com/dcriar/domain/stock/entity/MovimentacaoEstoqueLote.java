package com.dcriar.domain.stock.entity;

import com.dcriar.api.dto.request.stock.MovimentacaoRequestDTO;
import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade que representa um único registro no "Livro-Razão" do estoque de um lote de matéria-prima.
 * <p>
 * Cada movimentação detalha a data, o tipo (entrada/saída), a quantidade, o motivo e o custo por unidade,
 * permitindo rastrear o histórico completo de um lote.
 * <p>
 * <b>Padrão de projeto:</b> Toda criação ou atualização de movimentação deve ser feita via os métodos
 * {@link #from(MovimentacaoRequestDTO, LoteMateriaPrima)} e {@link #updateFrom(MovimentacaoRequestDTO, LoteMateriaPrima)},
 * que centralizam as regras de negócio e garantem consistência.
 * <p>
 * O campo {@code custoPorUnidadeBase} só deve ser preenchido em movimentações de entrada de compra.
 */
@Entity
@Table(name = "movimentacoes_estoque_lote")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
@EqualsAndHashCode(of = "id")
@EntityListeners(AuditingEntityListener.class)
public class MovimentacaoEstoqueLote {

    /**
     * O ID único da movimentação de estoque do lote.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * O lote de matéria-prima ao qual esta movimentação pertence.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id", nullable = false)
    private LoteMateriaPrima lote;

    /**
     * A data e hora em que a movimentação foi registrada.
     * Gerado automaticamente no momento da criação.
     */
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime data;

    /**
     * O tipo da movimentação (ex: ENTRADA_COMPRA, SAIDA_PRODUCAO).
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TipoMovimentacao tipo;

    /**
     * A quantidade movimentada. Positiva para entradas, negativa para saídas.
     */
    @Column(nullable = false, precision = 10, scale = 4)
    private BigDecimal quantidade;

    /**
     * O custo calculado por unidade de consumo (ex: R$/cm², R$/ml).
     * <p>
     * Este campo só é preenchido em movimentações de ENTRADA_COMPRA.
     */
    @Column(name = "custo_por_unidade_base", precision = 19, scale = 8)
    private BigDecimal custoPorUnidadeBase;

    /**
     * O motivo ou observação registrado para a movimentação.
     */
    @Column(length = 254)
    private String motivo;

    /**
     * Cria uma nova instância de MovimentacaoEstoqueLote a partir do DTO de request e do lote informado.
     * <p>
     * <b>Centraliza regras de negócio de criação de movimentação de estoque.</b>
     * <ul>
     *   <li>Deve ser utilizado exclusivamente pela camada de service.</li>
     *   <li>Recebe o DTO de request validado e o lote de matéria-prima.</li>
     *   <li>Não preenche o campo {@code custoPorUnidadeBase}, que deve ser setado manualmente se necessário.</li>
     * </ul>
     * <p>
     * <b>Exemplo de uso:</b>
     * <pre>
     * MovimentacaoRequestDTO dto = ...;
     * MovimentacaoEstoqueLote mov = MovimentacaoEstoqueLote.from(dto, lote);
     * mov.setCustoPorUnidadeBase(...); // se aplicável
     * </pre>
     *
     * @param dto  DTO contendo os dados da movimentação (tipo, quantidade, motivo)
     * @param lote Lote de matéria-prima ao qual a movimentação pertence
     * @return Nova instância de MovimentacaoEstoqueLote
     */
    public static MovimentacaoEstoqueLote from(MovimentacaoRequestDTO dto, LoteMateriaPrima lote) {
        return MovimentacaoEstoqueLote.builder()
                .lote(lote)
                .tipo(dto.getTipo())
                .quantidade(dto.getQuantidade())
                .motivo(dto.getMotivo())
                .build();
    }

    /**
     * Atualiza os campos da movimentação de estoque a partir do DTO de request e do lote informado.
     * <p>
     * <b>Centraliza regras de negócio de atualização de movimentação de estoque.</b>
     * <ul>
     *   <li>Deve ser utilizado exclusivamente pela camada de service.</li>
     *   <li>Recebe o DTO de request validado e o lote de matéria-prima.</li>
     *   <li>Não altera o campo {@code custoPorUnidadeBase}, que deve ser atualizado manualmente se necessário.</li>
     * </ul>
     * <p>
     * <b>Exemplo de uso:</b>
     * <pre>
     * movimentacao.updateFrom(dto, lote);
     * movimentacao.setCustoPorUnidadeBase(...); // se aplicável
     * </pre>
     *
     * @param dto  DTO contendo os dados da movimentação (tipo, quantidade, motivo)
     * @param lote Lote de matéria-prima ao qual a movimentação pertence
     */
    public void updateFrom(MovimentacaoRequestDTO dto, LoteMateriaPrima lote) {
        this.lote = lote;
        this.tipo = dto.getTipo();
        this.quantidade = dto.getQuantidade();
        this.motivo = dto.getMotivo();
    }
}
