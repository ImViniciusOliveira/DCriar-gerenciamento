package com.dcriar.domain.stock.service;

import com.dcriar.domain.production.entity.OrdemDeProducao;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.model.LoteRetalhoHierarchyItem;

import java.util.List;

/**
 * Serviço central para navegar e validar a árvore de retalhos derivada de lotes e ordens de produção.
 * Centraliza a recursão para evitar regras divergentes entre produção, lote e ajuste.
 */
public interface LoteRetalhoHierarchyService {

    List<LoteRetalhoHierarchyItem> listarFilhosDiretos(LoteMateriaPrima lote);

    List<LoteRetalhoHierarchyItem> listarDescendentes(LoteMateriaPrima lote);

    List<LoteRetalhoHierarchyItem> listarRetalhosDaOrdemRecursivamente(OrdemDeProducao ordem);

    boolean existeDescendenteComMovimentacaoDeSaida(LoteMateriaPrima lote);

    boolean existeRetalhoDaOrdemComMovimentacaoDeSaida(OrdemDeProducao ordem);
}
