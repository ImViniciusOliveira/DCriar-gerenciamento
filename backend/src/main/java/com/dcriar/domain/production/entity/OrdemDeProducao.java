package com.dcriar.domain.production.entity;

import com.dcriar.api.dto.request.production.OrdemDeProducaoRequestDTO;
import com.dcriar.domain.common.entity.AuditableEntity;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.production.enums.ModoCalculo;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Representa uma Ordem de Produção no sistema, detalhando a fabricação de um produto.
 * <p>
 * Esta entidade é genérica e pode representar diferentes tipos de produção,
 * desde cortes geométricos de materiais até o consumo de insumos como líquidos ou pós.
 * Ela registra qual produto foi produzido, os lotes de matéria-prima consumidos e a quantidade produzida.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@ToString(exclude = {"cortesRealizados", "lotesConsumidos"})
@EqualsAndHashCode(of = "id", callSuper = false)
@Table(name = "ordens_de_producao")
public class OrdemDeProducao extends AuditableEntity {

    /**
     * O ID único da ordem de produção.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * O produto final que está sendo fabricado por esta ordem de produção.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    /**
     * Os lotes de matéria-prima consumidos nesta ordem de produção.
     * <p>
     * Uma produção pode consumir material de um ou mais lotes.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "ordem_producao_lotes_consumidos",
            joinColumns = @JoinColumn(name = "ordem_producao_id"),
            inverseJoinColumns = @JoinColumn(name = "lote_materia_prima_id")
    )
    @Builder.Default
    private Set<LoteMateriaPrima> lotesConsumidos = new HashSet<>();

    /**
     * O ID do canal de venda para o qual o estoque será destinado (opcional).
     */
    @Column(name = "canal_venda_destino_id")
    private Long canalVendaDestinoId;

    /**
     * A quantidade de unidades do produto final que foram produzidas com sucesso.
     */
    @Column(name = "quantidade_produzida", nullable = false)
    private Integer quantidadeProduzida;

    /**
     * O modo de cálculo utilizado, se aplicável (ex: para ordens de corte).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "modo_calculo", length = 20)
    private ModoCalculo modoCalculo;

    /**
     * As margens de segurança aplicadas, se aplicável (ex: para ordens de corte).
     */
    @Embedded
    private Margens margens;

    @OneToMany(mappedBy = "ordemDeProducao", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @Builder.Default
    private List<CorteRealizado> cortesRealizados = new ArrayList<>();

    /**
     * A largura final do corte em centímetros, se aplicável.
     */
    @Column(name = "largura_final_cm", precision = 10, scale = 2)
    private BigDecimal larguraFinalCm;

    /**
     * O comprimento final do corte em centímetros, se aplicável.
     */
    @Column(name = "comprimento_final_cm", precision = 10, scale = 2)
    private BigDecimal comprimentoFinalCm;

    /**
     * Um motivo, observação ou referência para a ordem de produção (ex: número do pedido do cliente).
     */
    @Column(name = "motivo")
    private String motivo;

    /**
     * Indica se a orientação do produto foi rotacionada para otimização do corte.
     */
    @Column(name = "rotacionado")
    private boolean rotacionado;

    /**
     * Cria uma instância de OrdemDeProducao a partir do DTO de request, centralizando regras de negócio de criação.
     * Utilize este método na service para garantir padronização e validações extras.
     *
     * @param dto DTO de request com os dados da ordem de produção
     * @param produto Produto já carregado da base
     * @param lotesConsumidos Set de LoteMateriaPrima já carregados da base
     * @param margens Margens de segurança para a ordem de produção
     * @return Nova instância de OrdemDeProducao
     */
    public static OrdemDeProducao from(OrdemDeProducaoRequestDTO dto, Produto produto, Set<LoteMateriaPrima> lotesConsumidos, Margens margens) {
        OrdemDeProducaoBuilder builder = OrdemDeProducao.builder()
                .produto(produto)
                .lotesConsumidos(lotesConsumidos)
                .canalVendaDestinoId(dto.getCanalVendaDestinoId())
                .quantidadeProduzida(dto.getQuantidadeProduzida())
                .motivo(dto.getMotivo());

        // Atribui campos específicos de CORTE apenas se existirem no DTO
        if (dto.getModoCalculo() != null) {
            builder.modoCalculo(ModoCalculo.valueOf(dto.getModoCalculo()));
        }
        if (margens != null) {
            builder.margens(margens);
        }
        if (dto.getLarguraFinalCm() != null) {
            builder.larguraFinalCm(dto.getLarguraFinalCm());
        }
        if (dto.getComprimentoFinalCm() != null) {
            builder.comprimentoFinalCm(dto.getComprimentoFinalCm());
        }
        // O getter para boolean primitivo é 'isRotacionado', que não pode ser nulo.
        // Apenas setamos se for explicitamente parte do DTO de corte.
        if (dto.getModoCalculo() != null) { // Usamos modoCalculo como indicador de que é uma ordem de corte
            builder.rotacionado(dto.isRotacionado());
        }

        return builder.build();
    }

    /**
     * Atualiza os campos da OrdemDeProducao a partir do DTO de request, centralizando regras de negócio de atualização.
     * Utilize este método na service para garantir padronização e validações extras.
     *
     * @param dto DTO de request com os dados atualizados
     * @param produto Produto já carregado da base
     * @param lotesConsumidos Set de LoteMateriaPrima já carregados da base
     * @param margens Margens de segurança para a ordem de produção
     */
    public void updateFrom(OrdemDeProducaoRequestDTO dto, Produto produto, Set<LoteMateriaPrima> lotesConsumidos, Margens margens) {
        this.produto = produto;
        this.lotesConsumidos = lotesConsumidos;
        this.canalVendaDestinoId = dto.getCanalVendaDestinoId();
        this.quantidadeProduzida = dto.getQuantidadeProduzida();
        this.modoCalculo = ModoCalculo.valueOf(dto.getModoCalculo());
        this.margens = margens;
        this.larguraFinalCm = dto.getLarguraFinalCm();
        this.comprimentoFinalCm = dto.getComprimentoFinalCm();
        this.motivo = dto.getMotivo();
        this.rotacionado = dto.isRotacionado();
    }

    /**
     * Adiciona um registro de corte a esta ordem de produção e estabelece a relação bidirecional.
     *
     * @param corte O corte a ser adicionado.
     */
    public void addCorteRealizado(CorteRealizado corte) {
        cortesRealizados.add(corte);
        corte.setOrdemDeProducao(this);
    }
}
