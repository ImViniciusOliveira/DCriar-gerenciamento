package com.dcriar.domain.product.entity;

import com.dcriar.api.dto.request.product.CanalVendaRequestDTO;
import com.dcriar.domain.common.entity.AuditableEntity;
import com.dcriar.domain.common.util.HumanTextNormalizer;
import jakarta.persistence.*;
import lombok.*;

/**
 * Representa um canal de venda onde o estoque de produtos acabados é gerido.
 * <p>
 * Exemplos: Loja Física, Shopee, Mercado Livre.
 */
@Entity
@Table(name = "canais_venda")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@EqualsAndHashCode(of = "id", callSuper = false)
public class CanalVenda extends AuditableEntity {

    /**
     * O ID único do canal de venda.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * O nome único do canal de venda (ex: "Shopee", "Mercado Livre", "Loja Fisica").
     */
    @Column(nullable = false, unique = true, length = 50)
    private String nome;

    /**
     * Cria uma instância de {@link CanalVenda} a partir de um DTO de request.
     * <p>
     * Centraliza regras de negócio de criação, como normalização e validações extras.
     * Deve ser utilizado pela camada de serviço.
     *
     * @param dto DTO de request contendo os dados para criação do canal de venda
     * @return Nova instância de {@link CanalVenda} pronta para persistência
     */
    public static CanalVenda from(CanalVendaRequestDTO dto) {
        return CanalVenda.builder()
                .nome(HumanTextNormalizer.normalize(dto.getNome()))
                .build();
    }

    /**
     * Atualiza os campos da entidade a partir de um DTO de request.
     * <p>
     * Centraliza regras de negócio de atualização, garantindo que toda lógica relacionada à edição
     * fique encapsulada na entidade. Apenas campos presentes no DTO serão atualizados.
     * Deve ser utilizado pela camada de serviço.
     *
     * @param dto DTO de request contendo os dados para atualização
     */
    public void updateFrom(CanalVendaRequestDTO dto) {
        if (dto.getNome() != null) {
            this.nome = HumanTextNormalizer.normalize(dto.getNome());
        }
    }
}
