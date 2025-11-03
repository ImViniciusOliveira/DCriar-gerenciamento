package com.dcriar.domain.production.service;

import com.dcriar.api.dto.request.production.MargensRequestDTO;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.production.model.ParametrosCorte;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;

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
}
