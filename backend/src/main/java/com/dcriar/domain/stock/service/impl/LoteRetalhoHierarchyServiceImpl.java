package com.dcriar.domain.stock.service.impl;

import com.dcriar.domain.production.entity.OrdemDeProducao;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.entity.MovimentacaoEstoqueLote;
import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import com.dcriar.domain.stock.model.LoteRetalhoHierarchyItem;
import com.dcriar.domain.stock.repository.LoteMateriaPrimaRepository;
import com.dcriar.domain.stock.service.LoteRetalhoHierarchyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Implementação central da navegação recursiva de retalhos.
 */
@Service
@RequiredArgsConstructor
public class LoteRetalhoHierarchyServiceImpl implements LoteRetalhoHierarchyService {

    private final LoteMateriaPrimaRepository loteMateriaPrimaRepository;

    @Override
    @Transactional(readOnly = true)
    public List<LoteRetalhoHierarchyItem> listarFilhosDiretos(LoteMateriaPrima lote) {
        return loteMateriaPrimaRepository.findByLoteDeOrigem(lote).stream()
                .map(filho -> criarItem(filho, 1, List.of(lote.getId(), filho.getId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteRetalhoHierarchyItem> listarDescendentes(LoteMateriaPrima lote) {
        List<LoteRetalhoHierarchyItem> descendentes = new ArrayList<>();
        Set<Long> visitados = new HashSet<>();
        visitados.add(lote.getId());

        for (LoteRetalhoHierarchyItem filhoDireto : listarFilhosDiretos(lote)) {
            if (!visitados.add(filhoDireto.lote().getId())) {
                continue;
            }
            descendentes.add(filhoDireto);
            adicionarDescendentesRecursivamente(filhoDireto.lote(), filhoDireto.nivel(), filhoDireto.caminhoIds(), descendentes, visitados);
        }

        return descendentes;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteRetalhoHierarchyItem> listarRetalhosDaOrdemRecursivamente(OrdemDeProducao ordem) {
        List<LoteRetalhoHierarchyItem> descendentes = new ArrayList<>();
        Set<Long> visitados = new HashSet<>();

        List<LoteMateriaPrima> retalhosRaiz = loteMateriaPrimaRepository.findByOrdemDeProducaoOrigem(ordem);
        for (LoteMateriaPrima retalhoRaiz : retalhosRaiz) {
            if (!visitados.add(retalhoRaiz.getId())) {
                continue;
            }

            LoteRetalhoHierarchyItem itemRaiz = criarItem(
                    retalhoRaiz,
                    1,
                    retalhoRaiz.getLoteDeOrigem() != null
                            ? List.of(retalhoRaiz.getLoteDeOrigem().getId(), retalhoRaiz.getId())
                            : List.of(retalhoRaiz.getId())
            );
            descendentes.add(itemRaiz);
            adicionarDescendentesRecursivamente(retalhoRaiz, itemRaiz.nivel(), itemRaiz.caminhoIds(), descendentes, visitados);
        }

        return descendentes;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeDescendenteComAlteracaoAtiva(LoteMateriaPrima lote) {
        return listarDescendentes(lote).stream()
                .anyMatch(LoteRetalhoHierarchyItem::possuiAlteracaoAtiva);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeRetalhoDaOrdemComAlteracaoAtiva(OrdemDeProducao ordem) {
        return listarRetalhosDaOrdemRecursivamente(ordem).stream()
                .anyMatch(LoteRetalhoHierarchyItem::possuiAlteracaoAtiva);
    }

    private void adicionarDescendentesRecursivamente(
            LoteMateriaPrima lotePai,
            int nivelPai,
            List<Long> caminhoPai,
            List<LoteRetalhoHierarchyItem> acumulador,
            Set<Long> visitados
    ) {
        for (LoteMateriaPrima filho : loteMateriaPrimaRepository.findByLoteDeOrigem(lotePai)) {
            if (!visitados.add(filho.getId())) {
                continue;
            }

            List<Long> caminhoFilho = new ArrayList<>(caminhoPai);
            caminhoFilho.add(filho.getId());
            LoteRetalhoHierarchyItem item = criarItem(filho, nivelPai + 1, caminhoFilho);
            acumulador.add(item);

            adicionarDescendentesRecursivamente(filho, item.nivel(), item.caminhoIds(), acumulador, visitados);
        }
    }

    private LoteRetalhoHierarchyItem criarItem(LoteMateriaPrima lote, int nivel, List<Long> caminhoIds) {
        return new LoteRetalhoHierarchyItem(
                lote,
                nivel,
                lote.getLoteDeOrigem() != null ? lote.getLoteDeOrigem().getId() : null,
                lote.getOrdemDeProducaoOrigem() != null ? lote.getOrdemDeProducaoOrigem().getId() : null,
                List.copyOf(caminhoIds),
                possuiAlteracaoAtiva(lote)
        );
    }

    private boolean possuiAlteracaoAtiva(LoteMateriaPrima lote) {
        BigDecimal saldoOriginalDoRetalho = lote.getMovimentacoes().stream()
                .filter(movimentacao -> movimentacao.getTipo() == TipoMovimentacao.ENTRADA_SOBRA)
                .map(MovimentacaoEstoqueLote::getQuantidade)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (saldoOriginalDoRetalho.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        BigDecimal saldoAtual = lote.getSaldoAtual() != null
                ? lote.getSaldoAtual()
                : lote.getMovimentacoes().stream()
                .map(MovimentacaoEstoqueLote::getQuantidade)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return saldoAtual.compareTo(saldoOriginalDoRetalho) != 0;
    }
}
