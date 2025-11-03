package com.dcriar.domain.production.entity;

import com.dcriar.api.dto.request.production.CorteRealizadoRequestDTO;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * Representa um corte realizado em uma ordem de produção.
 * <p>
 * Cada registro armazena as dimensões, quantidade e tipo do corte (produto ou retalho).
 * Os métodos from e updateFrom centralizam regras de negócio para criação e atualização a partir do DTO.
 */
@Entity
@Table(name = "cortes_realizados")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
@ToString
@EqualsAndHashCode(of = "id")
public class CorteRealizado {
    /**
     * ID único do corte realizado.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Ordem de produção associada ao corte.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ordem_de_producao_id", nullable = false)
    private OrdemDeProducao ordemDeProducao;

    /**
     * Largura do corte em centímetros.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal larguraCm;

    /**
     * Comprimento do corte em centímetros.
     */
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal comprimentoCm;

    /**
     * Quantidade de peças produzidas com este corte.
     */
    @Column(nullable = false)
    private int quantidade;

    /**
     * Tipo do corte: "PRODUTO" ou "RETALHO".
     */
    @Column(nullable = false, length = 20)
    private String tipo;

    /**
     * Categoria do retalho quando {@code tipo == "RETALHO"}.
     * Valores sugeridos: "LATERAL", "FINAL". Nulo para produtos.
     */
    @Column(length = 20)
    private String retalhoCategoria;

    /**
     * Cria uma instância de CorteRealizado a partir do DTO de request.
     * <p>
     * Centraliza regras de negócio de criação.
     *
     * @param dto DTO de request
     * @param ordemDeProducao Ordem de produção associada
     * @return Nova instância de CorteRealizado
     */
    public static CorteRealizado from(CorteRealizadoRequestDTO dto, OrdemDeProducao ordemDeProducao) {
        return CorteRealizado.builder()
                .ordemDeProducao(ordemDeProducao)
                .larguraCm(dto.getLarguraCm())
                .comprimentoCm(dto.getComprimentoCm())
                .quantidade(dto.getQuantidade())
                .tipo(dto.getTipo())
                .retalhoCategoria(dto.getRetalhoCategoria())
                .build();
    }

    /**
     * Atualiza os campos da entidade CorteRealizado a partir do DTO de request.
     * <p>
     * Centraliza regras de negócio de atualização.
     *
     * @param dto DTO de request
     * @param ordemDeProducao Ordem de produção associada
     */
    public void updateFrom(CorteRealizadoRequestDTO dto, OrdemDeProducao ordemDeProducao) {
        this.ordemDeProducao = ordemDeProducao;
        this.larguraCm = dto.getLarguraCm();
        this.comprimentoCm = dto.getComprimentoCm();
        this.quantidade = dto.getQuantidade();
        this.tipo = dto.getTipo();
        this.retalhoCategoria = dto.getRetalhoCategoria();
    }
}
