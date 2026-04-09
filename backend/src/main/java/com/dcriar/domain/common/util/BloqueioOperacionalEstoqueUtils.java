package com.dcriar.domain.common.util;

import com.dcriar.domain.common.model.CamposBloqueadosInfo;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Regras compartilhadas de bloqueio operacional para recursos de estoque.
 * <p>
 * O objetivo é expor ao frontend, e reaproveitar no backend, quando um recurso
 * está em estado inconsistente e só deve aceitar ações de saneamento.
 */
public final class BloqueioOperacionalEstoqueUtils {

    public static final String ACAO_AJUSTE_FISICO_NEGATIVO = "ajusteFisicoNegativo";
    public static final String ACAO_ALOCAR_CANAL = "alocarCanal";
    public static final String ACAO_USAR_EM_PRODUCAO = "usarEmProducao";
    public static final String ACAO_AJUSTE_CANAL_POSITIVO = "ajusteCanalPositivo";
    public static final String ACAO_AJUSTE_CANAL_NEGATIVO = "ajusteCanalNegativo";
    public static final String ACAO_AJUSTE_LOTE_NEGATIVO = "ajusteLoteNegativo";
    public static final String ACAO_PERDA_DESCARTE = "perdaDescarte";

    private static final String MOTIVO_PRODUTO_ESTOQUE_FISICO_NEGATIVO =
            "Produto com estoque fisico negativo. Regularize com ajuste fisico positivo antes de continuar.";
    private static final String MOTIVO_PRODUTO_DISTRIBUICAO_INCONSISTENTE =
            "Produto com distribuicao acima do estoque fisico. Reduza a distribuicao ou saneie o estoque fisico antes de continuar.";
    private static final String MOTIVO_CANAL_NEGATIVO =
            "Canal com estoque negativo. Regularize com ajuste positivo antes de continuar.";
    private static final String MOTIVO_CANAL_SEM_SALDO =
            "Canal sem saldo disponivel para retirada.";
    private static final String MOTIVO_LOTE_NEGATIVO =
            "Lote com saldo negativo. Regularize com entrada ou ajuste positivo antes de continuar.";

    private BloqueioOperacionalEstoqueUtils() {
    }

    public static CamposBloqueadosInfo resolverBloqueiosOperacionaisProduto(
            Integer estoqueFisicoTotal,
            Integer estoqueDistribuidoTotal,
            Integer estoqueDisponivelParaAlocar
    ) {
        Map<String, String> bloqueios = new LinkedHashMap<>();
        int fisico = estoqueFisicoTotal != null ? estoqueFisicoTotal : 0;
        int distribuido = estoqueDistribuidoTotal != null ? estoqueDistribuidoTotal : 0;
        int disponivel = estoqueDisponivelParaAlocar != null ? estoqueDisponivelParaAlocar : 0;

        if (fisico < 0) {
            bloqueios.put(ACAO_AJUSTE_FISICO_NEGATIVO, "Nao e permitido aprofundar saldo fisico negativo.");
            bloqueios.put(ACAO_ALOCAR_CANAL, MOTIVO_PRODUTO_ESTOQUE_FISICO_NEGATIVO);
            bloqueios.put(ACAO_USAR_EM_PRODUCAO, MOTIVO_PRODUTO_ESTOQUE_FISICO_NEGATIVO);
        }

        if (distribuido > fisico || disponivel < 0) {
            bloqueios.putIfAbsent(ACAO_AJUSTE_FISICO_NEGATIVO,
                    "Nao e permitido reduzir ainda mais o estoque fisico enquanto o produto estiver inconsistente.");
            bloqueios.putIfAbsent(ACAO_ALOCAR_CANAL, MOTIVO_PRODUTO_DISTRIBUICAO_INCONSISTENTE);
            bloqueios.putIfAbsent(ACAO_USAR_EM_PRODUCAO, MOTIVO_PRODUTO_DISTRIBUICAO_INCONSISTENTE);
        }

        return bloqueios.isEmpty() ? CamposBloqueadosInfo.vazio() : CamposBloqueadosInfo.fromMotivos(bloqueios);
    }

    public static CamposBloqueadosInfo resolverBloqueiosOperacionaisCanal(
            Integer quantidadeNoCanal,
            Integer estoqueFisicoTotal,
            Integer estoqueDistribuidoTotal,
            Integer estoqueDisponivelParaAlocar
    ) {
        Map<String, String> bloqueios = new LinkedHashMap<>();
        int quantidade = quantidadeNoCanal != null ? quantidadeNoCanal : 0;

        CamposBloqueadosInfo bloqueiosProduto = resolverBloqueiosOperacionaisProduto(
                estoqueFisicoTotal,
                estoqueDistribuidoTotal,
                estoqueDisponivelParaAlocar
        );

        if (bloqueiosProduto.contemCampo(ACAO_ALOCAR_CANAL)) {
            bloqueios.put(ACAO_AJUSTE_CANAL_POSITIVO, bloqueiosProduto.motivosBloqueio().get(ACAO_ALOCAR_CANAL));
        }

        if (quantidade < 0) {
            bloqueios.put(ACAO_AJUSTE_CANAL_NEGATIVO, MOTIVO_CANAL_NEGATIVO);
        } else if (quantidade == 0) {
            bloqueios.put(ACAO_AJUSTE_CANAL_NEGATIVO, MOTIVO_CANAL_SEM_SALDO);
        }

        return bloqueios.isEmpty() ? CamposBloqueadosInfo.vazio() : CamposBloqueadosInfo.fromMotivos(bloqueios);
    }

    public static CamposBloqueadosInfo resolverBloqueiosOperacionaisLote(BigDecimal saldoAtual) {
        Map<String, String> bloqueios = new LinkedHashMap<>();
        BigDecimal saldo = saldoAtual != null ? saldoAtual : BigDecimal.ZERO;

        if (saldo.compareTo(BigDecimal.ZERO) < 0) {
            bloqueios.put(ACAO_AJUSTE_LOTE_NEGATIVO, "Nao e permitido aprofundar saldo negativo do lote.");
            bloqueios.put(ACAO_PERDA_DESCARTE, MOTIVO_LOTE_NEGATIVO);
            bloqueios.put(ACAO_USAR_EM_PRODUCAO, MOTIVO_LOTE_NEGATIVO);
        }

        return bloqueios.isEmpty() ? CamposBloqueadosInfo.vazio() : CamposBloqueadosInfo.fromMotivos(bloqueios);
    }
}
