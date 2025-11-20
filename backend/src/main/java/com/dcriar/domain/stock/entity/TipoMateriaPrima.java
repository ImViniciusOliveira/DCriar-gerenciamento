package com.dcriar.domain.stock.entity;

import com.dcriar.api.dto.request.stock.TipoMateriaPrimaRequestDTO;
import com.dcriar.domain.common.entity.AuditableEntity;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import jakarta.persistence.*;
import lombok.*;

/**
 * Representa um tipo abstrato de matéria-prima no sistema (o item de catálogo).
 * <p>
 * Esta entidade define as características gerais de um insumo, como seu nome
 * e a unidade em que ele é consumido nas "receitas" dos produtos.
 */
@Entity
@Table(name = "tipos_materia_prima")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder(toBuilder = true)
@ToString
@EqualsAndHashCode(of = "id", callSuper = false)
public class TipoMateriaPrima extends AuditableEntity {

    /**
     * O ID único do tipo de matéria-prima.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * O nome único do tipo de matéria-prima, servindo como uma chave de negócio.
     */
    @Column(nullable = false, unique = true, length = 150)
    private String nome;

    /**
     * Define como a "receita" de um produto mede o consumo deste material.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private UnidadeDeMedida unidadeDeConsumo;

    /**
     * Cria uma instância de TipoMateriaPrima a partir do DTO de request, centralizando regras de negócio de criação.
     * <p>
     * Este método deve ser utilizado pela camada de service para garantir que toda lógica de normalização,
     * validação extra e defaults seja aplicada de forma consistente.
     *
     * @param dto DTO de request com os dados para criação
     * @return Nova instância de TipoMateriaPrima
     */
    public static TipoMateriaPrima from(TipoMateriaPrimaRequestDTO dto) {
        // Centralize regras de negócio aqui (ex: normalização, validação extra)
        return TipoMateriaPrima.builder()
                .nome(dto.getNome() != null ? dto.getNome().trim() : null)
                .unidadeDeConsumo(dto.getUnidadeDeConsumo())
                .build();
    }

    /**
     * Atualiza os campos da entidade a partir do DTO de request, centralizando regras de negócio de atualização.
     * <p>
     * Este método deve ser utilizado pela camada de service para garantir que toda lógica de atualização
     * seja aplicada de forma consistente.
     *
     * @param dto DTO de request com os dados para atualização
     */
    public void updateFrom(TipoMateriaPrimaRequestDTO dto) {
        // Centralize regras de negócio para atualização
        if (dto.getNome() != null) {
            this.nome = dto.getNome().trim();
        }
        if (dto.getUnidadeDeConsumo() != null) {
            this.unidadeDeConsumo = dto.getUnidadeDeConsumo();
        }
    }
}
