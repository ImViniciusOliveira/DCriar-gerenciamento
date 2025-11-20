package com.dcriar.domain.production.service.impl;

import com.dcriar.api.dto.request.product.AjusteEstoqueRequestDTO;
import com.dcriar.api.dto.request.product.MovimentacaoEstoqueProdutoRequestDTO;
import com.dcriar.api.dto.request.production.*;
import com.dcriar.api.dto.request.stock.MovimentacaoRequestDTO;
import com.dcriar.api.dto.response.production.CorteRealizadoResponseDTO;
import com.dcriar.api.dto.response.production.OrdemDeProducaoResponseDTO;
import com.dcriar.api.dto.response.production.SimulacaoConsumoDiretoResponseDTO;
import com.dcriar.api.dto.response.production.SimulacaoCorteResponseDTO;
import com.dcriar.api.mapper.production.OrdemDeProducaoMapper;
import com.dcriar.domain.product.entity.MovimentacaoEstoqueProduto;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.entity.enums.TipoMovimentacaoProduto;
import com.dcriar.domain.product.repository.MovimentacaoEstoqueProdutoRepository;
import com.dcriar.domain.product.repository.ProdutoRepository;
import com.dcriar.domain.product.service.EstoqueProdutoService;
import com.dcriar.domain.production.entity.CorteRealizado;
import com.dcriar.domain.production.entity.Margens;
import com.dcriar.domain.production.entity.OrdemDeProducao;
import com.dcriar.domain.production.enums.ModoCalculo;
import com.dcriar.domain.production.model.ParametrosCorte;
import com.dcriar.domain.production.repository.OrdemDeProducaoRepository;
import com.dcriar.domain.production.service.CorteCalculatorService;
import com.dcriar.domain.production.service.OrdemDeProducaoService;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.entity.MovimentacaoEstoqueLote;
import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.dcriar.domain.stock.repository.LoteMateriaPrimaRepository;
import com.dcriar.domain.stock.repository.MovimentacaoEstoqueLoteRepository;
import com.dcriar.exception.custom.*;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Implementação do serviço para gerir Ordens de Produção.
 * <p>
 * Esta classe orquestra a criação de ordens por corte (com otimização de layout)
 * e por consumo direto, gerenciando a movimentação de estoque de matéria-prima e produtos acabados.
 * <p>
 * <b>Responsabilidades Principais:</b>
 * <ul>
 * <li>Criar ordens de produção, validando a compatibilidade do tipo de produção.</li>
 * <li>Orquestrar a baixa no estoque de matéria-prima.</li>
 * <li>Orquestrar a entrada no estoque de produtos acabados.</li>
 * <li>Gerar novos lotes de matéria-prima a partir de sobras (retalhos) em ordens de corte.</li>
 * <li>Distribuir opcionalmente o estoque produzido para canais de venda.</li>
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

    @Override
    @Transactional
    public OrdemDeProducaoResponseDTO criarOrdemDeCorte(OrdemDeCorteRequestDTO requestDTO) {

        // 1. Validações iniciais e busca de entidades principais.
        Produto produto = findProdutoById(requestDTO.getProdutoId());
        if (isGeometricUnit(produto.getTipoMateriaPrima().getUnidadeDeConsumo())) {
            throw new TipoProducaoIncompativelException("Este produto não pode ser produzido por corte. Utilize o endpoint de consumo direto.");
        }

        if (requestDTO.getLotePrincipalId() == null) {
            throw new LotePrincipalNaoEspecificadoException("A produção por corte exige a especificação de um 'lotePrincipalId'.");
        }
        LoteMateriaPrima lotePrincipal = findLoteById(requestDTO.getLotePrincipalId());

        BigDecimal consumoTotalMetros;
        BigDecimal comprimentoFinalCm;
        List<CorteRealizadoResponseDTO> cortesRealizadosDTOs;
        BigDecimal larguraFinalCm;
        ParametrosCorte parametros = null;

        // 2. Determina os parâmetros de corte (manual ou automático).
        if (requestDTO.getModoCalculo() == ModoCalculo.MANUAL) {
            larguraFinalCm = requestDTO.getLarguraFinalCm();
            comprimentoFinalCm = requestDTO.getComprimentoFinalCm();
            consumoTotalMetros = comprimentoFinalCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

            cortesRealizadosDTOs = gerarCortesManuais(requestDTO, produto, lotePrincipal);

        } else {
            // LÓGICA DE MARGEM CORRIGIDA
            // As margens agora definem a área útil de corte desde o início.
            parametros = corteCalculatorService.extrairParametrosCorte(
                    requestDTO.getQuantidadeProduzida(), produto, lotePrincipal, requestDTO.getMargens()
            );

            // Calcula o comprimento base necessário apenas para os produtos.
            long numeroDeLinhas = (long) Math.ceil((double) requestDTO.getQuantidadeProduzida() / parametros.produtosPorLinha());

            // O comprimento final a ser consumido é o dos produtos mais as margens superior/inferior.
            comprimentoFinalCm = parametros.comprimentoProduto().multiply(new BigDecimal(numeroDeLinhas));
            if (requestDTO.getMargens() != null) {
                comprimentoFinalCm = comprimentoFinalCm
                        .add(Optional.ofNullable(requestDTO.getMargens().getSuperior()).orElse(BigDecimal.ZERO))
                        .add(Optional.ofNullable(requestDTO.getMargens().getInferior()).orElse(BigDecimal.ZERO));
            }
            consumoTotalMetros = comprimentoFinalCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

            // A largura final reportada é a largura física total do lote, já que as margens estão contidas nela.
            larguraFinalCm = parametros.larguraTotalLoteCm();

            // Gera os cortes e retalhos com base nas dimensões físicas reais e no comprimento total consumido.
            cortesRealizadosDTOs = gerarCortesRealizadosDinamico(parametros, lotePrincipal, comprimentoFinalCm);
        }


        // 3. Valida se o lote principal tem saldo suficiente.
        validarSaldoLoteCorte(lotePrincipal, consumoTotalMetros);

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
                .rotacionado(parametros != null && parametros.rotacionado())
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
                            .ordemDeProducaoId(ordem.getId())
                            .build(),
                    ordem
            ));
        }

        OrdemDeProducao savedOrdem = ordemDeProducaoRepository.save(ordem);

        // 5. Orquestra as movimentações de estoque.
        registrarSaidaLote(lotePrincipal, consumoTotalMetros, "Consumido pela Ordem de Produção #" + savedOrdem.getId());
        registrarEntradaProduto(produto, requestDTO.getQuantidadeProduzida(), "Produzido via Ordem de Produção #" + savedOrdem.getId());
        distribuirEstoqueParaCanal(savedOrdem.getProduto().getId(), requestDTO.getCanalVendaDestinoId(), requestDTO.getQuantidadeProduzida());

        return ordemDeProducaoMapper.toDto(savedOrdem);
    }

    @Override
    @Transactional
    public OrdemDeProducaoResponseDTO criarOrdemDeConsumoDireto(OrdemDeConsumoDiretoRequestDTO requestDTO) {
        // 1. Validações iniciais e busca de entidades.
        Produto produto = findProdutoById(requestDTO.getProdutoId());
        if (isDirectConsumptionUnit(produto.getTipoMateriaPrima().getUnidadeDeConsumo())) {
            throw new TipoProducaoIncompativelException("Este produto não pode ser produzido por consumo direto. Utilize o endpoint de corte.");
        }

        Set<LoteMateriaPrima> lotesConsumidos = new HashSet<>(loteMateriaPrimaRepository.findAllById(requestDTO.getLotesConsumidosIds()));
        if (lotesConsumidos.size() != requestDTO.getLotesConsumidosIds().size()) {
            Set<Long> foundIds = lotesConsumidos.stream().map(LoteMateriaPrima::getId).collect(Collectors.toSet());
            Set<Long> missingIds = new HashSet<>(requestDTO.getLotesConsumidosIds());
            missingIds.removeAll(foundIds);
            throw new LotesMateriaPrimaNaoEncontradosException(missingIds);
        }

        // 2. Valida se o saldo total dos lotes é suficiente.
        BigDecimal consumoTotalNecessario = new BigDecimal(produto.getUnidadesPorProduto() * requestDTO.getQuantidadeProduzida());
        BigDecimal saldoTotalDisponivel = lotesConsumidos.stream()
                .map(movimentacaoEstoqueLoteRepository::findSaldoByLote)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (saldoTotalDisponivel.compareTo(consumoTotalNecessario) < 0) {
            throw new SaldoMateriaPrimaInsuficienteException(consumoTotalNecessario, saldoTotalDisponivel);
        }

        // 3. Cria e persiste a Ordem de Produção.
        OrdemDeProducaoRequestDTO ordemRequestDTO = OrdemDeProducaoRequestDTO.builder()
                .produtoId(produto.getId())
                .lotesConsumidosIds(new HashSet<>(requestDTO.getLotesConsumidosIds()))
                .canalVendaDestinoId(requestDTO.getCanalVendaDestinoId() != null ? requestDTO.getCanalVendaDestinoId() : null)
                .quantidadeProduzida(requestDTO.getQuantidadeProduzida())
                .modoCalculo(ModoCalculo.MANUAL.name())
                .motivo(requestDTO.getMotivo())
                .build();

        OrdemDeProducao ordem = OrdemDeProducao.from(ordemRequestDTO, produto, lotesConsumidos, null);
        OrdemDeProducao savedOrdem = ordemDeProducaoRepository.save(ordem);

        // 4. Orquestra as movimentações de estoque.
        // Consome dos lotes sequencialmente até atingir o necessário.
        BigDecimal consumoRestante = consumoTotalNecessario;
        for (LoteMateriaPrima lote : lotesConsumidos) {
            if (consumoRestante.compareTo(BigDecimal.ZERO) <= 0) break;
            BigDecimal saldoDoLote = movimentacaoEstoqueLoteRepository.findSaldoByLote(lote);
            BigDecimal consumoNesteLote = saldoDoLote.min(consumoRestante);
            if (consumoNesteLote.compareTo(BigDecimal.ZERO) > 0) {
                registrarSaidaLote(lote, consumoNesteLote, "Consumido pela Ordem de Produção #" + savedOrdem.getId());
                consumoRestante = consumoRestante.subtract(consumoNesteLote);
            }
        }

        registrarEntradaProduto(produto, requestDTO.getQuantidadeProduzida(), "Produzido via Ordem de Produção #" + savedOrdem.getId());
        distribuirEstoqueParaCanal(savedOrdem.getProduto().getId(), requestDTO.getCanalVendaDestinoId(), requestDTO.getQuantidadeProduzida());

        return ordemDeProducaoMapper.toDto(savedOrdem);
    }

    @Override
    @Transactional
    public void excluir(Long id) {
        if (!ordemDeProducaoRepository.existsById(id)) {
            throw new OrdemDeProducaoNaoEncontradaException(id);
        }
        ordemDeProducaoRepository.deleteById(id);
    }

    @Override
    public OrdemDeProducaoResponseDTO buscarPorId(Long id) {
        return ordemDeProducaoRepository.findById(id)
                .map(ordemDeProducaoMapper::toDto)
                .orElseThrow(() -> new OrdemDeProducaoNaoEncontradaException(id));
    }

    @Override
    public List<OrdemDeProducaoResponseDTO> listarTodas() {
        return ordemDeProducaoRepository.findAll().stream()
                .map(ordemDeProducaoMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public SimulacaoCorteResponseDTO simularCorte(SimulacaoCorteRequestDTO requestDTO) {
        Produto produto = findProdutoById(requestDTO.getProdutoId());
        if (isGeometricUnit(produto.getTipoMateriaPrima().getUnidadeDeConsumo())) {
            throw new TipoProducaoIncompativelException("Este produto não utiliza uma matéria-prima geométrica para simulação de corte.");
        }

        // Busca o primeiro lote compatível com estoque para usar como base para a simulação.
        LoteMateriaPrima loteParaSimulacao = loteMateriaPrimaRepository.findAllByTipoMateriaPrima(produto.getTipoMateriaPrima()).stream()
                .filter(lote -> movimentacaoEstoqueLoteRepository.findSaldoByLote(lote).compareTo(BigDecimal.ZERO) > 0)
                .findFirst()
                .orElseThrow(() -> new NenhumLoteComEstoqueException("Não há lotes de matéria-prima com estoque disponível para este produto."));


        ParametrosCorte parametros = corteCalculatorService.extrairParametrosCorte(requestDTO.getQuantidade(), produto, loteParaSimulacao, null);
        long numeroDeLinhas = (long) Math.ceil((double) requestDTO.getQuantidade() / parametros.produtosPorLinha());
        BigDecimal comprimentoFinalCm = parametros.comprimentoProduto().multiply(new BigDecimal(numeroDeLinhas));
        BigDecimal consumoEstimado = comprimentoFinalCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        return SimulacaoCorteResponseDTO.builder()
                .modoCalculo(ModoCalculo.AUTOMATICO)
                .larguraFinalCm(parametros.larguraTotalLoteCm())
                .comprimentoFinalCm(comprimentoFinalCm)
                .consumoEstimado(consumoEstimado)
                .build();
    }

    @Override
    public SimulacaoConsumoDiretoResponseDTO simularConsumoDireto(SimulacaoConsumoDiretoRequestDTO requestDTO) {
        Produto produto = findProdutoById(requestDTO.getProdutoId());
        if (isDirectConsumptionUnit(produto.getTipoMateriaPrima().getUnidadeDeConsumo())) {
            throw new TipoProducaoIncompativelException("Este produto utiliza uma matéria-prima geométrica. Utilize o simulador de corte.");
        }

        BigDecimal consumoTotalEstimado = new BigDecimal(produto.getUnidadesPorProduto() * requestDTO.getQuantidade());
        return SimulacaoConsumoDiretoResponseDTO.builder()
                .consumoTotalEstimado(consumoTotalEstimado)
                .unidadeDeConsumo(produto.getTipoMateriaPrima().getUnidadeDeConsumo())
                .build();
    }

    private List<CorteRealizadoResponseDTO> gerarCortesManuais(OrdemDeCorteRequestDTO requestDTO, Produto produto, LoteMateriaPrima lotePrincipal) {
        BigDecimal larguraFinalCm = requestDTO.getLarguraFinalCm();
        BigDecimal comprimentoFinalCm = requestDTO.getComprimentoFinalCm();
        BigDecimal larguraProduto = produto.getDimensoes().getLarguraCm();
        BigDecimal comprimentoProduto = produto.getDimensoes().getComprimentoCm();
        int quantidadeProduzida = requestDTO.getQuantidadeProduzida();

        List<CorteRealizadoResponseDTO> cortes = new ArrayList<>();

        int produtosPorLinha = larguraFinalCm.divide(larguraProduto, 0, RoundingMode.DOWN).intValue();
        if (produtosPorLinha == 0) {
            throw new DimensoesManuaisInvalidasException(String.format("A largura final (%.2f cm) é menor que a largura do produto (%.2f cm).", larguraFinalCm, larguraProduto));
        }

        int produtosRestantes = quantidadeProduzida;
        BigDecimal comprimentoAcumulado = BigDecimal.ZERO;

        // Objeto para manter o estado da sequência de retalhos laterais
        RetalhoLateralTracker tracker = new RetalhoLateralTracker();

        // Percorre linha por linha, montando cortes de produto e detectando retalhos laterais
        while (produtosRestantes > 0) {
            int produtosNestaLinha = Math.min(produtosPorLinha, produtosRestantes);

            if (comprimentoAcumulado.add(comprimentoProduto).compareTo(comprimentoFinalCm) > 0) {
                throw new DimensoesManuaisInvalidasException(String.format("O comprimento final (%.2f cm) não é suficiente para produzir a quantidade solicitada, que exigiria um comprimento de pelo menos %.2f cm.", comprimentoFinalCm, comprimentoAcumulado.add(comprimentoProduto)));
            }

            cortes.add(criarCorteProduto(larguraProduto, comprimentoProduto, produtosNestaLinha));

            BigDecimal larguraOcupada = larguraProduto.multiply(new BigDecimal(produtosNestaLinha));
            BigDecimal larguraRetalhoAtual = larguraFinalCm.subtract(larguraOcupada);

            processarRetalhoLateral(tracker, larguraRetalhoAtual, comprimentoProduto, lotePrincipal, cortes);

            comprimentoAcumulado = comprimentoAcumulado.add(comprimentoProduto);
            produtosRestantes -= produtosNestaLinha;
        }

        // Fecha qualquer sequência de retalho lateral pendente
        fecharSequenciaDeRetalhoLateral(tracker, lotePrincipal, cortes);


        // Retalho final (sobra de comprimento)
        BigDecimal comprimentoRetalhoFinal = comprimentoFinalCm.subtract(comprimentoAcumulado);
        if (comprimentoRetalhoFinal.compareTo(BigDecimal.ZERO) > 0) {
            cortes.add(criarCorteRetalho(larguraFinalCm, comprimentoRetalhoFinal, "FINAL"));
            criarLoteDeRetalho(lotePrincipal, larguraFinalCm, comprimentoRetalhoFinal.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        }

        return cortes;
    }


    private List<CorteRealizadoResponseDTO> gerarCortesRealizadosDinamico(ParametrosCorte parametros, LoteMateriaPrima lotePrincipal, BigDecimal ordemComprimentoFinalCm) {
        List<CorteRealizadoResponseDTO> cortesRealizados = new ArrayList<>();
        int produtosRestantes = parametros.quantidade();

        RetalhoLateralTracker tracker = new RetalhoLateralTracker();
        BigDecimal comprimentoAcumuladoProdutos = BigDecimal.ZERO; // Acompanha o comprimento consumido pelos produtos

        while (produtosRestantes > 0) {
            int produtosNestaLinha = Math.min(parametros.produtosPorLinha(), produtosRestantes);
            if (produtosNestaLinha <= 0) break;

            cortesRealizados.add(criarCorteProduto(parametros.larguraProduto(), parametros.comprimentoProduto(), produtosNestaLinha));

            // O retalho lateral é calculado sobre a LARGURA ÚTIL, não a largura total do lote.
            BigDecimal larguraProdutosOcupada = parametros.larguraProduto().multiply(new BigDecimal(produtosNestaLinha));
            BigDecimal larguraRetalhoLinha = parametros.larguraUtilCm().subtract(larguraProdutosOcupada);
            BigDecimal comprimentoLinha = parametros.comprimentoProduto();

            processarRetalhoLateral(tracker, larguraRetalhoLinha, comprimentoLinha, lotePrincipal, cortesRealizados);

            comprimentoAcumuladoProdutos = comprimentoAcumuladoProdutos.add(comprimentoLinha); // Acumula o comprimento do produto
            produtosRestantes -= produtosNestaLinha;
        }

        // Fecha qualquer sequência lateral pendente
        fecharSequenciaDeRetalhoLateral(tracker, lotePrincipal, cortesRealizados);

        // Retalho final (sobra de comprimento)
        BigDecimal comprimentoRetalhoFinal = ordemComprimentoFinalCm.subtract(comprimentoAcumuladoProdutos);
        if (comprimentoRetalhoFinal.compareTo(BigDecimal.ZERO) > 0) {
            cortesRealizados.add(criarCorteRetalho(parametros.larguraTotalLoteCm(), comprimentoRetalhoFinal, "FINAL")); // Usa a largura total do lote para o retalho final
            criarLoteDeRetalho(lotePrincipal, parametros.larguraTotalLoteCm(), comprimentoRetalhoFinal.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
        }

        return cortesRealizados;
    }

    private static class RetalhoLateralTracker {
        BigDecimal larguraSequencia = null;
        BigDecimal comprimentoSequencia = BigDecimal.ZERO;
    }

    private void processarRetalhoLateral(RetalhoLateralTracker tracker, BigDecimal larguraRetalhoLinha, BigDecimal comprimentoLinha, LoteMateriaPrima lotePrincipal, List<CorteRealizadoResponseDTO> cortesRealizados) {
        if (larguraRetalhoLinha.compareTo(BigDecimal.ZERO) > 0) {
            // Se já existe uma sequência e a largura do retalho atual é a mesma, acumula o comprimento.
            if (tracker.larguraSequencia != null && larguraRetalhoLinha.compareTo(tracker.larguraSequencia) == 0) {
                tracker.comprimentoSequencia = tracker.comprimentoSequencia.add(comprimentoLinha);
            } else {
                // Se a largura for diferente, fecha a sequência anterior e inicia uma nova.
                fecharSequenciaDeRetalhoLateral(tracker, lotePrincipal, cortesRealizados);
                tracker.larguraSequencia = larguraRetalhoLinha;
                tracker.comprimentoSequencia = comprimentoLinha;
            }
        } else {
            // Se não há retalho nesta linha, fecha qualquer sequência que estava em andamento.
            fecharSequenciaDeRetalhoLateral(tracker, lotePrincipal, cortesRealizados);
        }
    }

    private void fecharSequenciaDeRetalhoLateral(RetalhoLateralTracker tracker, LoteMateriaPrima lotePrincipal, List<CorteRealizadoResponseDTO> cortesRealizados) {
        if (tracker.larguraSequencia != null && tracker.comprimentoSequencia.compareTo(BigDecimal.ZERO) > 0) {
            cortesRealizados.add(criarCorteRetalho(tracker.larguraSequencia, tracker.comprimentoSequencia, "LATERAL"));
            criarLoteDeRetalho(lotePrincipal, tracker.larguraSequencia, tracker.comprimentoSequencia.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP));
            // Reseta o tracker
            tracker.larguraSequencia = null;
            tracker.comprimentoSequencia = BigDecimal.ZERO;
        }
    }


    private CorteRealizadoResponseDTO criarCorteProduto(BigDecimal larguraProduto, BigDecimal comprimentoProduto, int quantidade) {
        return CorteRealizadoResponseDTO.builder()
                .larguraCm(larguraProduto)
                .comprimentoCm(comprimentoProduto)
                .quantidade(quantidade)
                .tipo("PRODUTO")
                .build();
    }

    private CorteRealizadoResponseDTO criarCorteRetalho(BigDecimal largura, BigDecimal comprimento, String retalhoCategoria) {
        return CorteRealizadoResponseDTO.builder()
                .larguraCm(largura)
                .comprimentoCm(comprimento)
                .quantidade(1)
                .tipo("RETALHO")
                .retalhoCategoria(retalhoCategoria)
                .build();
    }

    private void criarLoteDeRetalho(LoteMateriaPrima lotePrincipal, BigDecimal larguraSobraCm, BigDecimal comprimentoMetros) {
        if (larguraSobraCm.compareTo(BigDecimal.ZERO) <= 0 || comprimentoMetros.compareTo(BigDecimal.ZERO) <= 0) return;
        Map<String, Object> novosAtributos = Map.of("larguraMm", larguraSobraCm.multiply(new BigDecimal("10")).intValue());
        LoteMateriaPrima loteRetalho = LoteMateriaPrima.builder()
                .tipoMateriaPrima(lotePrincipal.getTipoMateriaPrima())
                .unidadeDeEstoque(lotePrincipal.getUnidadeDeEstoque())
                .atributos(novosAtributos)
                .loteDeOrigem(lotePrincipal)
                .build();
        MovimentacaoRequestDTO entradaRetalhoDTO = com.dcriar.api.dto.request.stock.MovimentacaoRequestDTO.builder()
                .tipo(TipoMovimentacao.ENTRADA_SOBRA)
                .quantidade(comprimentoMetros)
                .motivo("Retalho gerado pela Ordem de Produção a partir do Lote ID: " + lotePrincipal.getId())
                .build();
        MovimentacaoEstoqueLote entradaRetalho = MovimentacaoEstoqueLote.from(entradaRetalhoDTO, loteRetalho);
        loteRetalho.getMovimentacoes().add(entradaRetalho);
        loteMateriaPrimaRepository.save(loteRetalho);
    }

    private void registrarSaidaLote(LoteMateriaPrima lote, BigDecimal quantidade, String motivo) {
        MovimentacaoRequestDTO saidaDTO = com.dcriar.api.dto.request.stock.MovimentacaoRequestDTO.builder()
                .tipo(com.dcriar.domain.stock.entity.enums.TipoMovimentacao.SAIDA_PRODUCAO)
                .quantidade(quantidade.negate())
                .motivo(motivo)
                .build();
        MovimentacaoEstoqueLote saida = MovimentacaoEstoqueLote.from(saidaDTO, lote);
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

    public void registrarEntradaProduto(Produto produto, int quantidade, String motivo) {
        MovimentacaoEstoqueProdutoRequestDTO movimentacaoDTO = MovimentacaoEstoqueProdutoRequestDTO.builder()
                .produtoId(produto.getId())
                .tipo(TipoMovimentacaoProduto.ENTRADA_PRODUCAO.name())
                .quantidade(quantidade)
                .motivo(motivo)
                .build();
        MovimentacaoEstoqueProduto entrada = MovimentacaoEstoqueProduto.from(movimentacaoDTO, produto);
        movimentacaoEstoqueProdutoRepository.save(entrada);
    }

    private void validarSaldoLoteCorte(LoteMateriaPrima lote, BigDecimal consumoEmMetros) {
        BigDecimal saldoAtual = movimentacaoEstoqueLoteRepository.findSaldoByLote(lote);
        if (consumoEmMetros.compareTo(saldoAtual) > 0) {
            throw new SaldoMateriaPrimaInsuficienteException(consumoEmMetros, saldoAtual);
        }
    }

    private Produto findProdutoById(Long id) {
        return produtoRepository.findById(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    private LoteMateriaPrima findLoteById(Long id) {
        return loteMateriaPrimaRepository.findById(id)
                .orElseThrow(() -> new LoteMateriaPrimaNaoEncontradoException(id));
    }

    private boolean isGeometricUnit(UnidadeDeMedida unidade) {
        return unidade != UnidadeDeMedida.METRO_LINEAR &&
                unidade != UnidadeDeMedida.CENTIMETRO_LINEAR &&
                unidade != UnidadeDeMedida.METRO_QUADRADO &&
                unidade != UnidadeDeMedida.CENTIMETRO_QUADRADO;
    }

    private boolean isDirectConsumptionUnit(UnidadeDeMedida unidade) {
        return unidade != UnidadeDeMedida.LITRO &&
                unidade != UnidadeDeMedida.MILILITRO &&
                unidade != UnidadeDeMedida.QUILOGRAMA &&
                unidade != UnidadeDeMedida.GRAMA &&
                unidade != UnidadeDeMedida.UNIDADE;
    }
}
