package com.dcriar.domain.stock.service.impl;

import com.dcriar.api.dto.request.stock.AplicarAjusteLoteRequestDTO;
import com.dcriar.api.dto.request.stock.CalcularAjusteLoteRequestDTO;
import com.dcriar.api.dto.response.stock.CalcularAjusteLoteResponseDTO;
import com.dcriar.api.dto.response.stock.ItemImpactadoAjusteLoteDTO;
import com.dcriar.api.dto.response.stock.LoteMateriaPrimaResponseDTO;
import com.dcriar.api.mapper.stock.LoteMateriaPrimaMapper;
import com.dcriar.domain.common.util.HumanNumberDisplayFormatter;
import com.dcriar.domain.common.util.LogicalMapKeySupport;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.entity.MovimentacaoEstoqueLote;
import com.dcriar.domain.stock.entity.enums.DirecaoAjusteLote;
import com.dcriar.domain.stock.entity.enums.ContextoItensImpactadosAjusteLote;
import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import com.dcriar.domain.stock.entity.enums.TipoOperacaoAjusteLote;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.dcriar.domain.stock.model.LoteRetalhoHierarchyItem;
import com.dcriar.domain.stock.repository.LoteMateriaPrimaRepository;
import com.dcriar.domain.stock.repository.MovimentacaoEstoqueLoteRepository;
import com.dcriar.domain.stock.service.AjusteLoteService;
import com.dcriar.domain.stock.service.LoteRetalhoHierarchyService;
import com.dcriar.domain.stock.util.LotePublicIdentifierFormatter;
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
    private final LoteRetalhoHierarchyService loteRetalhoHierarchyService;
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
                    .map(ItemImpactadoCalculado::lote)
                    .filter(loteImpactado -> requestDTO.getIdsItensImpactadosAtualizados().contains(loteImpactado.getId()))
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
        BigDecimal saldoAtualApresentacao = calcularSaldoApresentacao(lote, saldoAtualInterno);
        BigDecimal valorAtualLote = calcularValorAtualLote(lote);
        BigDecimal custoUnitarioAtualInterno = calcularCustoUnitarioAtualInterno(lote, saldoAtualInterno, valorAtualLote);
        BigDecimal custoUnitarioAtualApresentacao = calcularCustoUnitarioAtualApresentacao(lote);

        if (saldoAtualInterno.add(quantidadeMovimentacao).compareTo(BigDecimal.ZERO) < 0) {
            throw new EstoqueInsuficienteParaMovimentacaoException(
                    lote.getId(),
                    LotePublicIdentifierFormatter.format(lote),
                    lote.getTipoMateriaPrima().getNome(),
                    lote.getUnidadeCadastroEstoque().getSimbolo(),
                    quantidadeInformada.abs().doubleValue(),
                    saldoAtualApresentacao.doubleValue()
            );
        }

        BigDecimal saldoProjetadoInterno = saldoAtualInterno.add(quantidadeMovimentacao);

        BigDecimal valorProjetadoLote = switch (tipoOperacao) {
            case PERDA_DESCARTE -> custoUnitarioAtualInterno.multiply(saldoProjetadoInterno).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
            case AJUSTE -> valorAtualLote;
        };

        BigDecimal saldoProjetadoApresentacao = converterQuantidadeParaApresentacao(lote, saldoProjetadoInterno);
        BigDecimal custoUnitarioProjetadoInterno = saldoProjetadoInterno.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(SCALE_MONEY, RoundingMode.HALF_UP)
                : valorProjetadoLote.divide(saldoProjetadoInterno, SCALE_MONEY, RoundingMode.HALF_UP);
        BigDecimal custoUnitarioProjetadoApresentacao = saldoProjetadoApresentacao.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO.setScale(SCALE_MONEY, RoundingMode.HALF_UP)
                : valorProjetadoLote.divide(saldoProjetadoApresentacao, SCALE_MONEY, RoundingMode.HALF_UP);

        List<LoteRetalhoHierarchyItem> descendentes = loteRetalhoHierarchyService.listarDescendentes(lote);
        List<ItemImpactadoCalculado> itensImpactados = montarItensImpactados(custoUnitarioProjetadoInterno, tipoOperacao, descendentes);
        ContextoItensImpactadosAjusteLote contextoItensImpactados = resolverContextoItensImpactados(lote, tipoOperacao, descendentes, itensImpactados);

        return new ResultadoCalculoAjuste(
                lote,
                tipoOperacao,
                direcao,
                motivo,
                saldoAtualInterno,
                saldoProjetadoInterno,
                valorAtualLote,
                valorProjetadoLote,
                custoUnitarioAtualApresentacao,
                custoUnitarioProjetadoApresentacao,
                resolverTipoMovimentacao(tipoOperacao),
                quantidadeMovimentacao,
                contextoItensImpactados,
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
                .contextoItensImpactados(resultado.contextoItensImpactados())
                .itensImpactados(resultado.itensImpactados().stream().map(this::toItemImpactadoDTO).toList())
                .build();
    }

    private void validarRequest(TipoOperacaoAjusteLote tipoOperacao, DirecaoAjusteLote direcao, BigDecimal quantidade) {
        if (quantidade == null) {
            throw new QuantidadeUnidadesInvalidaException();
        }
        if (quantidade.compareTo(BigDecimal.ZERO) <= 0) {
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
        return lote.getSaldoAtual() != null ? lote.getSaldoAtual() : BigDecimal.ZERO;
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
            BigDecimal custoUnitarioProjetado,
            TipoOperacaoAjusteLote tipoOperacao,
            List<LoteRetalhoHierarchyItem> descendentes
    ) {
        if (tipoOperacao != TipoOperacaoAjusteLote.AJUSTE) {
            return List.of();
        }

        return descendentes.stream()
                .map(itemHierarchy -> {
                    LoteMateriaPrima loteImpactado = itemHierarchy.lote();
                    BigDecimal saldoAtualFilho = calcularSaldo(loteImpactado);
                    if (saldoAtualFilho.compareTo(BigDecimal.ZERO) <= 0) {
                        return null;
                    }
                    BigDecimal saldoApresentacaoFilho = converterQuantidadeParaApresentacao(loteImpactado, saldoAtualFilho);
                    BigDecimal valorAtual = calcularValorAtualLote(loteImpactado);
                    BigDecimal valorProjetado = custoUnitarioProjetado.multiply(saldoAtualFilho).setScale(SCALE_MONEY, RoundingMode.HALF_UP);
                    List<LoteMateriaPrima> cadeiaAteRaiz = loteRetalhoHierarchyService.listarCadeiaAteRaiz(loteImpactado);
                    String identificadorPublico = LotePublicIdentifierFormatter.format(loteImpactado);
                    String identificadorOrigemPublico = loteImpactado.getLoteDeOrigem() != null
                            ? LotePublicIdentifierFormatter.format(loteImpactado.getLoteDeOrigem())
                            : null;
                    String cadeiaPublica = LotePublicIdentifierFormatter.formatarCadeia(cadeiaAteRaiz);
                    return new ItemImpactadoCalculado(
                            loteImpactado,
                            "RETALHO",
                            montarDescricaoItemImpactado(itemHierarchy, identificadorPublico, identificadorOrigemPublico),
                            identificadorPublico,
                            identificadorOrigemPublico,
                            itemHierarchy.nivel(),
                            cadeiaPublica,
                            cadeiaAteRaiz.stream().map(LotePublicIdentifierFormatter::format).toList(),
                            saldoApresentacaoFilho,
                            formatarSaldoDescricao(loteImpactado, saldoApresentacaoFilho),
                            formatarDimensaoDescricao(loteImpactado, saldoApresentacaoFilho),
                            valorAtual,
                            valorProjetado,
                            true
                    );
                })
                .filter(java.util.Objects::nonNull)
                .toList();
    }

    private ContextoItensImpactadosAjusteLote resolverContextoItensImpactados(
            LoteMateriaPrima lote,
            TipoOperacaoAjusteLote tipoOperacao,
            List<LoteRetalhoHierarchyItem> descendentes,
            List<ItemImpactadoCalculado> itensImpactados
    ) {
        if (tipoOperacao == TipoOperacaoAjusteLote.PERDA_DESCARTE) {
            return ContextoItensImpactadosAjusteLote.PERDA_NAO_RECALCULA_DERIVADOS;
        }

        if (!lote.getTipoMateriaPrima().getUnidadeDeConsumo().isPermiteCorte()) {
            return ContextoItensImpactadosAjusteLote.MATERIA_PRIMA_NAO_GERA_RETALHO;
        }

        if (descendentes.isEmpty()) {
            return ContextoItensImpactadosAjusteLote.SEM_RETALHOS_VINCULADOS;
        }

        if (itensImpactados.isEmpty()) {
            return ContextoItensImpactadosAjusteLote.SEM_RETALHOS_COM_SALDO;
        }

        return ContextoItensImpactadosAjusteLote.COM_ITENS_IMPACTADOS;
    }

    private String montarDescricaoItemImpactado(
            LoteRetalhoHierarchyItem itemHierarchy,
            String identificadorPublico,
            String identificadorOrigemPublico
    ) {
        if (itemHierarchy.nivel() == 1) {
            return identificadorPublico + " originado de " + identificadorOrigemPublico;
        }
        return identificadorPublico + " derivado de " + identificadorOrigemPublico;
    }

    private ItemImpactadoAjusteLoteDTO toItemImpactadoDTO(ItemImpactadoCalculado item) {
        return ItemImpactadoAjusteLoteDTO.builder()
                .id(item.lote().getId())
                .tipoItem(item.tipoItem())
                .identificadorPublico(item.identificadorPublico())
                .identificadorOrigemPublico(item.identificadorOrigemPublico())
                .nivelArvore(item.nivelArvore())
                .cadeiaPublica(item.cadeiaPublica())
                .cadeiaIdentificadoresPublicos(item.cadeiaIdentificadoresPublicos())
                .descricao(item.descricao())
                .saldoAtual(item.saldoAtual())
                .saldoDescricao(item.saldoDescricao())
                .dimensaoDescricao(item.dimensaoDescricao())
                .valorAtual(item.valorAtual())
                .valorProjetado(item.valorProjetado())
                .selecionadoPorPadrao(item.selecionadoPorPadrao())
                .build();
    }

    private String formatarSaldoDescricao(LoteMateriaPrima lote, BigDecimal saldoApresentacao) {
        return formatarNumero(saldoApresentacao) + lote.getUnidadeCadastroEstoque().getSimbolo();
    }

    private String formatarDimensaoDescricao(LoteMateriaPrima lote, BigDecimal saldoApresentacao) {
        UnidadeDeMedida unidade = lote.getUnidadeCadastroEstoque();
        if (!unidade.isPermiteCorte()) {
            return null;
        }

        BigDecimal larguraMm = extrairLarguraMm(lote);
        if (larguraMm == null || larguraMm.compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }

        BigDecimal larguraCm = larguraMm.divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
        BigDecimal comprimentoCm = switch (unidade) {
            case METRO_LINEAR -> saldoApresentacao.multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP);
            case CENTIMETRO_LINEAR -> saldoApresentacao.setScale(2, RoundingMode.HALF_UP);
            case METRO_QUADRADO -> saldoApresentacao.multiply(new BigDecimal("10000"))
                    .divide(larguraCm, 2, RoundingMode.HALF_UP);
            case CENTIMETRO_QUADRADO -> saldoApresentacao.divide(larguraCm, 2, RoundingMode.HALF_UP);
            default -> null;
        };

        if (comprimentoCm == null) {
            return null;
        }

        return formatarNumero(larguraCm) + "cm x " + formatarNumero(comprimentoCm) + "cm";
    }

    private BigDecimal extrairLarguraMm(LoteMateriaPrima lote) {
        Object larguraValue = LogicalMapKeySupport.getLogicalValue(lote.getAtributos(), "larguraMm");
        if (larguraValue == null) {
            return null;
        }

        if (larguraValue instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }

        return new BigDecimal(larguraValue.toString()).setScale(2, RoundingMode.HALF_UP);
    }

    private String formatarNumero(BigDecimal valor) {
        return HumanNumberDisplayFormatter.format(valor, 2);
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
        responseDTO.setUnidadeDeEstoque(unidadeCadastro);
        responseDTO.setUnidadeCadastroEstoque(unidadeCadastro);
        responseDTO.setUnidadeSimbolo(unidadeCadastro.getSimbolo());
        responseDTO.setSaldoEstoque(calcularSaldoApresentacao(lote, saldoInterno));
        responseDTO.setSaldoInternoAtual(saldoInterno);
        responseDTO.setValorAtualLote(calcularValorAtualLote(lote));
        responseDTO.setCustoUnitarioAtual(calcularCustoUnitarioAtualApresentacao(lote));
        responseDTO.setIdentificadorPublico(LotePublicIdentifierFormatter.format(lote));
        responseDTO.setIdentificadorOrigemPublico(lote.getLoteDeOrigem() != null
                ? LotePublicIdentifierFormatter.format(lote.getLoteDeOrigem())
                : null);
        responseDTO.setTipoEstrutural(LotePublicIdentifierFormatter.resolverTipoEstrutural(lote));
    }

    private BigDecimal calcularSaldoApresentacao(LoteMateriaPrima lote, BigDecimal saldoInterno) {
        if (lote.getSaldoEstoque() != null) {
            return lote.getSaldoEstoque();
        }
        return converterQuantidadeParaApresentacao(lote, saldoInterno);
    }

    private BigDecimal calcularValorAtualLote(LoteMateriaPrima lote) {
        if (lote.getValorAtualLote() != null) {
            return lote.getValorAtualLote().setScale(SCALE_MONEY, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(SCALE_MONEY, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularCustoUnitarioAtualApresentacao(LoteMateriaPrima lote) {
        if (lote.getCustoUnitarioAtual() != null) {
            return lote.getCustoUnitarioAtual().setScale(SCALE_MONEY, RoundingMode.HALF_UP);
        }
        return BigDecimal.ZERO.setScale(SCALE_MONEY, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularCustoUnitarioAtualInterno(
            LoteMateriaPrima lote,
            BigDecimal saldoAtualInterno,
            BigDecimal valorAtualLote
    ) {
        if (saldoAtualInterno.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO.setScale(SCALE_MONEY, RoundingMode.HALF_UP);
        }
        return valorAtualLote.divide(saldoAtualInterno, SCALE_MONEY, RoundingMode.HALF_UP);
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
            ContextoItensImpactadosAjusteLote contextoItensImpactados,
            List<ItemImpactadoCalculado> itensImpactados
    ) {}

    private record ItemImpactadoCalculado(
            LoteMateriaPrima lote,
            String tipoItem,
            String descricao,
            String identificadorPublico,
            String identificadorOrigemPublico,
            Integer nivelArvore,
            String cadeiaPublica,
            List<String> cadeiaIdentificadoresPublicos,
            BigDecimal saldoAtual,
            String saldoDescricao,
            String dimensaoDescricao,
            BigDecimal valorAtual,
            BigDecimal valorProjetado,
            boolean selecionadoPorPadrao
    ) {}
}
