package com.dcriar.domain.sales.entity;

import com.dcriar.domain.product.entity.CanalVenda;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa o "cabeçalho" de uma Venda realizada.
 * <p>
 * Esta entidade agrupa todos os itens de uma única transação e armazena
 * informações gerais como a data, o canal de venda e o valor total.
 */
@Entity
@Table(name = "vendas")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"itens"})
@EqualsAndHashCode(of = "id")
public class Venda {

    /**
     * O ID único da venda.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * A data e hora em que a venda foi registrada.
     * Gerado automaticamente no momento da criação.
     */
    @CreationTimestamp
    @Column(name = "data_venda", nullable = false, updatable = false)
    private OffsetDateTime dataVenda;

    /**
     * O canal de venda onde esta transação ocorreu.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "canal_venda_id", nullable = false)
    private CanalVenda canalVenda;

    /**
     * O valor total da venda, somando os preços de todos os itens.
     */
    @Column(name = "valor_total", nullable = false, precision = 19, scale = 2)
    private BigDecimal valorTotal;

    /**
     * A lista de itens que compõem esta venda.
     * A relação é bidirecional, e o {@code mappedBy} aponta para o campo 'venda' na entidade ItemVenda.
     * O {@code CascadeType.ALL} garante que operações como persistência e remoção se propaguem para os itens.
     * O {@code orphanRemoval = true} garante que itens removidos da lista sejam deletados do banco de dados.
     */
    @OneToMany(mappedBy = "venda", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private List<ItemVenda> itens = new ArrayList<>();

    /**
     * Método auxiliar para adicionar um item à venda, garantindo a consistência
     * da relação bidirecional.
     *
     * @param item O ItemVenda a ser adicionado.
     */
    public void addItem(ItemVenda item) {
        this.itens.add(item);
        item.setVenda(this);
    }

    /**
     * Cria uma instância de Venda a partir dos dados informados, centralizando regras de negócio de criação.
     * <p>
     * Este método deve ser utilizado pela service para garantir consistência e aplicar validações extras.
     * CanalVenda e ItemVenda devem ser resolvidos previamente na service.
     *
     * @param canalVenda Canal de venda resolvido
     * @param itens Lista de itens da venda já convertidos
     * @return Venda criada
     */
    public static Venda from(CanalVenda canalVenda, List<ItemVenda> itens) {
        Venda venda = Venda.builder()
                .canalVenda(canalVenda)
                .itens(new ArrayList<>())
                .build();
        if (itens != null) {
            for (ItemVenda item : itens) {
                venda.addItem(item);
            }
        }
        // O valor total deve ser calculado e atribuído na service
        return venda;
    }

    /**
     * Atualiza os campos da Venda existente a partir dos dados informados, centralizando regras de negócio de atualização.
     * <p>
     * Este método deve ser utilizado pela service para garantir consistência e aplicar validações extras.
     * CanalVenda e ItemVenda devem ser resolvidos previamente na service.
     *
     * @param canalVenda Canal de venda resolvido
     * @param itens Lista de itens da venda já convertidos
     */
    public void updateFrom(CanalVenda canalVenda, List<ItemVenda> itens) {
        this.canalVenda = canalVenda;
        this.itens.clear();
        if (itens != null) {
            for (ItemVenda item : itens) {
                this.addItem(item);
            }
        }
    }
}
