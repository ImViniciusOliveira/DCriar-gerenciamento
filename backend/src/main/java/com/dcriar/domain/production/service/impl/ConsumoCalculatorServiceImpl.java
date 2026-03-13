package com.dcriar.domain.production.service.impl;

import com.dcriar.domain.production.model.PlanoDeConsumo;
import com.dcriar.domain.production.model.PlanoDeConsumoItem;
import com.dcriar.domain.production.service.ConsumoCalculatorService;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.repository.MovimentacaoEstoqueLoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ConsumoCalculatorServiceImpl implements ConsumoCalculatorService {

    private final MovimentacaoEstoqueLoteRepository movimentacaoEstoqueLoteRepository;

    @Override
    public PlanoDeConsumo calcularPlanoDeConsumo(LoteMateriaPrima lote, BigDecimal consumoNecessario) {
        BigDecimal saldoDoLote = movimentacaoEstoqueLoteRepository.findSaldoByLote(lote);
        
        // A validação de saldo suficiente já foi feita no service principal,
        // mas garantimos que não consumimos mais do que o disponível.
        BigDecimal consumoNesteLote = saldoDoLote.min(consumoNecessario);

        PlanoDeConsumoItem item = new PlanoDeConsumoItem(lote, consumoNesteLote);
        
        Map<Long, BigDecimal> saldosRestantes = new HashMap<>();
        saldosRestantes.put(lote.getId(), saldoDoLote.subtract(consumoNesteLote));

        return new PlanoDeConsumo(Collections.singletonList(item), saldosRestantes);
    }
}
