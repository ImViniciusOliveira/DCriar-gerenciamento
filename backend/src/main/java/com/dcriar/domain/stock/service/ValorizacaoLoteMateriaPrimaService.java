package com.dcriar.domain.stock.service;

import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.model.ValorizacaoAtualLoteMateriaPrima;

public interface ValorizacaoLoteMateriaPrimaService {

    ValorizacaoAtualLoteMateriaPrima calcularValorizacaoAtual(LoteMateriaPrima lote);
}
