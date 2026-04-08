package com.dcriar.api.hateoas.sales.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.springframework.hateoas.RepresentationModel;
import org.springframework.hateoas.server.core.Relation;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.dcriar.domain.sales.entity.enums.ModoLocalidadeVenda;

/**
 * Modelo de representação HATEOAS para uma Venda.
 * <p>
 * Este modelo expõe os dados de uma venda e inclui links para recursos relacionados,
 * seguindo os princípios do HATEOAS.
 */
@Getter
@Setter
@JsonRootName(value = "venda")
@Relation(collectionRelation = "vendas", itemRelation = "venda")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class VendaModel extends RepresentationModel<VendaModel> {

    @Schema(description = "ID único da venda.", example = "1")
    private Long id;

    @Schema(description = "ID do canal de venda.", example = "1")
    private Long canalVendaId;

    @Schema(description = "Nome do canal onde a venda ocorreu.", example = "LOJA_FISICA")
    private String nomeCanalVenda;

    @Schema(description = "Valor total da venda.", example = "125.50")
    private BigDecimal valorTotal;

    @Schema(description = "Nome completo do cliente associado à venda.", example = "Maria da Silva")
    private String nomeCompleto;

    @Schema(description = "País do cliente.", example = "Brasil")
    private String pais;

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

    @Schema(description = "Modo de localidade que o frontend deve usar.", example = "BRASIL")
    private ModoLocalidadeVenda modoLocalidade;

    @Schema(description = "Lista de itens que compõem a venda.")
    private List<ItemVendaModel> itens;

    @Schema(description = "Data e hora de criação da venda.")
    private LocalDateTime dataCriacao;

    @Schema(description = "Data e hora da última atualização da venda.")
    private LocalDateTime dataAtualizacao;
}
