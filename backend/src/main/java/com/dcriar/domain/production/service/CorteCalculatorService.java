package com.dcriar.domain.production.service;

import com.dcriar.api.dto.request.production.MargensRequestDTO;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.production.model.ParametrosCorte;
import com.dcriar.domain.production.model.ResumoLayoutCorte;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;

import java.math.BigDecimal;

/**
 * Interface responsável por realizar os cálculos geométricos para o planejamento de ordens de corte.
 * Define o contrato para extração dos parâmetros essenciais para a execução do corte.
 */
public interface CorteCalculatorService {
    /**
     * Extrai os parâmetros de corte otimizados para uma dada produção.
     * <p>
     * O método calcula a melhor forma de arranjar as peças no lote de matéria-prima,
     * testando a orientação normal e a rotacionada (90 graus) do produto. A orientação
     * que resultar no menor consumo de comprimento linear do lote será a escolhida.
     *
     * @param quantidade A quantidade de produtos a serem produzidos.
     * @param produto O produto a ser cortado.
     * @param lotePrincipal O lote de matéria-prima a ser utilizado.
     * @param margensRequest As margens de segurança a serem aplicadas no corte.
     * @return um objeto {@link ParametrosCorte} contendo os dados calculados para o layout de corte mais eficiente.
     */
    ParametrosCorte extrairParametrosCorte(
            int quantidade,
            Produto produto,
            LoteMateriaPrima lotePrincipal,
            MargensRequestDTO margensRequest
    );

    /**
     * Extrai parâmetros de corte para modo manual, onde o usuário define as dimensões do corte.
     * <p>
     * No modo manual, não há rotação automática nem margens. O sistema apenas calcula
     * o retalho lateral baseado na diferença entre o lote e o corte definido pelo usuário.
     *
     * @param quantidade A quantidade de produtos a serem produzidos.
     * @param produto O produto a ser cortado.
     * @param lotePrincipal O lote de matéria-prima a ser utilizado.
     * @param larguraCorteManualCm A largura do corte definida manualmente pelo usuário.
     * @param comprimentoCorteManualCm O comprimento do corte definido manualmente pelo usuário.
     * @return um objeto {@link ParametrosCorte} contendo os dados para o corte manual.
     */
    ParametrosCorte extrairParametrosCorteManual(
            int quantidade,
            Produto produto,
            LoteMateriaPrima lotePrincipal,
            BigDecimal larguraCorteManualCm,
            BigDecimal comprimentoCorteManualCm
    );

    /**
     * Realiza o cálculo detalhado do layout de corte, simulando a disposição física das peças
     * e identificando as sobras (retalhos) geradas.
     *
     * @param parametros Os parâmetros de corte já calculados e otimizados.
     * @param ordemComprimentoFinalCm O comprimento total de matéria-prima que será consumido.
     * @param isModoManual Se true, não gera retalho inferior (R2); se false, gera normalmente.
     * @return um objeto {@link ResumoLayoutCorte} contendo a lista detalhada de cortes e o resumo do layout.
     */
    ResumoLayoutCorte calcularLayoutDetalhado(ParametrosCorte parametros, BigDecimal ordemComprimentoFinalCm, boolean isModoManual);
}
