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
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.Comparator;

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
    public boolean possuiAlteracaoAtivaNoEstadoAtual(LoteMateriaPrima lote) {
        BigDecimal saldoOriginalRegistrado = obterSaldoOriginalRegistrado(lote);
        if (saldoOriginalRegistrado.compareTo(BigDecimal.ZERO) == 0) {
            return false;
        }

        BigDecimal saldoAtual = lote.getSaldoAtual() != null
                ? lote.getSaldoAtual()
                : lote.getMovimentacoes().stream()
                .map(MovimentacaoEstoqueLote::getQuantidade)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return saldoAtual.compareTo(saldoOriginalRegistrado) != 0;
    }

    @Override
    @Transactional(readOnly = true)
    public List<LoteMateriaPrima> listarCadeiaAteRaiz(LoteMateriaPrima lote) {
        LinkedList<LoteMateriaPrima> cadeia = new LinkedList<>();
        LoteMateriaPrima atual = lote;

        while (atual != null) {
            cadeia.addFirst(atual);
            atual = atual.getLoteDeOrigem();
        }

        return cadeia;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> listarOrdensRelacionadasIds(LoteMateriaPrima lote) {
        Set<Long> ordensRelacionadas = new LinkedHashSet<>();

        for (LoteMateriaPrima loteDaCadeia : listarCadeiaAteRaiz(lote)) {
            if (loteDaCadeia.getOrdemDeProducaoOrigem() != null) {
                ordensRelacionadas.add(loteDaCadeia.getOrdemDeProducaoOrigem().getId());
            }
        }

        lote.getMovimentacoes().stream()
                .filter(movimentacao -> movimentacao.getOrdemDeProducao() != null)
                .map(movimentacao -> movimentacao.getOrdemDeProducao().getId())
                .forEach(ordensRelacionadas::add);

        return new ArrayList<>(ordensRelacionadas);
    }

    @Override
    @Transactional(readOnly = true)
    public TipoMovimentacao obterTipoAlteracaoAtiva(LoteMateriaPrima lote) {
        return lote.getMovimentacoes().stream()
                .filter(movimentacao -> switch (movimentacao.getTipo()) {
                    case SAIDA_PRODUCAO, PERDA_DESCARTE, AJUSTE_INVENTARIO -> true;
                    default -> false;
                })
                .max(Comparator.comparing(MovimentacaoEstoqueLote::getData))
                .map(MovimentacaoEstoqueLote::getTipo)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public Long obterOrdemConsumidoraAtivaId(LoteMateriaPrima lote) {
        return lote.getMovimentacoes().stream()
                .filter(movimentacao -> movimentacao.getTipo() == TipoMovimentacao.SAIDA_PRODUCAO)
                .max(Comparator.comparing(MovimentacaoEstoqueLote::getData))
                .map(MovimentacaoEstoqueLote::getOrdemDeProducao)
                .filter(Objects::nonNull)
                .map(OrdemDeProducao::getId)
                .orElse(null);
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
                possuiAlteracaoAtivaNoEstadoAtual(lote)
        );
    }

    private BigDecimal obterSaldoOriginalRegistrado(LoteMateriaPrima lote) {
        TipoMovimentacao tipoEntradaOriginal = lote.getLoteDeOrigem() != null
                ? TipoMovimentacao.ENTRADA_SOBRA
                : TipoMovimentacao.ENTRADA_COMPRA;

        return lote.getMovimentacoes().stream()
                .filter(movimentacao -> movimentacao.getTipo() == tipoEntradaOriginal)
                .map(MovimentacaoEstoqueLote::getQuantidade)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
