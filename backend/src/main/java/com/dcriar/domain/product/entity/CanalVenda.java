package com.dcriar.domain.product.entity;

import com.dcriar.api.dto.request.product.CanalVendaRequestDTO;
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
@EqualsAndHashCode(of = "id")
public class CanalVenda {

    /**
     * O ID único do canal de venda.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * O nome único do canal de venda (ex: "SHOPEE", "MERCADO_LIVRE", "LOJA_FISICA").
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
        // Centralize regras de negócio aqui (ex: normalização, validação extra)
        return CanalVenda.builder()
                .nome(dto.getNome() != null ? dto.getNome().trim().toUpperCase() : null)
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
        // Centralize regras de negócio para atualização
        if (dto.getNome() != null) {
            this.nome = dto.getNome().trim().toUpperCase();
        }
    }
}
