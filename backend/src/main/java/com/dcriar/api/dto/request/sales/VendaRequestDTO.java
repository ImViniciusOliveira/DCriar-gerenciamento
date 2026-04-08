package com.dcriar.api.dto.request.sales;

import com.dcriar.api.jackson.HumanTextDeserializer;
import com.dcriar.api.jackson.TrimStringDeserializer;
import com.dcriar.api.validation.annotation.ValidVendaRequest;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import lombok.*;

import java.util.List;

/**
 * Data Transfer Object (DTO) para registrar uma nova Venda.
 * <p>
 * Este DTO é utilizado para receber os dados de uma nova venda, incluindo o canal
 * onde a transação ocorreu e a lista de itens vendidos. A validação dos dados é
 * garantida pela anotação customizada {@link ValidVendaRequest}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ValidVendaRequest
public class VendaRequestDTO {

    /**
     * O ID do canal de venda onde a transação ocorreu.
     */
    @Schema(description = "O ID do canal de venda onde a transação ocorreu.", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long canalVendaId;

    @Schema(description = "Nome completo do cliente associado à venda.", example = "Maria da Silva")
    @JsonDeserialize(using = HumanTextDeserializer.class)
    private String nomeCompleto;

    @Schema(description = "Apelido ou nome curto do cliente.", example = "Maria")
    @JsonDeserialize(using = HumanTextDeserializer.class)
    private String apelido;

    @Schema(description = "Endereço do cliente.", example = "Rua das Flores")
    @JsonDeserialize(using = HumanTextDeserializer.class)
    private String endereco;

    @Schema(description = "Número do endereço.", example = "123A")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String numero;

    @Schema(description = "Bairro do cliente.", example = "Centro")
    @JsonDeserialize(using = HumanTextDeserializer.class)
    private String bairro;

    @Schema(description = "Cidade do cliente.", example = "São Paulo")
    @JsonDeserialize(using = HumanTextDeserializer.class)
    private String cidade;

    @Schema(description = "Estado do cliente.", example = "São Paulo")
    @JsonDeserialize(using = HumanTextDeserializer.class)
    private String estado;

    @Schema(description = "CEP do cliente.", example = "01001-000")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String cep;

    @Schema(description = "CPF do cliente.", example = "123.456.789-09")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String cpf;

    @Schema(description = "Observação livre sobre a venda ou cliente.", example = "Entregar no período da tarde.")
    @JsonDeserialize(using = TrimStringDeserializer.class)
    private String observacao;

    /**
     * A lista de itens que compõem a venda.
     * <p>
     * A anotação {@code @Valid} garante que cada item da lista seja validado individualmente.
     */
    @Schema(description = "A lista de itens que compõem a venda.", example = "[{\"produtoId\":1,\"quantidade\":100}]", requiredMode = Schema.RequiredMode.REQUIRED)
    private List<@Valid ItemVendaRequestDTO> itens;
}
