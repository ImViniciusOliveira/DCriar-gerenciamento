package com.dcriar.domain.stock.service.impl;

import com.dcriar.api.dto.request.stock.CalcularAjusteLoteRequestDTO;
import com.dcriar.api.dto.response.stock.CalcularAjusteLoteResponseDTO;
import com.dcriar.api.dto.response.stock.ItemImpactadoAjusteLoteDTO;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.entity.MovimentacaoEstoqueLote;
import com.dcriar.domain.stock.entity.enums.DirecaoAjusteLote;
import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import com.dcriar.domain.stock.entity.enums.TipoOperacaoAjusteLote;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.dcriar.domain.stock.repository.LoteMateriaPrimaRepository;
import com.dcriar.domain.stock.repository.MovimentacaoEstoqueLoteRepository;
import com.dcriar.domain.stock.service.AjusteLoteService;
import com.dcriar.exception.custom.AjusteLoteInvalidoException;
import com.dcriar.exception.custom.EstoqueInsuficienteParaMovimentacaoException;
import com.dcriar.exception.custom.LoteMateriaPrimaNaoEncontradoException;
import com.dcriar.exception.custom.QuantidadeUnidadesInvalidaException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Implementa o fluxo operacional de preview de ajuste de lote sem interferir no livro-razão técnico atual.
 */
@Service
@RequiredArgsConstructor
public class AjusteLoteServiceImpl implements AjusteLoteService {

    private static final int SCALE_MONEY = 8;

    private final LoteMateriaPrimaRepository loteMateriaPrimaRepository;
    private final MovimentacaoEstoqueLoteRepository movimentacaoEstoqueLoteRepository;

    @Override
    @Transactional(readOnly = true)
    public CalcularAjusteLoteResponseDTO calcular(Long loteId, CalcularAjusteLoteRequestDTO requestDTO) {
        LoteMateriaPrima lote = loteMateriaPrimaRepository.findByIdWithTipoMateriaPrima(loteId)
                .orElseThrow(() -> new LoteMateriaPrimaNaoEncontradoException(loteId));

        validarRequest(requestDTO);

        BigDecimal saldoAtualInterno = calcularSaldo(lote);
        BigDecimal quantidadeAjusteInterna = converterQuantidadeParaUnidadeInterna(lote, requestDTO.getQuantidade());
        BigDecimal quantidadeMovimentacao = resolverQuantidadeMovimentacao(requestDTO, quantidadeAjusteInterna);

        if (saldoAtualInterno.add(quantidadeMovimentacao).compareTo(BigDecimal.ZERO) < 0) {
            throw new EstoqueInsuficienteParaMovimentacaoException(
                    loteId,
                    quantidadeMovimentacao.abs().doubleValue(),
                    saldoAtualInterno.doubleValue()
            );
        }

        BigDecimal custoUnitarioAtual = calcularCustoUnitarioAtual(lote);
        BigDecimal valorAtualLote = custoUnitarioAtual.multiply(saldoAtualInterno).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
        BigDecimal saldoProjetadoInterno = saldoAtualInterno.add(quantidadeMovimentacao);

        BigDecimal valorProjetadoLote = switch (requestDTO.getTipoOperacao()) {
            case PERDA_DESCARTE -> custoUnitarioAtual.multiply(saldoProjetadoInterno).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
            case AJUSTE -> valorAtualLote;
        };

        BigDecimal custoUnitarioProjetado = saldoProjetadoInterno.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(SCALE_MONEY, RoundingMode.HALF_UP)
                : valorProjetadoLote.divide(saldoProjetadoInterno, SCALE_MONEY, RoundingMode.HALF_UP);

        return CalcularAjusteLoteResponseDTO.builder()
                .loteId(lote.getId())
                .tipoOperacao(requestDTO.getTipoOperacao())
                .tipoOperacaoDescricao(requestDTO.getTipoOperacao().getDescricao())
                .direcao(requestDTO.getDirecao())
                .direcaoDescricao(requestDTO.getDirecao() != null ? requestDTO.getDirecao().getDescricao() : null)
                .unidadeApresentacao(lote.getUnidadeCadastroEstoque())
                .unidadeSimbolo(lote.getUnidadeCadastroEstoque().getSimbolo())
                .saldoAtual(converterQuantidadeParaApresentacao(lote, saldoAtualInterno))
                .saldoProjetado(converterQuantidadeParaApresentacao(lote, saldoProjetadoInterno))
                .valorAtualLote(valorAtualLote)
                .valorProjetadoLote(valorProjetadoLote)
                .custoUnitarioAtual(custoUnitarioAtual)
                .custoUnitarioProjetado(custoUnitarioProjetado)
                .tipoMovimentacaoGerada(resolverTipoMovimentacao(requestDTO))
                .quantidadeMovimentacaoGerada(quantidadeMovimentacao)
                .itensImpactados(montarItensImpactados(lote, custoUnitarioAtual, custoUnitarioProjetado, requestDTO.getTipoOperacao()))
                .build();
    }

