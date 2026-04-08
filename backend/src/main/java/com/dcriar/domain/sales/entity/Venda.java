package com.dcriar.domain.sales.entity;

import com.dcriar.domain.common.persistence.EncryptedStringAttributeConverter;
import com.dcriar.domain.common.entity.AuditableEntity;
import com.dcriar.domain.product.entity.CanalVenda;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Representa o "cabeçalho" de uma Venda realizada.
 * <p>
 * Esta entidade agrupa todos os itens de uma única transação e armazena
 * informações gerais como o canal de venda e o valor total.
 * As datas de criação e atualização são herdadas de {@link AuditableEntity}.
 */
@Entity
@Table(name = "vendas")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString(exclude = {"itens"})
@EqualsAndHashCode(of = "id", callSuper = false)
public class Venda extends AuditableEntity {

    /**
     * O ID único da venda.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    @Convert(converter = EncryptedStringAttributeConverter.class)
    @Column(name = "nome_completo")
    private String nomeCompleto;

    @Column(name = "pais", nullable = false, length = 120)
    private String pais;

    @Column(name = "apelido", length = 120)
    private String apelido;

    @Convert(converter = EncryptedStringAttributeConverter.class)
    @Column(name = "endereco")
    private String endereco;

    @Convert(converter = EncryptedStringAttributeConverter.class)
    @Column(name = "numero")
    private String numero;

    @Convert(converter = EncryptedStringAttributeConverter.class)
    @Column(name = "bairro")
    private String bairro;

    @Column(name = "cidade", length = 120)
    private String cidade;

    @Column(name = "estado", length = 60)
    private String estado;

    @Convert(converter = EncryptedStringAttributeConverter.class)
    @Column(name = "cep")
    private String cep;

    @Convert(converter = EncryptedStringAttributeConverter.class)
    @Column(name = "cpf")
    private String cpf;

    @Convert(converter = EncryptedStringAttributeConverter.class)
    @Column(name = "observacao")
    private String observacao;

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
