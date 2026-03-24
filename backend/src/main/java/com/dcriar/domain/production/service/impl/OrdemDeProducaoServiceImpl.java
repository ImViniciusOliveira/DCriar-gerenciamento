package com.dcriar.domain.production.service.impl;

import com.dcriar.api.dto.request.product.AjusteEstoqueRequestDTO;
import com.dcriar.api.dto.request.product.MovimentacaoEstoqueProdutoRequestDTO;
import com.dcriar.api.dto.request.production.*;
import com.dcriar.api.dto.response.product.EstoqueResponseDTO;
import com.dcriar.api.dto.request.stock.MovimentacaoRequestDTO;
import com.dcriar.api.dto.response.production.CorteRealizadoResponseDTO;
import com.dcriar.api.dto.response.production.OrdemDeConsumoResponseDTO;
import com.dcriar.api.dto.response.production.OrdemDeProducaoResponseDTO;
import com.dcriar.api.dto.response.production.SimulacaoConsumoResponseDTO;
import com.dcriar.api.dto.response.production.SimulacaoCorteResponseDTO;
import com.dcriar.api.mapper.production.OrdemDeProducaoMapper;
import com.dcriar.api.mapper.production.PlanoDeConsumoMapper;
import com.dcriar.domain.product.entity.MovimentacaoEstoqueProduto;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.entity.ProdutoDeCorte;
import com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto;
import com.dcriar.domain.product.repository.MovimentacaoEstoqueProdutoRepository;
import com.dcriar.domain.product.repository.ProdutoRepository;
import com.dcriar.domain.product.service.EstoqueProdutoService;
import com.dcriar.domain.production.entity.CorteRealizado;
import com.dcriar.domain.production.entity.Margens;
import com.dcriar.domain.production.entity.OrdemDeProducao;
import com.dcriar.domain.production.enums.ModoCalculo;
import com.dcriar.domain.production.model.ParametrosCorte;
import com.dcriar.domain.production.model.PlanoDeConsumo;
import com.dcriar.domain.production.model.PlanoDeConsumoItem;
import com.dcriar.domain.production.model.ResumoLayoutCorte;
import com.dcriar.domain.production.repository.OrdemDeProducaoRepository;
import com.dcriar.domain.production.service.CorteCalculatorService;
import com.dcriar.domain.production.service.OrdemDeProducaoService;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.entity.MovimentacaoEstoqueLote;
import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.dcriar.domain.stock.model.LoteRetalhoHierarchyItem;
import com.dcriar.domain.stock.repository.LoteMateriaPrimaRepository;
import com.dcriar.domain.stock.repository.MovimentacaoEstoqueLoteRepository;
import com.dcriar.domain.stock.service.LoteRetalhoHierarchyService;
import com.dcriar.exception.custom.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Implementação do serviço para gerir Ordens de Produção.
 * <p>
 * Esta classe orquestra a criação de ordens por corte (com otimização de layout)
 * e por consumo, gerenciando a movimentação de estoque de matéria-prima e produtos acabados.
 * <p>
 * <b>Responsabilidades Principais:</b>
 * <ul>
 * <li>Criar ordens de produção, validando a compatibilidade do tipo de produção.</li>
 * <li>Orquestrar a baixa no estoque de matéria-prima.</li>
 * <li>Orquestrar a entrada no estoque de produtos acabados.</li>
 * <li>Gerar novos lotes de matéria-prima a partir de sobras (retalhos) em ordens de corte.</li>
 * <li>Distribuir opcionalmente o estoque produzido para canais de venda.</li>
 * <li>Gerenciar o estorno completo em caso de exclusão da ordem.</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrdemDeProducaoServiceImpl implements OrdemDeProducaoService {

    private final OrdemDeProducaoRepository ordemDeProducaoRepository;
    private final ProdutoRepository produtoRepository;
    private final LoteMateriaPrimaRepository loteMateriaPrimaRepository;
    private final MovimentacaoEstoqueLoteRepository movimentacaoEstoqueLoteRepository;
    private final MovimentacaoEstoqueProdutoRepository movimentacaoEstoqueProdutoRepository;
    private final OrdemDeProducaoMapper ordemDeProducaoMapper;
    private final CorteCalculatorService corteCalculatorService;
    private final EstoqueProdutoService estoqueProdutoService;
    private final PlanoDeConsumoMapper planoDeConsumoMapper;
    private final LoteRetalhoHierarchyService loteRetalhoHierarchyService;

    @Override
    @Transactional
    public OrdemDeProducaoResponseDTO criarOrdemDeCorte(OrdemDeCorteRequestDTO requestDTO) {

        // 1. Validações iniciais e busca de entidades principais.
        Produto produto = findProdutoById(requestDTO.getProdutoId());
        if (!produto.getTipoMateriaPrima().getUnidadeDeConsumo().isPermiteCorte()) {
            throw TipoProducaoIncompativelException.produtoNaoPermiteCorte(
                    produto.getNome(),
                    produto.getTipoMateriaPrima().getUnidadeDeConsumo().name()
            );
        }

        if (requestDTO.getLoteId() == null) {
            throw LotePrincipalNaoEspecificadoException.paraProducaoPorCorte();
        }
        LoteMateriaPrima lotePrincipal = findLoteById(requestDTO.getLoteId());
        validarCompatibilidadeMaterialEntreProdutoELote(produto, lotePrincipal);
        carregarSaldoAtualNoLote(lotePrincipal);

        BigDecimal consumoTotalLote;
        BigDecimal comprimentoFinalCm;
        List<CorteRealizadoResponseDTO> cortesRealizadosDTOs;
        BigDecimal larguraFinalCm;
        ParametrosCorte parametros;
        boolean isModoManual = requestDTO.getModoCalculo() == ModoCalculo.MANUAL;

        // 2. Determina os parâmetros de corte (manual ou automático).
        if (isModoManual) {
            parametros = corteCalculatorService.extrairParametrosCorteManual(
                    requestDTO.getQuantidadeProduzida(),
                    produto,
                    lotePrincipal,
                    requestDTO.getLarguraBlocoProdutosCm(),
                    requestDTO.getComprimentoBlocoProdutosCm()
            );
            larguraFinalCm = parametros.larguraTotalLoteCm();
            comprimentoFinalCm = requestDTO.getComprimentoBlocoProdutosCm();
            ResumoLayoutCorte resumo = corteCalculatorService.calcularLayoutDetalhado(parametros, comprimentoFinalCm, true);
            cortesRealizadosDTOs = resumo.cortes();

        } else { // MODO AUTOMÁTICO - LÓGICA CORRIGIDA
            // ETAPA 1: Calcular layout físico com margens zero para validar a capacidade.
            ParametrosCorte parametrosBase = corteCalculatorService.extrairParametrosCorte(
                    requestDTO.getQuantidadeProduzida(), produto, lotePrincipal, null // Força margens zero
            );

            // ETAPA 2: Validar as margens do usuário e criar os parâmetros finais.
            BigDecimal margemEsquerda = Optional.ofNullable(requestDTO.getMargens()).map(MargensRequestDTO::getEsquerda).orElse(BigDecimal.ZERO);
            BigDecimal margemDireita = Optional.ofNullable(requestDTO.getMargens()).map(MargensRequestDTO::getDireita).orElse(BigDecimal.ZERO);

            BigDecimal larguraProdutosAgrupados = parametrosBase.larguraBlocoProdutosCm();
            BigDecimal larguraBlocoFinalComMargens = larguraProdutosAgrupados
                    .add(margemEsquerda)
                    .add(margemDireita);

            if (larguraBlocoFinalComMargens.compareTo(BigDecimal.ZERO) <= 0) {
                throw MargemInvalidaException.larguraFinalNaoPositiva(
                        larguraBlocoFinalComMargens,
                        larguraProdutosAgrupados,
                        margemEsquerda.add(margemDireita)
                );
            }

            if (larguraBlocoFinalComMargens.compareTo(parametrosBase.larguraTotalLoteCm()) > 0) {
                throw MargemInvalidaException.larguraComMargensExcedeLote(
                        larguraProdutosAgrupados,
                        margemEsquerda,
                        margemDireita,
                        parametrosBase.larguraTotalLoteCm()
                );
            }
            BigDecimal larguraRetalhoFinal = parametrosBase.larguraTotalLoteCm().subtract(larguraBlocoFinalComMargens);

            // ETAPA 3: Construir o objeto de parâmetros final para a ordem.
            parametros = new ParametrosCorte(
                    parametrosBase.larguraTotalLoteCm(),
                    parametrosBase.larguraProduto(),
                    parametrosBase.comprimentoProduto(),
                    parametrosBase.quantidade(),
                    margemEsquerda,
                    margemDireita,
                    parametrosBase.produtosPorLinha(),
                    parametrosBase.rotacionado(),
                    larguraBlocoFinalComMargens,
                    larguraRetalhoFinal
            );

            // Calcula o comprimento final
            long numeroDeLinhas = (long) Math.ceil((double) requestDTO.getQuantidadeProduzida() / parametros.produtosPorLinha());
            comprimentoFinalCm = parametros.comprimentoProduto().multiply(new BigDecimal(numeroDeLinhas));
            if (requestDTO.getMargens() != null) {
                comprimentoFinalCm = comprimentoFinalCm
                        .add(Optional.ofNullable(requestDTO.getMargens().getSuperior()).orElse(BigDecimal.ZERO))
                        .add(Optional.ofNullable(requestDTO.getMargens().getInferior()).orElse(BigDecimal.ZERO));
            }
            if (comprimentoFinalCm.compareTo(BigDecimal.ZERO) <= 0) {
                throw MargemInvalidaException.comprimentoFinalNaoPositivo(comprimentoFinalCm);
            }

            ResumoLayoutCorte resumo = corteCalculatorService.calcularLayoutDetalhado(parametros, comprimentoFinalCm, false);
            cortesRealizadosDTOs = resumo.cortes();
            larguraFinalCm = parametros.larguraTotalLoteCm();
        }

        consumoTotalLote = calcularConsumoCorteNaUnidadeDoLote(lotePrincipal, parametros.larguraTotalLoteCm(), comprimentoFinalCm);

        // 3. Valida se o lote principal tem saldo suficiente.
        validarSaldoLoteCorte(lotePrincipal, consumoTotalLote);

        // 4. Cria e persiste a Ordem de Produção e seus cortes.
        Margens margensEntity = null;
        MargensRequestDTO margensRequest = requestDTO.getMargens();
        if (requestDTO.getModoCalculo() == ModoCalculo.AUTOMATICO && margensRequest != null) {
            margensEntity = ordemDeProducaoMapper.toMargensEntity(margensRequest);
        }

        OrdemDeProducaoRequestDTO ordemRequestDTO = OrdemDeProducaoRequestDTO.builder()
                .produtoId(produto.getId())
                .lotesConsumidosIds(Set.of(lotePrincipal.getId()))
                .canalVendaDestinoId(requestDTO.getCanalVendaDestinoId() != null ? requestDTO.getCanalVendaDestinoId() : null)
                .quantidadeProduzida(requestDTO.getQuantidadeProduzida())
                .modoCalculo(requestDTO.getModoCalculo().name())
                .margens(margensRequest)
                .larguraFinalCm(larguraFinalCm)
                .comprimentoFinalCm(comprimentoFinalCm)
                .motivo(requestDTO.getMotivo())
                .rotacionado(parametros.rotacionado())
                .build();

        OrdemDeProducao ordem = OrdemDeProducao.from(ordemRequestDTO, produto, Set.of(lotePrincipal), margensEntity);

        for (CorteRealizadoResponseDTO dto : cortesRealizadosDTOs) {
            ordem.addCorteRealizado(CorteRealizado.from(
                    CorteRealizadoRequestDTO.builder()
                            .larguraCm(dto.getLarguraCm())
                            .comprimentoCm(dto.getComprimentoCm())
                            .quantidade(dto.getQuantidade())
                            .tipo(dto.getTipo())
                            .retalhoCategoria(dto.getRetalhoCategoria())
                            .repeticoes(resolveRepeticoes(dto))
                            .ordemDeProducaoId(ordem.getId())
                            .build(),
                    ordem
            ));
        }

        OrdemDeProducao savedOrdem = ordemDeProducaoRepository.save(ordem);

        // 5. Orquestra as movimentações de estoque.
        registrarSaidaLote(lotePrincipal, consumoTotalLote, "Consumido pela Ordem de Produção #" + savedOrdem.getId(), savedOrdem);
        registrarEntradaProduto(produto, requestDTO.getQuantidadeProduzida(), "Produzido via Ordem de Produção #" + savedOrdem.getId(), savedOrdem);
        distribuirEstoqueParaCanal(savedOrdem.getProduto().getId(), requestDTO.getCanalVendaDestinoId(), requestDTO.getQuantidadeProduzida());

        // 6. Criação de Lotes de Retalho
        for (CorteRealizadoResponseDTO dto : cortesRealizadosDTOs) {
             if ("RETALHO".equals(dto.getTipo())) {
                 criarLoteDeRetalho(lotePrincipal, dto.getLarguraCm(), dto.getComprimentoCm(), savedOrdem);
             }
        }

        return ordemDeProducaoMapper.toDto(savedOrdem);
    }

    @Override
    @Transactional
    public OrdemDeConsumoResponseDTO criarOrdemDeConsumo(OrdemDeConsumoRequestDTO requestDTO) {
        // 1. Validações iniciais e busca de entidades.
        Produto produto = findProdutoById(requestDTO.getProdutoId());
        if (!produto.getTipoMateriaPrima().getUnidadeDeConsumo().isConsumo()) {
            throw TipoProducaoIncompativelException.produtoNaoEhConsumo(
                    produto.getNome(),
                    produto.getTipoMateriaPrima().getUnidadeDeConsumo().name()
            );
        }

        LoteMateriaPrima loteConsumido = findLoteById(requestDTO.getLoteId());

        // 2. Valida se o saldo do lote é suficiente.
        BigDecimal consumoTotalNecessario = produto.getUnidadesPorProduto()
                .multiply(BigDecimal.valueOf(requestDTO.getQuantidadeProduzida()));
        BigDecimal saldoDisponivel = movimentacaoEstoqueLoteRepository.findSaldoByLote(loteConsumido);

        if (saldoDisponivel.compareTo(consumoTotalNecessario) < 0) {
            throw new SaldoMateriaPrimaInsuficienteException(consumoTotalNecessario, saldoDisponivel);
        }

        // 3. Cria e persiste a Ordem de Produção.
        OrdemDeProducaoRequestDTO ordemRequestDTO = OrdemDeProducaoRequestDTO.builder()
                .produtoId(produto.getId())
                .lotesConsumidosIds(Set.of(loteConsumido.getId())) // Mantém a estrutura da entidade
                .canalVendaDestinoId(requestDTO.getCanalVendaDestinoId())
                .quantidadeProduzida(requestDTO.getQuantidadeProduzida())
                .motivo(requestDTO.getMotivo())
                // Não seta modoCalculo, dimensões, etc., pois não se aplicam
                .build();

        OrdemDeProducao ordem = OrdemDeProducao.from(ordemRequestDTO, produto, Set.of(loteConsumido), null);
        OrdemDeProducao savedOrdem = ordemDeProducaoRepository.save(ordem);

        // 4. Orquestra as movimentações de estoque.
        registrarSaidaLote(loteConsumido, consumoTotalNecessario, "Consumido pela Ordem de Produção #" + savedOrdem.getId(), savedOrdem);
        registrarEntradaProduto(produto, requestDTO.getQuantidadeProduzida(), "Produzido via Ordem de Produção #" + savedOrdem.getId(), savedOrdem);
        distribuirEstoqueParaCanal(savedOrdem.getProduto().getId(), requestDTO.getCanalVendaDestinoId(), requestDTO.getQuantidadeProduzida());

        return OrdemDeConsumoResponseDTO.builder()
                .id(savedOrdem.getId())
                .produtoId(savedOrdem.getProduto().getId())
                .nomeProduto(savedOrdem.getProduto().getNome())
                .lotesConsumidosIds(savedOrdem.getLotesConsumidos().stream().map(LoteMateriaPrima::getId).toList())
                .quantidadeProduzida(savedOrdem.getQuantidadeProduzida())
                .dataCriacao(savedOrdem.getDataCriacao())
                .dataAtualizacao(savedOrdem.getDataAtualizacao())
                .motivo(savedOrdem.getMotivo())
                .build();
    }

    @Override
    @Transactional
    public OrdemDeProducaoResponseDTO atualizarOrdemDeCorte(Long id, OrdemDeCorteRequestDTO requestDTO) {
        OrdemDeProducao ordemExistente = findOrdemByIdWithDetails(id);
        prepararOrdemParaReprocessamento(ordemExistente);

        Produto produto = findProdutoById(requestDTO.getProdutoId());
        if (!produto.getTipoMateriaPrima().getUnidadeDeConsumo().isPermiteCorte()) {
            throw TipoProducaoIncompativelException.produtoNaoPermiteCorte(
                    produto.getNome(),
                    produto.getTipoMateriaPrima().getUnidadeDeConsumo().name()
            );
        }

        if (requestDTO.getLoteId() == null) {
            throw LotePrincipalNaoEspecificadoException.paraProducaoPorCorte();
        }
        LoteMateriaPrima lotePrincipal = findLoteById(requestDTO.getLoteId());
        validarCompatibilidadeMaterialEntreProdutoELote(produto, lotePrincipal);
        carregarSaldoAtualNoLote(lotePrincipal);

        BigDecimal comprimentoFinalCm;
        BigDecimal larguraFinalCm;
        List<CorteRealizadoResponseDTO> cortesRealizadosDTOs;
        ParametrosCorte parametros;
        boolean isModoManual = requestDTO.getModoCalculo() == ModoCalculo.MANUAL;

        if (isModoManual) {
            parametros = corteCalculatorService.extrairParametrosCorteManual(
                    requestDTO.getQuantidadeProduzida(),
                    produto,
                    lotePrincipal,
                    requestDTO.getLarguraBlocoProdutosCm(),
                    requestDTO.getComprimentoBlocoProdutosCm()
            );
            larguraFinalCm = parametros.larguraTotalLoteCm();
            comprimentoFinalCm = requestDTO.getComprimentoBlocoProdutosCm();
            ResumoLayoutCorte resumo = corteCalculatorService.calcularLayoutDetalhado(parametros, comprimentoFinalCm, true);
            cortesRealizadosDTOs = resumo.cortes();
        } else {
            ParametrosCorte parametrosBase = corteCalculatorService.extrairParametrosCorte(
                    requestDTO.getQuantidadeProduzida(), produto, lotePrincipal, null
            );

            BigDecimal margemEsquerda = Optional.ofNullable(requestDTO.getMargens()).map(MargensRequestDTO::getEsquerda).orElse(BigDecimal.ZERO);
            BigDecimal margemDireita = Optional.ofNullable(requestDTO.getMargens()).map(MargensRequestDTO::getDireita).orElse(BigDecimal.ZERO);

            BigDecimal larguraProdutosAgrupados = parametrosBase.larguraBlocoProdutosCm();
            BigDecimal larguraBlocoFinalComMargens = larguraProdutosAgrupados
                    .add(margemEsquerda)
                    .add(margemDireita);

            if (larguraBlocoFinalComMargens.compareTo(BigDecimal.ZERO) <= 0) {
                throw MargemInvalidaException.larguraFinalNaoPositiva(
                        larguraBlocoFinalComMargens,
                        larguraProdutosAgrupados,
                        margemEsquerda.add(margemDireita)
                );
            }

            if (larguraBlocoFinalComMargens.compareTo(parametrosBase.larguraTotalLoteCm()) > 0) {
                throw MargemInvalidaException.larguraComMargensExcedeLote(
                        larguraProdutosAgrupados,
                        margemEsquerda,
                        margemDireita,
                        parametrosBase.larguraTotalLoteCm()
                );
            }
            BigDecimal larguraRetalhoFinal = parametrosBase.larguraTotalLoteCm().subtract(larguraBlocoFinalComMargens);

            parametros = new ParametrosCorte(
                    parametrosBase.larguraTotalLoteCm(),
                    parametrosBase.larguraProduto(),
                    parametrosBase.comprimentoProduto(),
                    parametrosBase.quantidade(),
                    margemEsquerda,
                    margemDireita,
                    parametrosBase.produtosPorLinha(),
                    parametrosBase.rotacionado(),
                    larguraBlocoFinalComMargens,
                    larguraRetalhoFinal
            );

            long numeroDeLinhas = (long) Math.ceil((double) requestDTO.getQuantidadeProduzida() / parametros.produtosPorLinha());
            comprimentoFinalCm = parametros.comprimentoProduto().multiply(new BigDecimal(numeroDeLinhas));
            if (requestDTO.getMargens() != null) {
                comprimentoFinalCm = comprimentoFinalCm
                        .add(Optional.ofNullable(requestDTO.getMargens().getSuperior()).orElse(BigDecimal.ZERO))
                        .add(Optional.ofNullable(requestDTO.getMargens().getInferior()).orElse(BigDecimal.ZERO));
            }
            if (comprimentoFinalCm.compareTo(BigDecimal.ZERO) <= 0) {
                throw MargemInvalidaException.comprimentoFinalNaoPositivo(comprimentoFinalCm);
            }

            ResumoLayoutCorte resumo = corteCalculatorService.calcularLayoutDetalhado(parametros, comprimentoFinalCm, false);
            cortesRealizadosDTOs = resumo.cortes();
            larguraFinalCm = parametros.larguraTotalLoteCm();
        }

        BigDecimal consumoTotalLote = calcularConsumoCorteNaUnidadeDoLote(
                lotePrincipal,
                parametros.larguraTotalLoteCm(),
                comprimentoFinalCm
        );
        validarSaldoLoteCorte(lotePrincipal, consumoTotalLote);

        MargensRequestDTO margensRequest = requestDTO.getMargens();
        Margens margensEntity = null;
        if (requestDTO.getModoCalculo() == ModoCalculo.AUTOMATICO && margensRequest != null) {
            margensEntity = ordemDeProducaoMapper.toMargensEntity(margensRequest);
        }

        OrdemDeProducaoRequestDTO ordemRequestDTO = OrdemDeProducaoRequestDTO.builder()
                .produtoId(produto.getId())
                .lotesConsumidosIds(Set.of(lotePrincipal.getId()))
                .canalVendaDestinoId(requestDTO.getCanalVendaDestinoId())
                .quantidadeProduzida(requestDTO.getQuantidadeProduzida())
                .modoCalculo(requestDTO.getModoCalculo().name())
                .margens(margensRequest)
                .larguraFinalCm(larguraFinalCm)
                .comprimentoFinalCm(comprimentoFinalCm)
                .motivo(requestDTO.getMotivo())
                .rotacionado(parametros.rotacionado())
                .build();

        ordemExistente.updateFrom(ordemRequestDTO, produto, new HashSet<>(Set.of(lotePrincipal)), margensEntity);
        ordemExistente.getCortesRealizados().clear();

        for (CorteRealizadoResponseDTO dto : cortesRealizadosDTOs) {
            ordemExistente.addCorteRealizado(CorteRealizado.from(
                    CorteRealizadoRequestDTO.builder()
                            .larguraCm(dto.getLarguraCm())
                            .comprimentoCm(dto.getComprimentoCm())
                            .quantidade(dto.getQuantidade())
                            .tipo(dto.getTipo())
                            .retalhoCategoria(dto.getRetalhoCategoria())
                            .repeticoes(resolveRepeticoes(dto))
                            .ordemDeProducaoId(ordemExistente.getId())
                            .build(),
                    ordemExistente
            ));
        }

        OrdemDeProducao savedOrdem = ordemDeProducaoRepository.save(ordemExistente);

        registrarSaidaLote(lotePrincipal, consumoTotalLote, "Consumido pela Ordem de Produção #" + savedOrdem.getId(), savedOrdem);
        registrarEntradaProduto(produto, requestDTO.getQuantidadeProduzida(), "Produzido via Ordem de Produção #" + savedOrdem.getId(), savedOrdem);
        distribuirEstoqueParaCanal(savedOrdem.getProduto().getId(), requestDTO.getCanalVendaDestinoId(), requestDTO.getQuantidadeProduzida());

        for (CorteRealizadoResponseDTO dto : cortesRealizadosDTOs) {
            if ("RETALHO".equals(dto.getTipo())) {
                criarLoteDeRetalho(lotePrincipal, dto.getLarguraCm(), dto.getComprimentoCm(), savedOrdem);
            }
        }

        return ordemDeProducaoMapper.toDto(savedOrdem);
    }

    @Override
    @Transactional
    public OrdemDeProducaoResponseDTO atualizarOrdemDeConsumo(Long id, OrdemDeConsumoRequestDTO requestDTO) {
        OrdemDeProducao ordemExistente = findOrdemByIdWithDetails(id);
        prepararOrdemParaReprocessamento(ordemExistente);

        Produto produto = findProdutoById(requestDTO.getProdutoId());
        if (!produto.getTipoMateriaPrima().getUnidadeDeConsumo().isConsumo()) {
            throw TipoProducaoIncompativelException.produtoNaoEhConsumo(
                    produto.getNome(),
                    produto.getTipoMateriaPrima().getUnidadeDeConsumo().name()
            );
        }

        LoteMateriaPrima loteConsumido = findLoteById(requestDTO.getLoteId());
        validarCompatibilidadeMaterialEntreProdutoELote(produto, loteConsumido);

        BigDecimal consumoTotalNecessario = produto.getUnidadesPorProduto()
                .multiply(BigDecimal.valueOf(requestDTO.getQuantidadeProduzida()));
        BigDecimal saldoDisponivel = movimentacaoEstoqueLoteRepository.findSaldoByLote(loteConsumido);

        if (saldoDisponivel.compareTo(consumoTotalNecessario) < 0) {
            throw new SaldoMateriaPrimaInsuficienteException(consumoTotalNecessario, saldoDisponivel);
        }

        OrdemDeProducaoRequestDTO ordemRequestDTO = OrdemDeProducaoRequestDTO.builder()
                .produtoId(produto.getId())
                .lotesConsumidosIds(Set.of(loteConsumido.getId()))
                .canalVendaDestinoId(requestDTO.getCanalVendaDestinoId())
                .quantidadeProduzida(requestDTO.getQuantidadeProduzida())
                .motivo(requestDTO.getMotivo())
                .build();

        ordemExistente.updateFrom(ordemRequestDTO, produto, new HashSet<>(Set.of(loteConsumido)), null);
        ordemExistente.getCortesRealizados().clear();

        OrdemDeProducao savedOrdem = ordemDeProducaoRepository.save(ordemExistente);

        registrarSaidaLote(loteConsumido, consumoTotalNecessario, "Consumido pela Ordem de Produção #" + savedOrdem.getId(), savedOrdem);
        registrarEntradaProduto(produto, requestDTO.getQuantidadeProduzida(), "Produzido via Ordem de Produção #" + savedOrdem.getId(), savedOrdem);
        distribuirEstoqueParaCanal(savedOrdem.getProduto().getId(), requestDTO.getCanalVendaDestinoId(), requestDTO.getQuantidadeProduzida());

        return ordemDeProducaoMapper.toDto(savedOrdem);
    }

    /**
     * Exclui uma ordem de produção e estorna todas as movimentações de estoque associadas.
     * <p>
     * A exclusão é uma operação crítica que só é permitida se a integridade do estoque
     * for mantida. O método realiza as seguintes validações antes de prosseguir:
     * <ol>
     *     <li>Verifica se a quantidade de produto acabado gerada pela ordem ainda está disponível no estoque. Se parte já foi vendida, a exclusão é bloqueada.</li>
     *     <li>Verifica se os lotes de retalho gerados pela ordem ainda estão intactos (não foram utilizados em outras produções). Se algum retalho já foi consumido, a exclusão é bloqueada.</li>
     * </ol>
     * Se as validações passarem, o método reverte todas as operações:
     * <ul>
     *     <li>Cria uma movimentação de saída para o produto acabado (estorno).</li>
     *     <li>Cria movimentações de entrada para a matéria-prima (devolução aos lotes originais).</li>
     *     <li>Exclui fisicamente os lotes de retalho gerados.</li>
     *     <li>Exclui a ordem de produção.</li>
     * </ul>
     *
     * @param id O ID da ordem de produção a ser excluída.
     * @throws OrdemDeProducaoNaoEncontradaException se a ordem não existir.
     * @throws ImpossivelExcluirProducaoException se as regras de integridade forem violadas.
     */
    @Override
    @Transactional
    public void excluir(Long id) {
        OrdemDeProducao ordem = findOrdemByIdWithDetails(id);
        prepararOrdemParaReprocessamento(ordem);
        ordem.getLotesConsumidos().clear();
        ordemDeProducaoRepository.delete(ordem);
    }

    @Override
    @Transactional(readOnly = true)
    public OrdemDeProducaoResponseDTO buscarPorId(Long id) {
        return ordemDeProducaoRepository.findByIdWithDetails(id)
                .map(this::toDetailedDto)
                .orElseThrow(() -> new OrdemDeProducaoNaoEncontradaException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrdemDeProducaoResponseDTO> listarPaginado(Pageable pageable) {
        Page<OrdemDeProducao> ordensPage = ordemDeProducaoRepository.findAll(pageable);
        return ordensPage.map(this::toDetailedDto);
    }

    @Override
    public SimulacaoCorteResponseDTO simularCorte(SimulacaoCorteRequestDTO requestDTO) {
        Produto produto = findProdutoById(requestDTO.getProdutoId());
        LoteMateriaPrima loteParaSimulacao = findLoteById(requestDTO.getLoteId());
        validarCompatibilidadeMaterialEntreProdutoELote(produto, loteParaSimulacao);
        carregarSaldoDisponivelParaEdicaoNoLote(loteParaSimulacao, requestDTO.getOrdemId());

        ParametrosCorte parametros = corteCalculatorService.extrairParametrosCorte(
            requestDTO.getQuantidade(), produto, loteParaSimulacao, null
        );
        BigDecimal larguraFinalCm = parametros.larguraTotalLoteCm();
        long numeroDeLinhasTotal = (long) Math.ceil((double) requestDTO.getQuantidade() / parametros.produtosPorLinha());
        BigDecimal comprimentoBlocoProdutosCm = parametros.comprimentoProduto().multiply(new BigDecimal(numeroDeLinhasTotal));
        BigDecimal consumoEstimado = calcularConsumoCorteNaUnidadeDoLote(loteParaSimulacao, larguraFinalCm, comprimentoBlocoProdutosCm);
        validarSaldoLoteCorte(loteParaSimulacao, consumoEstimado);

        ResumoLayoutCorte resumo = corteCalculatorService.calcularLayoutDetalhado(parametros, comprimentoBlocoProdutosCm, false);

        return SimulacaoCorteResponseDTO.builder()
                .modoCalculo(ModoCalculo.AUTOMATICO)
                .larguraFinalCm(larguraFinalCm)
                .comprimentoFinalCm(comprimentoBlocoProdutosCm)
                .consumoEstimado(consumoEstimado)
                .rotacionado(parametros.rotacionado())
                .produtosPorLinha(resumo.produtosPorLinha())
                .numeroLinhasCompletas(resumo.numeroLinhasCompletas())
                .produtosNaUltimaLinha(resumo.produtosNaUltimaLinha())
                .sobraLateral(resumo.sobraLateral())
                .sobraInferior(resumo.sobraInferior())
                .saldoRolo(resumo.saldoRolo())
                .dimensaoProduto(formatarDimensao(parametros.larguraProduto(), parametros.comprimentoProduto()))
                .consumoTotal(formatarDimensao(larguraFinalCm, comprimentoBlocoProdutosCm))
                .larguraBlocoProdutosCm(parametros.larguraBlocoProdutosCm())
                .comprimentoBlocoProdutosCm(comprimentoBlocoProdutosCm)
                .build();
    }

    @Override
    public SimulacaoCorteResponseDTO verificarCorte(VerificacaoCorteRequestDTO requestDTO) {
        Produto produto = findProdutoById(requestDTO.getProdutoId());
        LoteMateriaPrima lote = findLoteById(requestDTO.getLoteId());
        validarCompatibilidadeMaterialEntreProdutoELote(produto, lote);
        carregarSaldoDisponivelParaEdicaoNoLote(lote, requestDTO.getOrdemId());

        BigDecimal comprimentoBlocoProdutosCm;
        BigDecimal larguraBlocoProdutosCm;
        BigDecimal comprimentoFinalCm;
        ParametrosCorte parametros;
        boolean isModoManual = requestDTO.getModoCalculo() == ModoCalculo.MANUAL;

        if (isModoManual) {
            comprimentoBlocoProdutosCm = requestDTO.getComprimentoBlocoProdutosCm();
            larguraBlocoProdutosCm = requestDTO.getLarguraBlocoProdutosCm();
            comprimentoFinalCm = comprimentoBlocoProdutosCm;
            parametros = corteCalculatorService.extrairParametrosCorteManual(
                requestDTO.getQuantidade(), produto, lote, larguraBlocoProdutosCm, comprimentoBlocoProdutosCm
            );
        } else { // MODO AUTOMÁTICO
            // ETAPA 1: Calcular layout físico com margens zero para validar a capacidade.
            ParametrosCorte parametrosBase = corteCalculatorService.extrairParametrosCorte(
                    requestDTO.getQuantidade(), produto, lote, null // Força margens zero
            );

            // ETAPA 2: Validar as margens do usuário e criar os parâmetros finais.
            BigDecimal margemEsquerda = Optional.ofNullable(requestDTO.getMargens()).map(MargensRequestDTO::getEsquerda).orElse(BigDecimal.ZERO);
            BigDecimal margemDireita = Optional.ofNullable(requestDTO.getMargens()).map(MargensRequestDTO::getDireita).orElse(BigDecimal.ZERO);

            larguraBlocoProdutosCm = requestDTO.getLarguraBlocoProdutosCm() != null ? requestDTO.getLarguraBlocoProdutosCm() : parametrosBase.larguraBlocoProdutosCm();
            comprimentoBlocoProdutosCm = requestDTO.getComprimentoBlocoProdutosCm() != null ? requestDTO.getComprimentoBlocoProdutosCm() : parametrosBase.comprimentoProduto().multiply(new BigDecimal((long) Math.ceil((double) requestDTO.getQuantidade() / parametrosBase.produtosPorLinha())));

            BigDecimal larguraBlocoFinalComMargens = larguraBlocoProdutosCm.add(margemEsquerda).add(margemDireita);

            if (larguraBlocoFinalComMargens.compareTo(BigDecimal.ZERO) <= 0) {
                throw MargemInvalidaException.larguraFinalNaoPositiva(
                        larguraBlocoFinalComMargens,
                        larguraBlocoProdutosCm,
                        margemEsquerda.add(margemDireita)
                );
            }

            if (larguraBlocoFinalComMargens.compareTo(parametrosBase.larguraTotalLoteCm()) > 0) {
                throw MargemInvalidaException.larguraComMargensExcedeLote(
                        larguraBlocoProdutosCm,
                        margemEsquerda,
                        margemDireita,
                        parametrosBase.larguraTotalLoteCm()
                );
            }

            BigDecimal larguraRetalhoFinal = parametrosBase.larguraTotalLoteCm().subtract(larguraBlocoFinalComMargens);

            // ETAPA 3: Construir o objeto de parâmetros final para a resposta.
            parametros = new ParametrosCorte(
                    parametrosBase.larguraTotalLoteCm(),
                    parametrosBase.larguraProduto(),
                    parametrosBase.comprimentoProduto(),
                    parametrosBase.quantidade(),
                    margemEsquerda,
                    margemDireita,
                    parametrosBase.produtosPorLinha(),
                    parametrosBase.rotacionado(),
                    larguraBlocoFinalComMargens,
                    larguraRetalhoFinal
            );

            // Comprimento final considera margens
            comprimentoFinalCm = comprimentoBlocoProdutosCm;
            if (requestDTO.getMargens() != null) {
                comprimentoFinalCm = comprimentoFinalCm
                        .add(Optional.ofNullable(requestDTO.getMargens().getSuperior()).orElse(BigDecimal.ZERO))
                        .add(Optional.ofNullable(requestDTO.getMargens().getInferior()).orElse(BigDecimal.ZERO));
            }
            if (comprimentoFinalCm.compareTo(BigDecimal.ZERO) <= 0) {
                throw MargemInvalidaException.comprimentoFinalNaoPositivo(comprimentoFinalCm);
            }
        }

        ResumoLayoutCorte resumo = corteCalculatorService.calcularLayoutDetalhado(parametros, comprimentoFinalCm, isModoManual);

        BigDecimal consumoEstimado = calcularConsumoCorteNaUnidadeDoLote(lote, parametros.larguraTotalLoteCm(), comprimentoFinalCm);
        validarSaldoLoteCorte(lote, consumoEstimado);

        return SimulacaoCorteResponseDTO.builder()
                .modoCalculo(requestDTO.getModoCalculo())
                .larguraFinalCm(parametros.larguraTotalLoteCm())
                .comprimentoFinalCm(comprimentoFinalCm)
                .consumoEstimado(consumoEstimado)
                .rotacionado(parametros.rotacionado())
                .produtosPorLinha(resumo.produtosPorLinha())
                .numeroLinhasCompletas(resumo.numeroLinhasCompletas())
                .produtosNaUltimaLinha(resumo.produtosNaUltimaLinha())
                .sobraLateral(resumo.sobraLateral())
                .sobraInferior(resumo.sobraInferior())
                .saldoRolo(resumo.saldoRolo())
                .dimensaoProduto(formatarDimensao(parametros.larguraProduto(), parametros.comprimentoProduto()))
                .consumoTotal(formatarDimensao(parametros.larguraTotalLoteCm(), comprimentoFinalCm))
                .larguraBlocoProdutosCm(larguraBlocoProdutosCm)
                .comprimentoBlocoProdutosCm(comprimentoBlocoProdutosCm)
                .build();
    }

    @Override
    public SimulacaoConsumoResponseDTO simularConsumo(SimulacaoConsumoRequestDTO requestDTO) {
        Produto produto = findProdutoById(requestDTO.getProdutoId());
        if (!produto.getTipoMateriaPrima().getUnidadeDeConsumo().isConsumo()) {
            throw TipoProducaoIncompativelException.produtoUsaMateriaPrimaGeometrica(
                    produto.getNome(),
                    produto.getTipoMateriaPrima().getUnidadeDeConsumo().name()
            );
        }

        LoteMateriaPrima loteConsumido = findLoteById(requestDTO.getLoteId());
        UnidadeDeMedida unidadeExibicao = produto.getTipoMateriaPrima().getUnidadeDeConsumo();

        BigDecimal consumoTotalNecessario = produto.getUnidadesPorProduto()
                .multiply(BigDecimal.valueOf(requestDTO.getQuantidade()));
        BigDecimal saldoTotalDisponivel = calcularSaldoDisponivelParaEdicao(loteConsumido, requestDTO.getOrdemId());

        if (saldoTotalDisponivel.compareTo(consumoTotalNecessario) < 0) {
            throw new SaldoMateriaPrimaInsuficienteException(consumoTotalNecessario, saldoTotalDisponivel);
        }

        PlanoDeConsumo plano = new PlanoDeConsumo(
                List.of(new PlanoDeConsumoItem(loteConsumido, consumoTotalNecessario)),
                Map.of(loteConsumido.getId(), saldoTotalDisponivel.subtract(consumoTotalNecessario))
        );

        return SimulacaoConsumoResponseDTO.builder()
                .consumoTotalEstimado(converterConsumoParaUnidadeExibicao(consumoTotalNecessario, unidadeExibicao))
                .unidadeDeConsumo(unidadeExibicao)
                .unidadeDescricao(unidadeExibicao.getDescricao())
                .unidadeDescricaoPlural(unidadeExibicao.getDescricaoPlural())
                .unidadeSimbolo(unidadeExibicao.getSimbolo())
                .exibirQuantidadeComSimbolo(unidadeExibicao.isExibirQuantidadeComSimbolo())
                .planoDeConsumo(plano.itens().stream().map(planoDeConsumoMapper::toDto).collect(Collectors.toList()))
                .saldoRestante(plano.saldosRestantes().entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> converterConsumoParaUnidadeExibicao(entry.getValue(), unidadeExibicao)
                        )))
                .build();
    }

    private BigDecimal converterConsumoParaUnidadeExibicao(BigDecimal quantidadeInterna, UnidadeDeMedida unidadeExibicao) {
        return unidadeExibicao.converterQuantidadeDaUnidadeInternaParaInformada(quantidadeInterna, unidadeExibicao);
    }

    private void criarLoteDeRetalho(LoteMateriaPrima lotePrincipal, BigDecimal larguraSobraCm, BigDecimal comprimentoRetalhoCm, OrdemDeProducao ordemOrigem) {
        if (larguraSobraCm.compareTo(BigDecimal.ZERO) <= 0 || comprimentoRetalhoCm.compareTo(BigDecimal.ZERO) <= 0) return;
        
        BigDecimal quantidadeRealRetalho = calcularQuantidadeRetalhoNaUnidadeDoLote(lotePrincipal, larguraSobraCm, comprimentoRetalhoCm);

        BigDecimal custoUnitario = calcularCustoUnitario(lotePrincipal);
        
        BigDecimal custoTotalRetalho = custoUnitario.multiply(quantidadeRealRetalho);

        Map<String, Object> novosAtributos = Map.of("larguraMm", larguraSobraCm.multiply(new BigDecimal("10")).intValue());
        LoteMateriaPrima loteRetalho = LoteMateriaPrima.builder()
                .tipoMateriaPrima(lotePrincipal.getTipoMateriaPrima())
                .unidadeDeEstoque(lotePrincipal.getUnidadeDeEstoque())
                .unidadeCadastroEstoque(
                        lotePrincipal.getUnidadeCadastroEstoque() != null
                                ? lotePrincipal.getUnidadeCadastroEstoque()
                                : lotePrincipal.getUnidadeDeEstoque()
                )
                .atributos(novosAtributos)
                .loteDeOrigem(lotePrincipal)
                .ordemDeProducaoOrigem(ordemOrigem)
                .custoTotalLote(custoTotalRetalho)
                .motivo("Retalho gerado pela Ordem de Produção #" + ordemOrigem.getId())
                .build();
        
        loteRetalho = loteMateriaPrimaRepository.save(loteRetalho);

        MovimentacaoRequestDTO entradaRetalhoDTO = com.dcriar.api.dto.request.stock.MovimentacaoRequestDTO.builder()
                .tipo(TipoMovimentacao.ENTRADA_SOBRA)
                .quantidade(quantidadeRealRetalho)
                .motivo("Retalho gerado pela Ordem de Produção #" + ordemOrigem.getId())
                .build();
        MovimentacaoEstoqueLote entradaRetalho = MovimentacaoEstoqueLote.from(entradaRetalhoDTO, loteRetalho);
        entradaRetalho.setOrdemDeProducao(ordemOrigem);
        
        movimentacaoEstoqueLoteRepository.save(entradaRetalho);
    }

    private BigDecimal calcularCustoUnitario(LoteMateriaPrima lote) {
        BigDecimal saldoAtual = lote.getSaldoCalculado() != null
                ? lote.getSaldoCalculado()
                : movimentacaoEstoqueLoteRepository.findSaldoByLote(lote);

        if (possuiAjusteOuPerdaManual(lote)) {
            if (saldoAtual.compareTo(BigDecimal.ZERO) <= 0 || lote.getCustoTotalLote() == null) {
                return BigDecimal.ZERO;
            }
            return lote.getCustoTotalLote().divide(saldoAtual, 4, RoundingMode.HALF_UP);
        }

        BigDecimal quantidadeBaseComCusto = calcularQuantidadeBaseComCusto(lote);
        if (quantidadeBaseComCusto.compareTo(BigDecimal.ZERO) <= 0 || lote.getCustoTotalLote() == null) {
            return BigDecimal.ZERO;
        }

        return lote.getCustoTotalLote().divide(quantidadeBaseComCusto, 4, RoundingMode.HALF_UP);
    }

    private boolean possuiAjusteOuPerdaManual(LoteMateriaPrima lote) {
        return lote.getMovimentacoes().stream()
                .map(MovimentacaoEstoqueLote::getTipo)
                .anyMatch(tipo -> tipo == TipoMovimentacao.AJUSTE_INVENTARIO || tipo == TipoMovimentacao.PERDA_DESCARTE);
    }

    private BigDecimal calcularQuantidadeBaseComCusto(LoteMateriaPrima lote) {
        BigDecimal quantidadeEntradaCompra = somarQuantidadePorTipo(lote, TipoMovimentacao.ENTRADA_COMPRA);
        if (quantidadeEntradaCompra.compareTo(BigDecimal.ZERO) > 0) {
            return quantidadeEntradaCompra;
        }

        BigDecimal quantidadeEntradaSobra = somarQuantidadePorTipo(lote, TipoMovimentacao.ENTRADA_SOBRA);
        if (quantidadeEntradaSobra.compareTo(BigDecimal.ZERO) > 0) {
            return quantidadeEntradaSobra;
        }

        BigDecimal quantidadeAjustePositiva = lote.getMovimentacoes().stream()
                .filter(movimentacao -> movimentacao.getTipo() == TipoMovimentacao.AJUSTE_INVENTARIO)
                .map(MovimentacaoEstoqueLote::getQuantidade)
                .filter(quantidade -> quantidade.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return quantidadeAjustePositiva.compareTo(BigDecimal.ZERO) > 0
                ? quantidadeAjustePositiva
                : BigDecimal.ZERO;
    }

    private BigDecimal somarQuantidadePorTipo(LoteMateriaPrima lote, TipoMovimentacao tipoMovimentacao) {
        return lote.getMovimentacoes().stream()
                .filter(movimentacao -> movimentacao.getTipo() == tipoMovimentacao)
                .map(MovimentacaoEstoqueLote::getQuantidade)
                .filter(quantidade -> quantidade.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void registrarSaidaLote(LoteMateriaPrima lote, BigDecimal quantidade, String motivo, OrdemDeProducao ordem) {
        MovimentacaoRequestDTO saidaDTO = com.dcriar.api.dto.request.stock.MovimentacaoRequestDTO.builder()
                .tipo(com.dcriar.domain.stock.entity.enums.TipoMovimentacao.SAIDA_PRODUCAO)
                .quantidade(quantidade.negate())
                .motivo(motivo)
                .build();
        MovimentacaoEstoqueLote saida = MovimentacaoEstoqueLote.from(saidaDTO, lote);
        saida.setOrdemDeProducao(ordem);
        movimentacaoEstoqueLoteRepository.save(saida);
    }

    public void distribuirEstoqueParaCanal(Long produtoId, Long canalVendaId, Integer quantidade) {
        if (canalVendaId != null && quantidade != null && quantidade > 0) {
            AjusteEstoqueRequestDTO ajusteDTO = AjusteEstoqueRequestDTO.builder()
                    .produtoId(produtoId)
                    .canalVendaId(canalVendaId)
                    .quantidade(quantidade)
                    .build();
            estoqueProdutoService.ajustarEstoque(ajusteDTO);
        }
    }

    public void registrarEntradaProduto(Produto produto, int quantidade, String motivo, OrdemDeProducao ordem) {
        MovimentacaoEstoqueProdutoRequestDTO movimentacaoDTO = MovimentacaoEstoqueProdutoRequestDTO.builder()
                .produtoId(produto.getId())
                .tipo(TipoMovimentacaoProduto.ENTRADA_PRODUCAO.name())
                .quantidade(quantidade)
                .motivo(motivo)
                .build();
        MovimentacaoEstoqueProduto entrada = MovimentacaoEstoqueProduto.from(movimentacaoDTO, produto);
        entrada.setOrdemDeProducao(ordem);
        movimentacaoEstoqueProdutoRepository.save(entrada);
    }

    private void validarSaldoLoteCorte(LoteMateriaPrima lote, BigDecimal consumoEmMetros) {
        BigDecimal saldoAtual = lote.getSaldoCalculado() != null ? lote.getSaldoCalculado() : movimentacaoEstoqueLoteRepository.findSaldoByLote(lote);
        if (consumoEmMetros.compareTo(saldoAtual) > 0) {
            throw new SaldoMateriaPrimaInsuficienteException(consumoEmMetros, saldoAtual);
        }
    }

    private Integer resolveRepeticoes(CorteRealizadoResponseDTO dto) {
        return dto.getRepeticoes() != null && dto.getRepeticoes() > 0 ? dto.getRepeticoes() : 1;
    }


    private Produto findProdutoById(Long id) {
        return produtoRepository.findByIdWithTipoMateriaPrima(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    private OrdemDeProducao findOrdemByIdWithDetails(Long id) {
        return ordemDeProducaoRepository.findByIdWithDetails(id)
                .orElseThrow(() -> new OrdemDeProducaoNaoEncontradaException(id));
    }

    private LoteMateriaPrima findLoteById(Long id) {
        return loteMateriaPrimaRepository.findByIdWithTipoMateriaPrima(id)
                .orElseThrow(() -> new LoteMateriaPrimaNaoEncontradoException(id));
    }

    private void prepararOrdemParaReprocessamento(OrdemDeProducao ordem) {
        validarOrdemPodeSerEstornada(ordem);
        List<MovimentacaoEstoqueLote> movimentacoesLote = estornarMovimentacoesDaOrdem(ordem);
        desvincularMovimentacoesDaOrdem(ordem, movimentacoesLote);
        limparRetalhosDaOrdem(ordem);
    }

    private void validarOrdemPodeSerEstornada(OrdemDeProducao ordem) {
        Integer saldoAtualProduto = movimentacaoEstoqueProdutoRepository.findSaldoByProduto(ordem.getProduto());
        if (saldoAtualProduto < ordem.getQuantidadeProduzida()) {
            throw ImpossivelExcluirProducaoException.estoqueInsuficienteParaEstorno(
                    ordem.getQuantidadeProduzida(),
                    saldoAtualProduto
            );
        }

        if (ordem.getCanalVendaDestinoId() != null) {
            int estoqueAtualCanal = consultarSaldoCanal(ordem.getProduto().getId(), ordem.getCanalVendaDestinoId());
            if (estoqueAtualCanal < ordem.getQuantidadeProduzida()) {
                throw ImpossivelExcluirProducaoException.estoqueCanalInsuficienteParaEstorno(
                        ordem.getCanalVendaDestinoId(),
                        ordem.getQuantidadeProduzida(),
                        estoqueAtualCanal
                );
            }
        }

        List<LoteRetalhoHierarchyItem> retalhosGerados = loteRetalhoHierarchyService.listarRetalhosDaOrdemRecursivamente(ordem);
        for (LoteRetalhoHierarchyItem item : retalhosGerados) {
            if (item.possuiAlteracaoAtiva()) {
                LoteMateriaPrima retalho = item.lote();
                throw ImpossivelExcluirProducaoException.retalhoJaUtilizado(construirContextoRetalhoBloqueado(ordem, retalho));
            }
        }
    }

    private List<MovimentacaoEstoqueLote> estornarMovimentacoesDaOrdem(OrdemDeProducao ordem) {
        if (ordem.getCanalVendaDestinoId() != null && ordem.getQuantidadeProduzida() != null && ordem.getQuantidadeProduzida() > 0) {
            AjusteEstoqueRequestDTO ajusteDTO = AjusteEstoqueRequestDTO.builder()
                    .produtoId(ordem.getProduto().getId())
                    .canalVendaId(ordem.getCanalVendaDestinoId())
                    .quantidade(-ordem.getQuantidadeProduzida())
                    .build();
            estoqueProdutoService.ajustarEstoque(ajusteDTO);
        }

        MovimentacaoEstoqueProdutoRequestDTO estornoProdutoDTO = MovimentacaoEstoqueProdutoRequestDTO.builder()
                .produtoId(ordem.getProduto().getId())
                .tipo(TipoMovimentacaoProduto.ESTORNO_PRODUCAO.name())
                .quantidade(-ordem.getQuantidadeProduzida())
                .motivo("Estorno da Ordem de Produção #" + ordem.getId())
                .build();
        MovimentacaoEstoqueProduto estornoProduto = MovimentacaoEstoqueProduto.from(estornoProdutoDTO, ordem.getProduto());
        movimentacaoEstoqueProdutoRepository.save(estornoProduto);

        List<MovimentacaoEstoqueLote> movimentacoesLote = movimentacaoEstoqueLoteRepository.findByOrdemDeProducao(ordem);
        for (MovimentacaoEstoqueLote movimentacao : movimentacoesLote) {
            if (movimentacao.getQuantidade().compareTo(BigDecimal.ZERO) >= 0) continue;

            MovimentacaoRequestDTO estornoMPDTO = MovimentacaoRequestDTO.builder()
                    .tipo(TipoMovimentacao.ESTORNO_PRODUCAO)
                    .quantidade(movimentacao.getQuantidade().abs())
                    .motivo("Estorno da Ordem de Produção #" + ordem.getId())
                    .build();
            MovimentacaoEstoqueLote estornoMP = MovimentacaoEstoqueLote.from(estornoMPDTO, movimentacao.getLote());
            movimentacaoEstoqueLoteRepository.save(estornoMP);
        }

        return movimentacoesLote;
    }

    private void desvincularMovimentacoesDaOrdem(OrdemDeProducao ordem, List<MovimentacaoEstoqueLote> movimentacoesLote) {
        List<MovimentacaoEstoqueProduto> movimentosProduto = movimentacaoEstoqueProdutoRepository.findByOrdemDeProducao(ordem);
        movimentosProduto.forEach(mov -> mov.setOrdemDeProducao(null));
        movimentacaoEstoqueProdutoRepository.saveAll(movimentosProduto);

        movimentacoesLote.forEach(mov -> mov.setOrdemDeProducao(null));
        movimentacaoEstoqueLoteRepository.saveAll(movimentacoesLote);
    }

    private void limparRetalhosDaOrdem(OrdemDeProducao ordem) {
        List<LoteMateriaPrima> retalhosGerados = loteRetalhoHierarchyService.listarRetalhosDaOrdemRecursivamente(ordem).stream()
                .sorted(Comparator.comparingInt(LoteRetalhoHierarchyItem::nivel).reversed())
                .map(LoteRetalhoHierarchyItem::lote)
                .toList();
        loteMateriaPrimaRepository.deleteAll(retalhosGerados);
    }

    private int consultarSaldoCanal(Long produtoId, Long canalVendaId) {
        try {
            EstoqueResponseDTO estoque = estoqueProdutoService.consultarEstoque(produtoId, canalVendaId);
            return estoque.getQuantidade();
        } catch (EstoqueNaoEncontradoException ex) {
            return 0;
        }
    }

    private ImpossivelExcluirProducaoException.ContextoRetalhoBloqueado construirContextoRetalhoBloqueado(
            OrdemDeProducao ordem,
            LoteMateriaPrima retalho
    ) {
        return new ImpossivelExcluirProducaoException.ContextoRetalhoBloqueado(
                ordem.getId(),
                retalho.getId(),
                retalho.getOrdemDeProducaoOrigem() != null ? retalho.getOrdemDeProducaoOrigem().getId() : ordem.getId(),
                construirCadeiaRetalhos(retalho),
                listarOrdensRelacionadasIds(retalho),
                obterTipoAlteracaoAtiva(retalho),
                obterOrdemConsumidoraAtivaId(retalho)
        );
    }

    private List<ImpossivelExcluirProducaoException.CadeiaRetalhoItem> construirCadeiaRetalhos(LoteMateriaPrima retalho) {
        return listarCadeiaAteRaiz(retalho).stream()
                .map(lote -> new ImpossivelExcluirProducaoException.CadeiaRetalhoItem(
                        lote.getId(),
                        lote.getOrdemDeProducaoOrigem() != null ? lote.getOrdemDeProducaoOrigem().getId() : null
                ))
                .toList();
    }

    private List<Long> listarOrdensRelacionadasIds(LoteMateriaPrima retalho) {
        Set<Long> ordensRelacionadas = new LinkedHashSet<>();
        List<LoteMateriaPrima> cadeia = listarCadeiaAteRaiz(retalho);

        for (LoteMateriaPrima loteDaCadeia : cadeia) {
            if (loteDaCadeia.getOrdemDeProducaoOrigem() != null) {
                ordensRelacionadas.add(loteDaCadeia.getOrdemDeProducaoOrigem().getId());
            }
        }

        retalho.getMovimentacoes().stream()
                .filter(movimentacao -> movimentacao.getOrdemDeProducao() != null)
                .map(movimentacao -> movimentacao.getOrdemDeProducao().getId())
                .forEach(ordensRelacionadas::add);

        return new ArrayList<>(ordensRelacionadas);
    }

    private List<LoteMateriaPrima> listarCadeiaAteRaiz(LoteMateriaPrima retalho) {
        LinkedList<LoteMateriaPrima> cadeia = new LinkedList<>();
        LoteMateriaPrima atual = retalho;

        while (atual != null) {
            cadeia.addFirst(atual);
            atual = atual.getLoteDeOrigem();
        }

        return cadeia;
    }

    private TipoMovimentacao obterTipoAlteracaoAtiva(LoteMateriaPrima retalho) {
        return retalho.getMovimentacoes().stream()
                .filter(movimentacao -> switch (movimentacao.getTipo()) {
                    case SAIDA_PRODUCAO, PERDA_DESCARTE, AJUSTE_INVENTARIO -> true;
                    default -> false;
                })
                .max(Comparator.comparing(MovimentacaoEstoqueLote::getData))
                .map(MovimentacaoEstoqueLote::getTipo)
                .orElse(null);
    }

    private Long obterOrdemConsumidoraAtivaId(LoteMateriaPrima retalho) {
        return retalho.getMovimentacoes().stream()
                .filter(movimentacao -> movimentacao.getTipo() == TipoMovimentacao.SAIDA_PRODUCAO)
                .max(Comparator.comparing(MovimentacaoEstoqueLote::getData))
                .map(MovimentacaoEstoqueLote::getOrdemDeProducao)
                .map(OrdemDeProducao::getId)
                .orElse(null);
    }

    private void validarCompatibilidadeMaterialEntreProdutoELote(Produto produto, LoteMateriaPrima lote) {
        if (!produto.getTipoMateriaPrima().equals(lote.getTipoMateriaPrima())) {
            throw IncompatibilidadeMaterialException.entreProdutoELote(
                    produto.getTipoMateriaPrima().getNome(),
                    lote.getTipoMateriaPrima().getNome()
            );
        }
    }

    private void carregarSaldoAtualNoLote(LoteMateriaPrima lote) {
        lote.setSaldoCalculado(movimentacaoEstoqueLoteRepository.findSaldoByLote(lote));
    }

    private void carregarSaldoDisponivelParaEdicaoNoLote(LoteMateriaPrima lote, Long ordemId) {
        lote.setSaldoCalculado(calcularSaldoDisponivelParaEdicao(lote, ordemId));
    }

    private OrdemDeProducaoResponseDTO toDetailedDto(OrdemDeProducao ordem) {
        OrdemDeProducaoResponseDTO dto = ordemDeProducaoMapper.toDto(ordem);
        if (ordem.getModoCalculo() != ModoCalculo.AUTOMATICO) {
            dto.setMargens(null);
        }
        preencherBlocoProdutos(ordem, dto);
        preencherSimulacaoInicial(ordem, dto);
        return dto;
    }

    private void preencherSimulacaoInicial(OrdemDeProducao ordem, OrdemDeProducaoResponseDTO dto) {
        if (ordem.getProduto() instanceof ProdutoDeCorte produtoDeCorte) {
            dto.setSimulacaoInicialCorte(construirSimulacaoInicialCorte(ordem, dto, produtoDeCorte));
            dto.setSimulacaoInicialConsumo(null);
            return;
        }

        dto.setSimulacaoInicialCorte(null);
        dto.setSimulacaoInicialConsumo(construirSimulacaoInicialConsumo(ordem));
    }

    private SimulacaoCorteResponseDTO construirSimulacaoInicialCorte(
            OrdemDeProducao ordem,
            OrdemDeProducaoResponseDTO dto,
            ProdutoDeCorte produtoDeCorte
    ) {
        List<CorteRealizado> cortes = Optional.ofNullable(ordem.getCortesRealizados()).orElse(List.of());
        List<CorteRealizado> cortesProduto = cortes.stream()
                .filter(corte -> "PRODUTO".equalsIgnoreCase(corte.getTipo()))
                .toList();

        CorteRealizado lateralRetalho = cortes.stream()
                .filter(corte -> "RETALHO".equalsIgnoreCase(corte.getTipo()))
                .filter(corte -> corte.getRetalhoCategoria() != null && "LATERAL".equalsIgnoreCase(corte.getRetalhoCategoria()))
                .findFirst()
                .orElse(null);

        CorteRealizado inferiorRetalho = cortes.stream()
                .filter(corte -> "RETALHO".equalsIgnoreCase(corte.getTipo()))
                .filter(corte -> corte.getRetalhoCategoria() != null && "FINAL".equalsIgnoreCase(corte.getRetalhoCategoria()))
                .findFirst()
                .orElse(null);

        BigDecimal larguraPeca = ordem.isRotacionado()
                ? produtoDeCorte.getDimensoes().getComprimentoCm()
                : produtoDeCorte.getDimensoes().getLarguraCm();
        BigDecimal comprimentoPeca = ordem.isRotacionado()
                ? produtoDeCorte.getDimensoes().getLarguraCm()
                : produtoDeCorte.getDimensoes().getComprimentoCm();

        int produtosPorLinha = cortesProduto.stream()
                .map(CorteRealizado::getQuantidade)
                .max(Integer::compareTo)
                .orElse(0);

        int totalLinhas = cortesProduto.size();

        int produtosNaUltimaLinha = totalLinhas > 0 && produtosPorLinha > 0
                ? ordem.getQuantidadeProduzida() - (Math.max(totalLinhas - 1, 0) * produtosPorLinha)
                : ordem.getQuantidadeProduzida();

        int linhasCompletas = totalLinhas > 0 && produtosNaUltimaLinha > 0 && produtosNaUltimaLinha < produtosPorLinha
                ? totalLinhas - 1
                : totalLinhas;

        BigDecimal consumoEstimado = BigDecimal.ZERO;
        LoteMateriaPrima lotePrincipal = ordem.getLotesConsumidos().stream().findFirst().orElse(null);
        if (lotePrincipal != null && ordem.getLarguraFinalCm() != null && ordem.getComprimentoFinalCm() != null) {
            consumoEstimado = calcularConsumoCorteNaUnidadeDoLote(
                    lotePrincipal,
                    ordem.getLarguraFinalCm(),
                    ordem.getComprimentoFinalCm()
            );
        }

        return SimulacaoCorteResponseDTO.builder()
                .modoCalculo(ordem.getModoCalculo())
                .larguraFinalCm(ordem.getLarguraFinalCm())
                .comprimentoFinalCm(ordem.getComprimentoFinalCm())
                .consumoEstimado(consumoEstimado)
                .rotacionado(ordem.isRotacionado())
                .produtosPorLinha(produtosPorLinha)
                .numeroLinhasCompletas(Math.max(linhasCompletas, 0))
                .produtosNaUltimaLinha(totalLinhas > 0 && produtosNaUltimaLinha < produtosPorLinha ? Math.max(produtosNaUltimaLinha, 0) : 0)
                .sobraLateral(formatarSobra(lateralRetalho))
                .sobraInferior(formatarSobra(inferiorRetalho))
                .saldoRolo("")
                .dimensaoProduto(formatarDimensao(larguraPeca, comprimentoPeca))
                .consumoTotal(formatarDimensao(ordem.getLarguraFinalCm(), ordem.getComprimentoFinalCm()))
                .larguraBlocoProdutosCm(dto.getLarguraBlocoProdutosCm())
                .comprimentoBlocoProdutosCm(dto.getComprimentoBlocoProdutosCm())
                .build();
    }

    private SimulacaoConsumoResponseDTO construirSimulacaoInicialConsumo(OrdemDeProducao ordem) {
        UnidadeDeMedida unidadeExibicao = ordem.getProduto().getTipoMateriaPrima().getUnidadeDeConsumo();
        BigDecimal consumoTotalInterno = ordem.getProduto().getUnidadesPorProduto()
                .multiply(BigDecimal.valueOf(ordem.getQuantidadeProduzida()));

        List<LoteMateriaPrima> lotesConsumidos = ordem.getLotesConsumidos().stream().toList();
        List<com.dcriar.api.dto.response.production.PlanoDeConsumoItemDTO> planoDeConsumo = new ArrayList<>();
        Map<Long, BigDecimal> saldoRestante = new LinkedHashMap<>();

        for (int index = 0; index < lotesConsumidos.size(); index++) {
            LoteMateriaPrima lote = lotesConsumidos.get(index);
            BigDecimal quantidadeItem = index == 0 ? consumoTotalInterno : BigDecimal.ZERO;
            planoDeConsumo.add(com.dcriar.api.dto.response.production.PlanoDeConsumoItemDTO.builder()
                    .loteId(lote.getId())
                    .motivoLote(lote.getMotivo())
                    .quantidadeAConsumir(converterConsumoParaUnidadeExibicao(quantidadeItem, unidadeExibicao))
                    .build());
            saldoRestante.put(
                    lote.getId(),
                    converterConsumoParaUnidadeExibicao(movimentacaoEstoqueLoteRepository.findSaldoByLote(lote), unidadeExibicao)
            );
        }

        return SimulacaoConsumoResponseDTO.builder()
                .consumoTotalEstimado(converterConsumoParaUnidadeExibicao(consumoTotalInterno, unidadeExibicao))
                .unidadeDeConsumo(unidadeExibicao)
                .unidadeDescricao(unidadeExibicao.getDescricao())
                .unidadeDescricaoPlural(unidadeExibicao.getDescricaoPlural())
                .unidadeSimbolo(unidadeExibicao.getSimbolo())
                .exibirQuantidadeComSimbolo(unidadeExibicao.isExibirQuantidadeComSimbolo())
                .planoDeConsumo(planoDeConsumo)
                .saldoRestante(saldoRestante)
                .build();
    }

    private void preencherBlocoProdutos(OrdemDeProducao ordem, OrdemDeProducaoResponseDTO dto) {
        if (!(ordem.getProduto() instanceof ProdutoDeCorte produtoDeCorte) || ordem.getLarguraFinalCm() == null || ordem.getComprimentoFinalCm() == null) {
            return;
        }

        CorteRealizado lateralRetalho = ordem.getCortesRealizados().stream()
                .filter(corte -> "RETALHO".equalsIgnoreCase(corte.getTipo()))
                .filter(corte -> corte.getRetalhoCategoria() != null && "LATERAL".equalsIgnoreCase(corte.getRetalhoCategoria()))
                .findFirst()
                .orElse(null);

        CorteRealizado inferiorRetalho = ordem.getCortesRealizados().stream()
                .filter(corte -> "RETALHO".equalsIgnoreCase(corte.getTipo()))
                .filter(corte -> corte.getRetalhoCategoria() != null && "FINAL".equalsIgnoreCase(corte.getRetalhoCategoria()))
                .findFirst()
                .orElse(null);

        BigDecimal larguraBloco = ordem.getLarguraFinalCm();
        BigDecimal comprimentoBloco = ordem.getComprimentoFinalCm();

        if (lateralRetalho != null && lateralRetalho.getLarguraCm() != null) {
            larguraBloco = larguraBloco.subtract(lateralRetalho.getLarguraCm());
        }
        if (inferiorRetalho != null && inferiorRetalho.getComprimentoCm() != null) {
            comprimentoBloco = comprimentoBloco.subtract(inferiorRetalho.getComprimentoCm());
        }

        if (ordem.getModoCalculo() == ModoCalculo.AUTOMATICO && ordem.getMargens() != null) {
            larguraBloco = larguraBloco
                    .subtract(valorOuZero(ordem.getMargens().getEsquerda()))
                    .subtract(valorOuZero(ordem.getMargens().getDireita()));
            comprimentoBloco = comprimentoBloco
                    .subtract(valorOuZero(ordem.getMargens().getSuperior()))
                    .subtract(valorOuZero(ordem.getMargens().getInferior()));
        }

        if (ordem.getModoCalculo() == ModoCalculo.MANUAL && produtoDeCorte.getDimensoes() != null) {
            BigDecimal larguraProduto = ordem.isRotacionado()
                    ? produtoDeCorte.getDimensoes().getComprimentoCm()
                    : produtoDeCorte.getDimensoes().getLarguraCm();
            BigDecimal comprimentoProduto = ordem.isRotacionado()
                    ? produtoDeCorte.getDimensoes().getLarguraCm()
                    : produtoDeCorte.getDimensoes().getComprimentoCm();

            if (larguraBloco.compareTo(BigDecimal.ZERO) <= 0) {
                larguraBloco = larguraProduto;
            }
            if (comprimentoBloco.compareTo(BigDecimal.ZERO) <= 0) {
                comprimentoBloco = comprimentoProduto;
            }
        }

        dto.setLarguraBlocoProdutosCm(larguraBloco.max(BigDecimal.ZERO));
        dto.setComprimentoBlocoProdutosCm(comprimentoBloco.max(BigDecimal.ZERO));
    }

    private BigDecimal valorOuZero(BigDecimal valor) {
        return valor != null ? valor : BigDecimal.ZERO;
    }

    private String formatarSobra(CorteRealizado corte) {
        if (corte == null || corte.getLarguraCm() == null || corte.getComprimentoCm() == null) {
            return "";
        }
        return formatarDimensao(corte.getLarguraCm(), corte.getComprimentoCm());
    }

    private BigDecimal calcularSaldoDisponivelParaEdicao(LoteMateriaPrima lote, Long ordemId) {
        BigDecimal saldoAtual = movimentacaoEstoqueLoteRepository.findSaldoByLote(lote);
        if (ordemId == null) {
            return saldoAtual;
        }

        OrdemDeProducao ordem = findOrdemByIdWithDetails(ordemId);
        BigDecimal consumoOriginalDaOrdem = movimentacaoEstoqueLoteRepository.findByOrdemDeProducao(ordem).stream()
                .filter(mov -> mov.getLote() != null && mov.getLote().getId().equals(lote.getId()))
                .filter(mov -> mov.getQuantidade().compareTo(BigDecimal.ZERO) < 0)
                .map(mov -> mov.getQuantidade().abs())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return saldoAtual.add(consumoOriginalDaOrdem);
    }

    private BigDecimal calcularConsumoCorteNaUnidadeDoLote(
            LoteMateriaPrima lote,
            BigDecimal larguraUtilizadaCm,
            BigDecimal comprimentoConsumidoCm
    ) {
        return switch (lote.getUnidadeDeEstoque()) {
            case METRO_LINEAR -> comprimentoConsumidoCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            case CENTIMETRO_LINEAR -> comprimentoConsumidoCm.setScale(4, RoundingMode.HALF_UP);
            case METRO_QUADRADO -> larguraUtilizadaCm.multiply(comprimentoConsumidoCm)
                    .divide(new BigDecimal("10000"), 4, RoundingMode.HALF_UP);
            case CENTIMETRO_QUADRADO -> larguraUtilizadaCm.multiply(comprimentoConsumidoCm)
                    .setScale(4, RoundingMode.HALF_UP);
            default -> throw UnidadeEstoqueCorteInvalidaException.unidadeNaoSuportada(lote.getUnidadeDeEstoque());
        };
    }

    private BigDecimal calcularQuantidadeRetalhoNaUnidadeDoLote(
            LoteMateriaPrima lotePrincipal,
            BigDecimal larguraSobraCm,
            BigDecimal comprimentoRetalhoCm
    ) {
        return switch (lotePrincipal.getUnidadeDeEstoque()) {
            case METRO_LINEAR -> comprimentoRetalhoCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            case CENTIMETRO_LINEAR -> comprimentoRetalhoCm.setScale(4, RoundingMode.HALF_UP);
            case METRO_QUADRADO -> larguraSobraCm.multiply(comprimentoRetalhoCm)
                    .divide(new BigDecimal("10000"), 4, RoundingMode.HALF_UP);
            case CENTIMETRO_QUADRADO -> larguraSobraCm.multiply(comprimentoRetalhoCm)
                    .setScale(4, RoundingMode.HALF_UP);
            default -> throw UnidadeEstoqueCorteInvalidaException.unidadeNaoSuportada(lotePrincipal.getUnidadeDeEstoque());
        };
    }

    private String formatarDimensao(BigDecimal largura, BigDecimal comprimento) {
        return String.format("%scm x %scm",
                largura.stripTrailingZeros().toPlainString(),
                comprimento.stripTrailingZeros().toPlainString());
    }
}
