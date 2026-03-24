package com.dcriar.domain.stock.service.impl;

import com.dcriar.api.dto.request.stock.AplicarAjusteLoteRequestDTO;
import com.dcriar.api.dto.request.stock.CalcularAjusteLoteRequestDTO;
import com.dcriar.api.dto.response.stock.CalcularAjusteLoteResponseDTO;
import com.dcriar.api.dto.response.stock.ItemImpactadoAjusteLoteDTO;
import com.dcriar.api.dto.response.stock.LoteMateriaPrimaResponseDTO;
import com.dcriar.api.mapper.stock.LoteMateriaPrimaMapper;
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
import jakarta.persistence.EntityManager;
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
    private final LoteMateriaPrimaMapper loteMateriaPrimaMapper;
    private final EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public CalcularAjusteLoteResponseDTO calcular(Long loteId, CalcularAjusteLoteRequestDTO requestDTO) {
        LoteMateriaPrima lote = loteMateriaPrimaRepository.findByIdWithTipoMateriaPrima(loteId)
                .orElseThrow(() -> new LoteMateriaPrimaNaoEncontradoException(loteId));

        ResultadoCalculoAjuste resultado = calcularResultado(lote, requestDTO.getTipoOperacao(), requestDTO.getDirecao(), requestDTO.getQuantidade(), requestDTO.getMotivo());
        return construirResponse(resultado);
    }

    @Override
    @Transactional
    public LoteMateriaPrimaResponseDTO aplicar(Long loteId, AplicarAjusteLoteRequestDTO requestDTO) {
        LoteMateriaPrima lote = loteMateriaPrimaRepository.findByIdWithTipoMateriaPrima(loteId)
                .orElseThrow(() -> new LoteMateriaPrimaNaoEncontradoException(loteId));

        ResultadoCalculoAjuste resultado = calcularResultado(lote, requestDTO.getTipoOperacao(), requestDTO.getDirecao(), requestDTO.getQuantidade(), requestDTO.getMotivo());

        validarItensImpactadosSelecionados(lote.getId(), requestDTO.getIdsItensImpactadosAtualizados(), resultado.itensImpactados());

        MovimentacaoEstoqueLote movimentacao = MovimentacaoEstoqueLote.builder()
                .lote(lote)
                .tipo(resultado.tipoMovimentacaoGerada())
                .quantidade(resultado.quantidadeMovimentacao())
                .motivo(requestDTO.getMotivo())
                .build();
        movimentacaoEstoqueLoteRepository.save(movimentacao);

        lote.setCustoTotalLote(resultado.valorProjetadoLote());
        loteMateriaPrimaRepository.save(lote);

        if (requestDTO.getIdsItensImpactadosAtualizados() != null && !requestDTO.getIdsItensImpactadosAtualizados().isEmpty()) {
            List<LoteMateriaPrima> lotesImpactadosParaAtualizar = resultado.itensImpactados().stream()
                    .filter(item -> requestDTO.getIdsItensImpactadosAtualizados().contains(item.lote().getId()))
                    .map(ItemImpactadoCalculado::lote)
                    .toList();

            lotesImpactadosParaAtualizar.forEach(loteImpactado -> {
                ItemImpactadoCalculado itemCalculado = resultado.itensImpactados().stream()
                        .filter(item -> item.lote().getId().equals(loteImpactado.getId()))
                        .findFirst()
                        .orElseThrow();
                loteImpactado.setCustoTotalLote(itemCalculado.valorProjetado());
            });
            loteMateriaPrimaRepository.saveAll(lotesImpactadosParaAtualizar);
        }

        loteMateriaPrimaRepository.flush();
        entityManager.clear();

        LoteMateriaPrima loteAtualizado = loteMateriaPrimaRepository.findByIdWithTipoMateriaPrima(loteId)
                .orElseThrow(() -> new LoteMateriaPrimaNaoEncontradoException(loteId));
        BigDecimal saldoAtualizado = calcularSaldo(loteAtualizado);
        LoteMateriaPrimaResponseDTO responseDTO = loteMateriaPrimaMapper.toResponseDTO(loteAtualizado);
        popularDadosDeApresentacao(responseDTO, loteAtualizado, saldoAtualizado);
        return responseDTO;
    }

    private ResultadoCalculoAjuste calcularResultado(
            LoteMateriaPrima lote,
            TipoOperacaoAjusteLote tipoOperacao,
            DirecaoAjusteLote direcao,
            BigDecimal quantidadeInformada,
            String motivo
    ) {
        validarRequest(tipoOperacao, direcao, quantidadeInformada);

        BigDecimal saldoAtualInterno = calcularSaldo(lote);
        BigDecimal quantidadeAjusteInterna = converterQuantidadeParaUnidadeInterna(lote, quantidadeInformada);
        BigDecimal quantidadeMovimentacao = resolverQuantidadeMovimentacao(tipoOperacao, direcao, quantidadeAjusteInterna);

        if (saldoAtualInterno.add(quantidadeMovimentacao).compareTo(BigDecimal.ZERO) < 0) {
            throw new EstoqueInsuficienteParaMovimentacaoException(
                    lote.getId(),
                    quantidadeMovimentacao.abs().doubleValue(),
                    saldoAtualInterno.doubleValue()
            );
        }

        BigDecimal custoUnitarioAtual = calcularCustoUnitarioAtual(lote);
        BigDecimal valorAtualLote = custoUnitarioAtual.multiply(saldoAtualInterno).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
        BigDecimal saldoProjetadoInterno = saldoAtualInterno.add(quantidadeMovimentacao);

        BigDecimal valorProjetadoLote = switch (tipoOperacao) {
            case PERDA_DESCARTE -> custoUnitarioAtual.multiply(saldoProjetadoInterno).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
            case AJUSTE -> valorAtualLote;
        };

        BigDecimal custoUnitarioProjetado = saldoProjetadoInterno.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(SCALE_MONEY, RoundingMode.HALF_UP)
                : valorProjetadoLote.divide(saldoProjetadoInterno, SCALE_MONEY, RoundingMode.HALF_UP);

        List<ItemImpactadoCalculado> itensImpactados = montarItensImpactados(lote, custoUnitarioAtual, custoUnitarioProjetado, tipoOperacao);

        return new ResultadoCalculoAjuste(
                lote,
                tipoOperacao,
                direcao,
                motivo,
                saldoAtualInterno,
                saldoProjetadoInterno,
                valorAtualLote,
                valorProjetadoLote,
                custoUnitarioAtual,
                custoUnitarioProjetado,
                resolverTipoMovimentacao(tipoOperacao),
                quantidadeMovimentacao,
                itensImpactados
        );
    }

    private CalcularAjusteLoteResponseDTO construirResponse(ResultadoCalculoAjuste resultado) {
        return CalcularAjusteLoteResponseDTO.builder()
                .loteId(resultado.lote().getId())
                .tipoOperacao(resultado.tipoOperacao())
                .tipoOperacaoDescricao(resultado.tipoOperacao().getDescricao())
                .direcao(resultado.direcao())
                .direcaoDescricao(resultado.direcao() != null ? resultado.direcao().getDescricao() : null)
                .unidadeApresentacao(resultado.lote().getUnidadeCadastroEstoque())
                .unidadeSimbolo(resultado.lote().getUnidadeCadastroEstoque().getSimbolo())
                .saldoAtual(converterQuantidadeParaApresentacao(resultado.lote(), resultado.saldoAtualInterno()))
                .saldoProjetado(converterQuantidadeParaApresentacao(resultado.lote(), resultado.saldoProjetadoInterno()))
                .valorAtualLote(resultado.valorAtualLote())
                .valorProjetadoLote(resultado.valorProjetadoLote())
                .custoUnitarioAtual(resultado.custoUnitarioAtual())
                .custoUnitarioProjetado(resultado.custoUnitarioProjetado())
                .tipoMovimentacaoGerada(resultado.tipoMovimentacaoGerada())
                .quantidadeMovimentacaoGerada(resultado.quantidadeMovimentacao())
                .itensImpactados(resultado.itensImpactados().stream().map(this::toItemImpactadoDTO).toList())
                .build();
    }

    private void validarRequest(TipoOperacaoAjusteLote tipoOperacao, DirecaoAjusteLote direcao, BigDecimal quantidade) {
        if (quantidade == null || quantidade.compareTo(BigDecimal.ZERO) <= 0) {
            throw new QuantidadeUnidadesInvalidaException(quantidade);
        }
        if (tipoOperacao == TipoOperacaoAjusteLote.AJUSTE && direcao == null) {
            throw AjusteLoteInvalidoException.direcaoObrigatoriaParaAjuste();
        }
        if (tipoOperacao == TipoOperacaoAjusteLote.PERDA_DESCARTE && direcao != null) {
            throw AjusteLoteInvalidoException.perdaNaoAceitaDirecao();
        }
    }

    private BigDecimal resolverQuantidadeMovimentacao(TipoOperacaoAjusteLote tipoOperacao, DirecaoAjusteLote direcao, BigDecimal quantidadeInterna) {
        if (tipoOperacao == TipoOperacaoAjusteLote.PERDA_DESCARTE) {
            return quantidadeInterna.negate();
        }
        return direcao == DirecaoAjusteLote.ADICIONAR
                ? quantidadeInterna
                : quantidadeInterna.negate();
    }

    private TipoMovimentacao resolverTipoMovimentacao(TipoOperacaoAjusteLote tipoOperacao) {
        return tipoOperacao == TipoOperacaoAjusteLote.PERDA_DESCARTE
                ? TipoMovimentacao.PERDA_DESCARTE
                : TipoMovimentacao.AJUSTE_INVENTARIO;
    }

    private BigDecimal calcularSaldo(LoteMateriaPrima lote) {
        return lote.getSaldoAtual() != null
                ? lote.getSaldoAtual()
                : movimentacaoEstoqueLoteRepository.findSaldoByLote(lote);
    }

    private BigDecimal calcularCustoUnitarioAtual(LoteMateriaPrima lote) {
        BigDecimal saldoAtual = calcularSaldo(lote);
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

    private List<ItemImpactadoCalculado> montarItensImpactados(
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
                    return new ItemImpactadoCalculado(
                            loteFilho,
                            "RETALHO",
                            "Retalho originado do lote #" + lote.getId(),
                            converterQuantidadeParaApresentacao(loteFilho, saldoAtualFilho),
                            valorAtual,
                            valorProjetado,
                            true
                    );
                })
                .toList();
    }

    private ItemImpactadoAjusteLoteDTO toItemImpactadoDTO(ItemImpactadoCalculado item) {
        return ItemImpactadoAjusteLoteDTO.builder()
                .id(item.lote().getId())
                .tipoItem(item.tipoItem())
                .descricao(item.descricao())
                .saldoAtual(item.saldoAtual())
                .valorAtual(item.valorAtual())
                .valorProjetado(item.valorProjetado())
                .selecionadoPorPadrao(item.selecionadoPorPadrao())
                .build();
    }

    private void validarItensImpactadosSelecionados(Long loteId, List<Long> idsSelecionados, List<ItemImpactadoCalculado> itensImpactados) {
        if (idsSelecionados == null || idsSelecionados.isEmpty()) {
            return;
        }

        List<Long> idsValidos = itensImpactados.stream()
                .map(item -> item.lote().getId())
                .toList();

        idsSelecionados.stream()
                .filter(id -> !idsValidos.contains(id))
                .findFirst()
                .ifPresent(id -> {
                    throw AjusteLoteInvalidoException.itemImpactadoNaoPertenceAoLote(id, loteId);
                });
    }

    private void popularDadosDeApresentacao(LoteMateriaPrimaResponseDTO responseDTO, LoteMateriaPrima lote, BigDecimal saldoInterno) {
        UnidadeDeMedida unidadeCadastro = lote.getUnidadeCadastroEstoque();
        UnidadeDeMedida unidadePrincipal = lote.getTipoMateriaPrima().getUnidadeDeConsumo();
        BigDecimal saldoApresentacao = saldoInterno;

        if (unidadePrincipal.isConsumo() && !unidadePrincipal.isPermiteCorte()) {
            saldoApresentacao = unidadePrincipal.converterQuantidadeDaUnidadeInternaParaInformada(
                    saldoInterno,
                    unidadeCadastro
            );
        }

        responseDTO.setUnidadeDeEstoque(unidadeCadastro);
        responseDTO.setUnidadeCadastroEstoque(unidadeCadastro);
        responseDTO.setUnidadeSimbolo(unidadeCadastro.getSimbolo());
        responseDTO.setSaldoEstoque(saldoApresentacao);
    }

    private record ResultadoCalculoAjuste(
            LoteMateriaPrima lote,
            TipoOperacaoAjusteLote tipoOperacao,
            DirecaoAjusteLote direcao,
            String motivo,
            BigDecimal saldoAtualInterno,
            BigDecimal saldoProjetadoInterno,
            BigDecimal valorAtualLote,
            BigDecimal valorProjetadoLote,
            BigDecimal custoUnitarioAtual,
            BigDecimal custoUnitarioProjetado,
            TipoMovimentacao tipoMovimentacaoGerada,
            BigDecimal quantidadeMovimentacao,
            List<ItemImpactadoCalculado> itensImpactados
    ) {}

    private record ItemImpactadoCalculado(
            LoteMateriaPrima lote,
            String tipoItem,
            String descricao,
            BigDecimal saldoAtual,
            BigDecimal valorAtual,
            BigDecimal valorProjetado,
            boolean selecionadoPorPadrao
    ) {}
}
