package com.dcriar.api.dto.response.sales;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Data Transfer Object (DTO) que representa a resposta de uma Venda finalizada.
 * <p>
 * Este DTO fornece uma representação completa da transação, incluindo o canal de venda,
 * o valor total e a lista detalhada de todos os itens vendidos.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendaResponseDTO {

    /**
     * O ID único da venda.
     */
    @Schema(description = "O ID único da venda.", example = "1")
    private Long id;

    /**
     * O ID do canal de venda. Necessário para operações de edição.
     */
    @Schema(description = "O ID do canal de venda.", example = "1")
    private Long canalVendaId;

    /**
     * O nome do canal de venda onde a transação ocorreu.
     */
    @Schema(description = "O nome do canal de venda onde a transação ocorreu.", example = "SHOPEE")
    private String nomeCanalVenda;

    /**
     * O valor total da venda, somando todos os itens.
     */
    @Schema(description = "O valor total da venda.", example = "99.90")
    private BigDecimal valorTotal;

    @Schema(description = "Nome completo do cliente associado à venda.", example = "Maria da Silva")
    private String nomeCompleto;

    @Schema(description = "Apelido ou nome curto do cliente.", example = "Maria")
    private String apelido;

    @Schema(description = "Endereço do cliente.", example = "Rua das Flores")
    private String endereco;

    @Schema(description = "Número do endereço.", example = "123A")
    private String numero;

    @Schema(description = "Bairro do cliente.", example = "Centro")
    private String bairro;

    @Schema(description = "Cidade do cliente.", example = "São Paulo")
    private String cidade;

    @Schema(description = "Estado do cliente.", example = "Sao Paulo")
    private String estado;

    @Schema(description = "CEP do cliente.", example = "01001000")
    private String cep;

    @Schema(description = "CPF do cliente.", example = "12345678909")
    private String cpf;

    @Schema(description = "Observação livre sobre a venda ou cliente.", example = "Entregar no período da tarde.")
    private String observacao;

    /**
     * A lista de itens que compõem a venda.
     */
    @Schema(description = "A lista de itens que foram vendidos nesta transação.")
    private List<ItemVendaResponseDTO> itens;

    /**
     * A data e hora em que a venda foi criada.
     */
    @Schema(description = "Data e hora de criação da venda.")
    private LocalDateTime dataCriacao;

    /**
     * A data e hora da última atualização da venda.
     */
    @Schema(description = "Data e hora da última atualização da venda.")
    private LocalDateTime dataAtualizacao;
}
