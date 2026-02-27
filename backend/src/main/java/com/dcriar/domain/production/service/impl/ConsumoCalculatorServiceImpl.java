package com.dcriar.domain.production.service.impl;

import com.dcriar.domain.production.model.PlanoDeConsumo;
import com.dcriar.domain.production.model.PlanoDeConsumoItem;
import com.dcriar.domain.production.service.ConsumoCalculatorService;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.repository.MovimentacaoEstoqueLoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConsumoCalculatorServiceImpl implements ConsumoCalculatorService {

    private final MovimentacaoEstoqueLoteRepository movimentacaoEstoqueLoteRepository;

    @Override
    public PlanoDeConsumo calcularPlanoDeConsumo(List<LoteMateriaPrima> lotes, BigDecimal consumoNecessario) {
        List<PlanoDeConsumoItem> itens = new ArrayList<>();
        Map<Long, BigDecimal> saldosRestantes = new HashMap<>();
        BigDecimal consumoRestante = consumoNecessario;

        // Inicializa o mapa de saldos restantes com o saldo atual de todos os lotes envolvidos.
        for (LoteMateriaPrima lote : lotes) {
            saldosRestantes.put(lote.getId(), movimentacaoEstoqueLoteRepository.findSaldoByLote(lote));
        }

        for (LoteMateriaPrima lote : lotes) {
            if (consumoRestante.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }

            BigDecimal saldoDoLote = saldosRestantes.get(lote.getId());
            if (saldoDoLote == null || saldoDoLote.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal consumoNesteLote = saldoDoLote.min(consumoRestante);

            if (consumoNesteLote.compareTo(BigDecimal.ZERO) > 0) {
                itens.add(new PlanoDeConsumoItem(lote, consumoNesteLote));
                consumoRestante = consumoRestante.subtract(consumoNesteLote);
                saldosRestantes.put(lote.getId(), saldoDoLote.subtract(consumoNesteLote));
            }
        }

        return new PlanoDeConsumo(itens, saldosRestantes);
    }
}