    private void validarRequest(CalcularAjusteLoteRequestDTO requestDTO) {
        if (requestDTO.getQuantidade() == null || requestDTO.getQuantidade().compareTo(BigDecimal.ZERO) <= 0) {
            throw new QuantidadeUnidadesInvalidaException(requestDTO.getQuantidade());
        }
        if (requestDTO.getTipoOperacao() == TipoOperacaoAjusteLote.AJUSTE && requestDTO.getDirecao() == null) {
            throw AjusteLoteInvalidoException.direcaoObrigatoriaParaAjuste();
        }
        if (requestDTO.getTipoOperacao() == TipoOperacaoAjusteLote.PERDA_DESCARTE && requestDTO.getDirecao() != null) {
            throw AjusteLoteInvalidoException.perdaNaoAceitaDirecao();
        }
    }

    private BigDecimal resolverQuantidadeMovimentacao(CalcularAjusteLoteRequestDTO requestDTO, BigDecimal quantidadeInterna) {
        if (requestDTO.getTipoOperacao() == TipoOperacaoAjusteLote.PERDA_DESCARTE) {
            return quantidadeInterna.negate();
        }
        return requestDTO.getDirecao() == DirecaoAjusteLote.ADICIONAR
                ? quantidadeInterna
                : quantidadeInterna.negate();
    }

    private TipoMovimentacao resolverTipoMovimentacao(CalcularAjusteLoteRequestDTO requestDTO) {
        return requestDTO.getTipoOperacao() == TipoOperacaoAjusteLote.PERDA_DESCARTE
                ? TipoMovimentacao.PERDA_DESCARTE
                : TipoMovimentacao.AJUSTE_INVENTARIO;
    }

    private BigDecimal calcularSaldo(LoteMateriaPrima lote) {
        return lote.getSaldoAtual() != null
                ? lote.getSaldoAtual()
                : movimentacaoEstoqueLoteRepository.findSaldoByLote(lote);
    }

    private BigDecimal calcularCustoUnitarioAtual(LoteMateriaPrima lote) {
        BigDecimal quantidadeBaseComCusto = calcularQuantidadeBaseComCusto(lote);
        if (quantidadeBaseComCusto.compareTo(BigDecimal.ZERO) <= 0 || lote.getCustoTotalLote() == null) {
            return BigDecimal.ZERO.setScale(SCALE_MONEY, RoundingMode.HALF_UP);
        }
        return lote.getCustoTotalLote().divide(quantidadeBaseComCusto, SCALE_MONEY, RoundingMode.HALF_UP);
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

    private BigDecimal converterQuantidadeParaUnidadeInterna(LoteMateriaPrima lote, BigDecimal quantidadeInformada) {
        UnidadeDeMedida unidadePrincipal = lote.getTipoMateriaPrima().getUnidadeDeConsumo();
        if (unidadePrincipal.isConsumo() && !unidadePrincipal.isPermiteCorte()) {
            return unidadePrincipal.converterQuantidadeParaUnidadeInterna(quantidadeInformada, lote.getUnidadeCadastroEstoque());
        }
        return quantidadeInformada;
    }

    private BigDecimal converterQuantidadeParaApresentacao(LoteMateriaPrima lote, BigDecimal quantidadeInterna) {
        UnidadeDeMedida unidadePrincipal = lote.getTipoMateriaPrima().getUnidadeDeConsumo();
        if (unidadePrincipal.isConsumo() && !unidadePrincipal.isPermiteCorte()) {
            return unidadePrincipal.converterQuantidadeDaUnidadeInternaParaInformada(quantidadeInterna, lote.getUnidadeCadastroEstoque());
        }
        return quantidadeInterna;
    }

    private List<ItemImpactadoAjusteLoteDTO> montarItensImpactados(
            LoteMateriaPrima lote,
            BigDecimal custoUnitarioAtual,
            BigDecimal custoUnitarioProjetado,
            TipoOperacaoAjusteLote tipoOperacao
    ) {
        if (tipoOperacao != TipoOperacaoAjusteLote.AJUSTE) {
            return List.of();
        }

        return loteMateriaPrimaRepository.findByLoteDeOrigem(lote).stream()
                .map(loteFilho -> {
                    BigDecimal saldoAtualFilho = calcularSaldo(loteFilho);
                    BigDecimal valorAtual = custoUnitarioAtual.multiply(saldoAtualFilho).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
                    BigDecimal valorProjetado = custoUnitarioProjetado.multiply(saldoAtualFilho).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
                    return ItemImpactadoAjusteLoteDTO.builder()
                            .id(loteFilho.getId())
                            .tipoItem("RETALHO")
                            .descricao("Retalho originado do lote #" + lote.getId())
                            .saldoAtual(converterQuantidadeParaApresentacao(loteFilho, saldoAtualFilho))
                            .valorAtual(valorAtual)
                            .valorProjetado(valorProjetado)
                            .selecionadoPorPadrao(true)
                            .build();
                })
                .toList();
    }
}
