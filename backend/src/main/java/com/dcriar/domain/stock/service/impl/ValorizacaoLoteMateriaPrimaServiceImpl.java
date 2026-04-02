package com.dcriar.domain.stock.service.impl;

import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.entity.MovimentacaoEstoqueLote;
import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.dcriar.domain.stock.model.ValorizacaoAtualLoteMateriaPrima;
import com.dcriar.domain.stock.repository.MovimentacaoEstoqueLoteRepository;
import com.dcriar.domain.stock.service.ValorizacaoLoteMateriaPrimaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ValorizacaoLoteMateriaPrimaServiceImpl implements ValorizacaoLoteMateriaPrimaService {

    private static final int SCALE_MONEY = 8;

    private final MovimentacaoEstoqueLoteRepository movimentacaoEstoqueLoteRepository;

    @Override
    public ValorizacaoAtualLoteMateriaPrima calcularValorizacaoAtual(LoteMateriaPrima lote) {
        BigDecimal saldoInterno = calcularSaldo(lote);
        BigDecimal saldoApresentacao = converterQuantidadeParaApresentacao(lote, saldoInterno);
        BigDecimal custoUnitarioAtualInterno = calcularCustoUnitarioAtualInterno(lote, saldoInterno);
        BigDecimal valorAtualLote = custoUnitarioAtualInterno.multiply(saldoInterno).setScale(SCALE_MONEY, RoundingMode.HALF_UP);

        BigDecimal custoUnitarioAtualApresentacao = saldoApresentacao.compareTo(BigDecimal.ZERO) <= 0
                ? BigDecimal.ZERO.setScale(SCALE_MONEY, RoundingMode.HALF_UP)
                : valorAtualLote.divide(saldoApresentacao, SCALE_MONEY, RoundingMode.HALF_UP);

        return new ValorizacaoAtualLoteMateriaPrima(
                saldoInterno,
                saldoApresentacao,
                valorAtualLote,
                custoUnitarioAtualInterno,
                custoUnitarioAtualApresentacao
        );
    }

    private BigDecimal calcularSaldo(LoteMateriaPrima lote) {
        return lote.getSaldoAtual() != null
                ? lote.getSaldoAtual()
                : movimentacaoEstoqueLoteRepository.findSaldoByLote(lote);
    }

    private BigDecimal calcularCustoUnitarioAtualInterno(LoteMateriaPrima lote, BigDecimal saldoAtual) {
        if (possuiAjusteOuPerdaManual(lote)) {
            if (saldoAtual.compareTo(BigDecimal.ZERO) <= 0 || lote.getCustoTotalLote() == null) {
                return BigDecimal.ZERO.setScale(SCALE_MONEY, RoundingMode.HALF_UP);
            }
            return lote.getCustoTotalLote().divide(saldoAtual, SCALE_MONEY, RoundingMode.HALF_UP);
        }

        BigDecimal quantidadeBaseComCusto = calcularQuantidadeBaseComCusto(lote);
        if (quantidadeBaseComCusto.compareTo(BigDecimal.ZERO) <= 0 || lote.getCustoTotalLote() == null) {
            return BigDecimal.ZERO.setScale(SCALE_MONEY, RoundingMode.HALF_UP);
        }
        return lote.getCustoTotalLote().divide(quantidadeBaseComCusto, SCALE_MONEY, RoundingMode.HALF_UP);
    }

    private boolean possuiAjusteOuPerdaManual(LoteMateriaPrima lote) {
        return movimentacaoEstoqueLoteRepository.findAllByLote(lote).stream()
                .map(MovimentacaoEstoqueLote::getTipo)
                .anyMatch(tipo -> tipo == TipoMovimentacao.AJUSTE_INVENTARIO || tipo == TipoMovimentacao.PERDA_DESCARTE);
    }

    private BigDecimal calcularQuantidadeBaseComCusto(LoteMateriaPrima lote) {
        List<MovimentacaoEstoqueLote> movimentacoes = movimentacaoEstoqueLoteRepository.findAllByLote(lote);

        BigDecimal quantidadeEntradaCompra = somarQuantidadePorTipo(movimentacoes, TipoMovimentacao.ENTRADA_COMPRA);
        if (quantidadeEntradaCompra.compareTo(BigDecimal.ZERO) > 0) {
            return quantidadeEntradaCompra;
        }

        BigDecimal quantidadeEntradaSobra = somarQuantidadePorTipo(movimentacoes, TipoMovimentacao.ENTRADA_SOBRA);
        if (quantidadeEntradaSobra.compareTo(BigDecimal.ZERO) > 0) {
            return quantidadeEntradaSobra;
        }

        BigDecimal quantidadeAjustePositiva = movimentacoes.stream()
                .filter(mov -> mov.getTipo() == TipoMovimentacao.AJUSTE_INVENTARIO)
                .map(MovimentacaoEstoqueLote::getQuantidade)
                .filter(qtd -> qtd.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return quantidadeAjustePositiva.compareTo(BigDecimal.ZERO) > 0
                ? quantidadeAjustePositiva
                : BigDecimal.ZERO;
    }

    private BigDecimal somarQuantidadePorTipo(List<MovimentacaoEstoqueLote> movimentacoes, TipoMovimentacao tipoMovimentacao) {
        return movimentacoes.stream()
                .filter(mov -> mov.getTipo() == tipoMovimentacao)
                .map(MovimentacaoEstoqueLote::getQuantidade)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal converterQuantidadeParaApresentacao(LoteMateriaPrima lote, BigDecimal quantidadeInterna) {
        UnidadeDeMedida unidadePrincipal = lote.getTipoMateriaPrima().getUnidadeDeConsumo();
        if (unidadePrincipal.isConsumo() && !unidadePrincipal.isPermiteCorte()) {
            return unidadePrincipal.converterQuantidadeDaUnidadeInternaParaInformada(quantidadeInterna, lote.getUnidadeCadastroEstoque());
        }
        return quantidadeInterna;
    }
}
