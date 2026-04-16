package com.dcriar.api.support;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Catálogo central de nomes técnicos de parâmetros/campos da API para labels amigáveis ao usuário.
 * <p>
 * A API continua usando nomes técnicos internamente, mas mensagens de erro e validação podem
 * consumir esta classe para expor um texto mais compreensível ao cliente.
 */
public final class ApiFieldLabels {

    private static final Pattern CAMEL_CASE_BOUNDARY = Pattern.compile("(?<=[a-záàâãéèêíìîóòôõúùûç])(?=[A-Z])");

    private static final Map<String, String> EXPLICIT_LABELS = Map.ofEntries(
            Map.entry("id", "id"),
            Map.entry("produtoId", "produto"),
            Map.entry("produtoIds", "produtos"),
            Map.entry("canalVendaId", "canal de venda"),
            Map.entry("canalVendaIds", "canais de venda"),
            Map.entry("canalId", "canal de venda"),
            Map.entry("canalIds", "canais de venda"),
            Map.entry("loteId", "lote"),
            Map.entry("loteIds", "lotes"),
            Map.entry("ordemDeProducaoId", "ordem de produção"),
            Map.entry("ordemDeProducaoIds", "ordens de produção"),
            Map.entry("vendaId", "venda"),
            Map.entry("vendaIds", "vendas"),
            Map.entry("precoId", "preço"),
            Map.entry("nome", "nome"),
            Map.entry("nomeProduto", "produto"),
            Map.entry("nomeCanalVenda", "canal de venda"),
            Map.entry("nomeMateriaPrima", "matéria-prima"),
            Map.entry("nomeTipoMateriaPrima", "matéria-prima"),
            Map.entry("tipoMateriaPrimaId", "matéria-prima"),
            Map.entry("sku", "SKU"),
            Map.entry("cpf", "CPF"),
            Map.entry("cep", "CEP"),
            Map.entry("uf", "UF"),
            Map.entry("periodo", "período"),
            Map.entry("sort", "ordenação"),
            Map.entry("page", "página"),
            Map.entry("size", "quantidade por página"),
            Map.entry("tipoProduto", "tipo de produto"),
            Map.entry("tipoMovimentacao", "tipo de movimentação"),
            Map.entry("tipoEstrutural", "tipo estrutural"),
            Map.entry("unidadeDeMedida", "unidade de medida"),
            Map.entry("unidadeDeConsumo", "unidade de consumo"),
            Map.entry("unidadeDeEstoque", "unidade de estoque"),
            Map.entry("unidadeCadastroConsumo", "unidade de cadastro de consumo"),
            Map.entry("quantidade", "quantidade"),
            Map.entry("quantidadeInicial", "quantidade inicial"),
            Map.entry("quantidadeNoCanal", "quantidade no canal"),
            Map.entry("motivo", "motivo"),
            Map.entry("direcao", "direção"),
            Map.entry("descricao", "descrição"),
            Map.entry("precoComercial", "preço comercial"),
            Map.entry("codigoFabricante", "código do fabricante"),
            Map.entry("tipoMaterial", "tipo de material"),
            Map.entry("materiaPrimaId", "matéria-prima"),
            Map.entry("larguraMm", "largura"),
            Map.entry("comprimentoMm", "comprimento"),
            Map.entry("larguraCm", "largura"),
            Map.entry("comprimentoCm", "comprimento"),
            Map.entry("custoTotalLote", "custo total do lote"),
            Map.entry("valorAtualLote", "valor atual do lote"),
            Map.entry("custoUnitarioAtual", "custo unitário atual"),
            Map.entry("estoqueCritico", "estoque crítico"),
            Map.entry("skuProduto", "SKU"),
            Map.entry("saldoAtual", "saldo atual"),
            Map.entry("saldoConsiderado", "saldo considerado"),
            Map.entry("percentualRisco", "percentual de risco"),
            Map.entry("statusAnalise", "status da análise"),
            Map.entry("faixaEstoque", "faixa de estoque"),
            Map.entry("politicaSaldoRetalho", "política de saldo de retalhos"),
            Map.entry("estoqueFisicoTotal", "estoque físico"),
            Map.entry("estoqueDistribuidoTotal", "estoque distribuído"),
            Map.entry("estoqueDisponivelParaAlocar", "estoque disponível"),
            Map.entry("dataInicio", "data inicial"),
            Map.entry("dataFim", "data final"),
            Map.entry("receita", "faturamento"),
            Map.entry("totalPedidos", "total de pedidos"),
            Map.entry("nomeCanal", "canal de venda"),
            Map.entry("unidadesVendidas", "unidades vendidas"),
            Map.entry("trendLabel", "tipo de agrupamento"),
            Map.entry("diasInformados", "dias informados"),
            Map.entry("limiteMaximoDias", "limite máximo de dias")
    );

    private ApiFieldLabels() {
    }

    public static String resolve(String technicalName) {
        if (technicalName == null || technicalName.isBlank()) {
            return "";
        }

        String normalized = technicalName.trim();
        String explicit = EXPLICIT_LABELS.get(normalized);
        if (explicit != null) {
            return explicit;
        }

        return humanize(normalized);
    }

    private static String humanize(String technicalName) {
        String prepared = technicalName
                .replace('[', ' ')
                .replace(']', ' ')
                .replace('.', ' ')
                .replace('_', ' ')
                .replace('-', ' ');

        prepared = prepared
                .replaceAll("Ids\\b", " ids")
                .replaceAll("Id\\b", " id");

        prepared = CAMEL_CASE_BOUNDARY.matcher(prepared).replaceAll(" ");
        prepared = prepared.replaceAll("\\s+", " ").trim();

        if (prepared.isBlank()) {
            return technicalName;
        }

        String lowered = prepared.toLowerCase(Locale.ROOT);
        return switch (lowered) {
            case "sku" -> "SKU";
            case "cpf" -> "CPF";
            case "cep" -> "CEP";
            case "uf" -> "UF";
            default -> lowered;
        };
    }
}
