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
import com.dcriar.api.mapper.production.PlanoDeConsumoMapper;
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
import com.dcriar.domain.production.model.PlanoDeConsumo;
import com.dcriar.domain.production.model.ResumoLayoutCorte;
import com.dcriar.domain.production.repository.OrdemDeProducaoRepository;
import com.dcriar.domain.production.service.ConsumoCalculatorService;
import com.dcriar.domain.production.service.CorteCalculatorService;
import com.dcriar.domain.production.service.OrdemDeProducaoService;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.entity.MovimentacaoEstoqueLote;
import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.dcriar.domain.stock.repository.LoteMateriaPrimaRepository;
import com.dcriar.domain.stock.repository.MovimentacaoEstoqueLoteRepository;
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
 * e por consumo direto, gerenciando a movimentação de estoque de matéria-prima e produtos acabados.
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
    private final ConsumoCalculatorService consumoCalculatorService;
    private final PlanoDeConsumoMapper planoDeConsumoMapper;

    @Override
    @Transactional
    public OrdemDeProducaoResponseDTO criarOrdemDeCorte(OrdemDeCorteRequestDTO requestDTO) {

        // 1. Validações iniciais e busca de entidades principais.
        Produto produto = findProdutoById(requestDTO.getProdutoId());
        if (!produto.getTipoMateriaPrima().getUnidadeDeConsumo().isPermiteCorte()) {
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
        ParametrosCorte parametros;

        // 2. Determina os parâmetros de corte (manual ou automático).
        if (requestDTO.getModoCalculo() == ModoCalculo.MANUAL) {
            parametros = corteCalculatorService.extrairParametrosCorteManual(
                    requestDTO.getQuantidadeProduzida(),
                    produto,
                    lotePrincipal,
                    requestDTO.getLarguraFinalCm()
            );
            larguraFinalCm = requestDTO.getLarguraFinalCm();
            comprimentoFinalCm = requestDTO.getComprimentoFinalCm();
            ResumoLayoutCorte resumo = corteCalculatorService.calcularLayoutDetalhado(parametros, comprimentoFinalCm, true);
            cortesRealizadosDTOs = resumo.cortes();

        } else { // MODO AUTOMÁTICO
            parametros = corteCalculatorService.extrairParametrosCorte(
                    requestDTO.getQuantidadeProduzida(), produto, lotePrincipal, requestDTO.getMargens()
            );

            long numeroDeLinhas = (long) Math.ceil((double) requestDTO.getQuantidadeProduzida() / parametros.produtosPorLinha());

            comprimentoFinalCm = parametros.comprimentoProduto().multiply(new BigDecimal(numeroDeLinhas));
            if (requestDTO.getMargens() != null) {
                comprimentoFinalCm = comprimentoFinalCm
                        .add(Optional.ofNullable(requestDTO.getMargens().getSuperior()).orElse(BigDecimal.ZERO))
                        .add(Optional.ofNullable(requestDTO.getMargens().getInferior()).orElse(BigDecimal.ZERO));
            }
            larguraFinalCm = parametros.larguraTotalLoteCm();
            
            ResumoLayoutCorte resumo = corteCalculatorService.calcularLayoutDetalhado(parametros, comprimentoFinalCm, false);
            cortesRealizadosDTOs = resumo.cortes();
        }

        consumoTotalMetros = comprimentoFinalCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

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
                            .ordemDeProducaoId(ordem.getId())
                            .build(),
                    ordem
            ));
        }

        OrdemDeProducao savedOrdem = ordemDeProducaoRepository.save(ordem);

        // 5. Orquestra as movimentações de estoque.
        registrarSaidaLote(lotePrincipal, consumoTotalMetros, "Consumido pela Ordem de Produção #" + savedOrdem.getId(), savedOrdem);
        registrarEntradaProduto(produto, requestDTO.getQuantidadeProduzida(), "Produzido via Ordem de Produção #" + savedOrdem.getId(), savedOrdem);
        distribuirEstoqueParaCanal(savedOrdem.getProduto().getId(), requestDTO.getCanalVendaDestinoId(), requestDTO.getQuantidadeProduzida());

        // 6. Criação de Lotes de Retalho
        for (CorteRealizadoResponseDTO dto : cortesRealizadosDTOs) {
             if ("RETALHO".equals(dto.getTipo())) {
                 criarLoteDeRetalho(lotePrincipal, dto.getLarguraCm(), dto.getComprimentoCm().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP), savedOrdem);
             }
        }

        return ordemDeProducaoMapper.toDto(savedOrdem);
    }

    @Override
    @Transactional
    public OrdemDeProducaoResponseDTO criarOrdemDeConsumoDireto(OrdemDeConsumoDiretoRequestDTO requestDTO) {
        // 1. Validações iniciais e busca de entidades.
        Produto produto = findProdutoById(requestDTO.getProdutoId());
        if (!produto.getTipoMateriaPrima().getUnidadeDeConsumo().isConsumoDireto()) {
            throw new TipoProducaoIncompativelException("Este produto não é para consumo direto.");
        }

        List<LoteMateriaPrima> lotesConsumidos = loteMateriaPrimaRepository.findAllById(requestDTO.getLotesConsumidosIds());
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

        OrdemDeProducao ordem = OrdemDeProducao.from(ordemRequestDTO, produto, new HashSet<>(lotesConsumidos), null);
        OrdemDeProducao savedOrdem = ordemDeProducaoRepository.save(ordem);

        // 4. Orquestra as movimentações de estoque.
        PlanoDeConsumo plano = consumoCalculatorService.calcularPlanoDeConsumo(lotesConsumidos, consumoTotalNecessario);
        plano.itens().forEach(item ->
                registrarSaidaLote(item.lote(), item.quantidadeAConsumir(), "Consumido pela Ordem de Produção #" + savedOrdem.getId(), savedOrdem)
        );

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
        OrdemDeProducao ordem = ordemDeProducaoRepository.findById(id)
                .orElseThrow(() -> new OrdemDeProducaoNaoEncontradaException(id));

        // 1. Validação: Produto Acabado
        Integer saldoAtualProduto = movimentacaoEstoqueProdutoRepository.findSaldoByProduto(ordem.getProduto());
        if (saldoAtualProduto < ordem.getQuantidadeProduzida()) {
            throw new ImpossivelExcluirProducaoException(
                    String.format("Estoque insuficiente para estorno. Produzido: %d, Saldo Atual: %d. Produtos já foram vendidos ou consumidos.",
                            ordem.getQuantidadeProduzida(), saldoAtualProduto)
            );
        }

        // 2. Validação: Retalhos Gerados
        List<LoteMateriaPrima> retalhosGerados = loteMateriaPrimaRepository.findByOrdemDeProducaoOrigem(ordem);
        for (LoteMateriaPrima retalho : retalhosGerados) {
            boolean temSaida = retalho.getMovimentacoes().stream()
                    .anyMatch(m -> m.getQuantidade().compareTo(BigDecimal.ZERO) < 0);
            
            if (temSaida) {
                 throw new ImpossivelExcluirProducaoException(
                    String.format("O retalho gerado (Lote #%d) já foi utilizado em outra produção.", retalho.getId())
            );
            }
        }

        // 3. Estorno: Produto Acabado
        MovimentacaoEstoqueProdutoRequestDTO estornoProdutoDTO = MovimentacaoEstoqueProdutoRequestDTO.builder()
                .produtoId(ordem.getProduto().getId())
                .tipo(TipoMovimentacaoProduto.ESTORNO_PRODUCAO.name())
                .quantidade(-ordem.getQuantidadeProduzida())
                .motivo("Estorno da Ordem de Produção #" + ordem.getId())
                .build();
        MovimentacaoEstoqueProduto estornoProduto = MovimentacaoEstoqueProduto.from(estornoProdutoDTO, ordem.getProduto());
        movimentacaoEstoqueProdutoRepository.save(estornoProduto);

        // 4. Estorno: Matéria-Prima
        List<MovimentacaoEstoqueLote> baixasMP = movimentacaoEstoqueLoteRepository.findByOrdemDeProducao(ordem);
        for (MovimentacaoEstoqueLote baixa : baixasMP) {
            if (baixa.getQuantidade().compareTo(BigDecimal.ZERO) >= 0) continue;

            MovimentacaoRequestDTO estornoMPDTO = MovimentacaoRequestDTO.builder()
                    .tipo(TipoMovimentacao.ESTORNO_PRODUCAO)
                    .quantidade(baixa.getQuantidade().abs())
                    .motivo("Estorno da Ordem de Produção #" + ordem.getId())
                    .build();
            MovimentacaoEstoqueLote estornoMP = MovimentacaoEstoqueLote.from(estornoMPDTO, baixa.getLote());
            estornoMP.setOrdemDeProducao(ordem);
            movimentacaoEstoqueLoteRepository.save(estornoMP);
        }

        // 5. Limpeza: Vínculos
        // Dissocia todas as movimentações da ordem antes de excluí-la para evitar erros de objeto transiente.
        List<MovimentacaoEstoqueProduto> movimentosProduto = movimentacaoEstoqueProdutoRepository.findByOrdemDeProducao(ordem);
        movimentosProduto.forEach(mov -> mov.setOrdemDeProducao(null));
        movimentacaoEstoqueProdutoRepository.saveAll(movimentosProduto);

        baixasMP.forEach(mov -> mov.setOrdemDeProducao(null));
        movimentacaoEstoqueLoteRepository.saveAll(baixasMP);

        // 6. Limpeza: Retalhos e Relações da Ordem
        loteMateriaPrimaRepository.deleteAll(retalhosGerados);
        ordem.getLotesConsumidos().clear();
        // Não é mais necessário salvar a ordem aqui, pois ela será deletada.
        // ordemDeProducaoRepository.save(ordem);

        // 7. Exclusão Final: Ordem de Produção
        ordemDeProducaoRepository.delete(ordem);
    }

    @Override
    @Transactional(readOnly = true)
    public OrdemDeProducaoResponseDTO buscarPorId(Long id) {
        return ordemDeProducaoRepository.findByIdWithDetails(id)
                .map(ordemDeProducaoMapper::toDto)
                .orElseThrow(() -> new OrdemDeProducaoNaoEncontradaException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<OrdemDeProducaoResponseDTO> listarPaginado(Pageable pageable) {
        Page<OrdemDeProducao> ordensPage = ordemDeProducaoRepository.findAll(pageable);
        return ordensPage.map(ordemDeProducaoMapper::toDto);
    }

    @Override
    public SimulacaoCorteResponseDTO simularCorte(SimulacaoCorteRequestDTO requestDTO) {
        Produto produto = findProdutoById(requestDTO.getProdutoId());
        if (!produto.getTipoMateriaPrima().getUnidadeDeConsumo().isPermiteCorte()) {
            throw new TipoProducaoIncompativelException("Este produto não utiliza uma matéria-prima geométrica para simulação de corte.");
        }
        LoteMateriaPrima loteParaSimulacao = findLoteById(requestDTO.getLoteId());

        // Validação de compatibilidade
        if (!loteParaSimulacao.getTipoMateriaPrima().getId().equals(produto.getTipoMateriaPrima().getId())) {
            throw new TipoProducaoIncompativelException(
                    produto.getNome(),
                    produto.getTipoMateriaPrima().getNome(),
                    loteParaSimulacao.getId(),
                    loteParaSimulacao.getTipoMateriaPrima().getNome()
            );
        }

        ParametrosCorte parametros = corteCalculatorService.extrairParametrosCorte(requestDTO.getQuantidade(), produto, loteParaSimulacao, null);
        
        // Cálculo do comprimento linear total (mesma lógica da criação da ordem)
        long numeroDeLinhasTotal = (long) Math.ceil((double) requestDTO.getQuantidade() / parametros.produtosPorLinha());
        BigDecimal comprimentoFinalCm = parametros.comprimentoProduto().multiply(new BigDecimal(numeroDeLinhasTotal));
        
        // Executa a simulação detalhada do layout
        ResumoLayoutCorte resumo = corteCalculatorService.calcularLayoutDetalhado(parametros, comprimentoFinalCm, false);

        BigDecimal consumoEstimado = comprimentoFinalCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        validarSaldoLoteCorte(loteParaSimulacao, consumoEstimado);

        return SimulacaoCorteResponseDTO.builder()
                .modoCalculo(ModoCalculo.AUTOMATICO)
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
                .larguraBlocoProdutosCm(parametros.larguraBlocoProdutosCm())
                .comprimentoBlocoProdutosCm(comprimentoFinalCm)
                .build();
    }

    @Override
    public SimulacaoCorteResponseDTO verificarCorte(VerificacaoCorteRequestDTO requestDTO) {
        Produto produto = findProdutoById(requestDTO.getProdutoId());
        LoteMateriaPrima lote = findLoteById(requestDTO.getLoteId());

        BigDecimal comprimentoFinalCm;
        ParametrosCorte parametros;
        boolean isModoManual = requestDTO.getModoCalculo() == ModoCalculo.MANUAL;

        if (isModoManual) {
            // Modo manual: usuário define dimensões do corte
            comprimentoFinalCm = requestDTO.getComprimentoFinalCm();
            parametros = corteCalculatorService.extrairParametrosCorteManual(
                    requestDTO.getQuantidade(),
                    produto,
                    lote,
                    requestDTO.getLarguraFinalCm()
            );

        } else {
            // Modo automático: sistema calcula com margens
            parametros = corteCalculatorService.extrairParametrosCorte(
                    requestDTO.getQuantidade(), produto, lote, requestDTO.getMargens()
            );

            long numeroDeLinhas = (long) Math.ceil((double) requestDTO.getQuantidade() / parametros.produtosPorLinha());
            comprimentoFinalCm = parametros.comprimentoProduto().multiply(new BigDecimal(numeroDeLinhas));
            
            if (requestDTO.getMargens() != null) {
                comprimentoFinalCm = comprimentoFinalCm
                        .add(Optional.ofNullable(requestDTO.getMargens().getSuperior()).orElse(BigDecimal.ZERO))
                        .add(Optional.ofNullable(requestDTO.getMargens().getInferior()).orElse(BigDecimal.ZERO));
            }
        }

        ResumoLayoutCorte resumo = corteCalculatorService.calcularLayoutDetalhado(parametros, comprimentoFinalCm, isModoManual);
        BigDecimal consumoEstimado = comprimentoFinalCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);

        validarSaldoLoteCorte(lote, consumoEstimado);

        return SimulacaoCorteResponseDTO.builder()
                .modoCalculo(requestDTO.getModoCalculo())
                .larguraFinalCm(parametros.larguraTotalLoteCm()) // Corrigido: largura total do lote
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
                .larguraBlocoProdutosCm(parametros.larguraBlocoProdutosCm())
                .comprimentoBlocoProdutosCm(comprimentoFinalCm)
                .build();
    }

    @Override
    public SimulacaoConsumoDiretoResponseDTO simularConsumoDireto(SimulacaoConsumoDiretoRequestDTO requestDTO) {
        Produto produto = findProdutoById(requestDTO.getProdutoId());
        if (!produto.getTipoMateriaPrima().getUnidadeDeConsumo().isConsumoDireto()) {
            throw new TipoProducaoIncompativelException("Este produto utiliza uma matéria-prima geométrica. Utilize o simulador de corte.");
        }

        List<LoteMateriaPrima> lotesConsumidos = loteMateriaPrimaRepository.findAllById(requestDTO.getLotesConsumidosIds());
        if (lotesConsumidos.size() != requestDTO.getLotesConsumidosIds().size()) {
            throw new LotesMateriaPrimaNaoEncontradosException(new HashSet<>(requestDTO.getLotesConsumidosIds()));
        }

        BigDecimal consumoTotalNecessario = new BigDecimal(produto.getUnidadesPorProduto() * requestDTO.getQuantidade());
        BigDecimal saldoTotalDisponivel = lotesConsumidos.stream()
                .map(movimentacaoEstoqueLoteRepository::findSaldoByLote)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (saldoTotalDisponivel.compareTo(consumoTotalNecessario) < 0) {
            throw new SaldoMateriaPrimaInsuficienteException(consumoTotalNecessario, saldoTotalDisponivel);
        }

        PlanoDeConsumo plano = consumoCalculatorService.calcularPlanoDeConsumo(lotesConsumidos, consumoTotalNecessario);

        return SimulacaoConsumoDiretoResponseDTO.builder()
                .consumoTotalEstimado(consumoTotalNecessario)
                .unidadeDeConsumo(produto.getTipoMateriaPrima().getUnidadeDeConsumo())
                .planoDeConsumo(plano.itens().stream().map(planoDeConsumoMapper::toDto).collect(Collectors.toList()))
                .saldoRestante(plano.saldosRestantes())
                .build();
    }

    private void criarLoteDeRetalho(LoteMateriaPrima lotePrincipal, BigDecimal larguraSobraCm, BigDecimal comprimentoMetros, OrdemDeProducao ordemOrigem) {
        if (larguraSobraCm.compareTo(BigDecimal.ZERO) <= 0 || comprimentoMetros.compareTo(BigDecimal.ZERO) <= 0) return;
        
        // 1. Calcular a quantidade real do retalho com base na unidade de estoque
        BigDecimal quantidadeRealRetalho;
        if (lotePrincipal.getUnidadeDeEstoque() == UnidadeDeMedida.METRO_QUADRADO) {
            // Área = Largura (m) * Comprimento (m)
            BigDecimal larguraMetros = larguraSobraCm.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            quantidadeRealRetalho = larguraMetros.multiply(comprimentoMetros);
        } else if (lotePrincipal.getUnidadeDeEstoque() == UnidadeDeMedida.METRO_LINEAR) {
            quantidadeRealRetalho = comprimentoMetros;
        } else {
            return;
        }

        // 2. Calcular o custo unitário do lote pai
        BigDecimal custoUnitario = calcularCustoUnitario(lotePrincipal);
        
        // 3. Calcular o custo total do retalho
        BigDecimal custoTotalRetalho = custoUnitario.multiply(quantidadeRealRetalho);

        Map<String, Object> novosAtributos = Map.of("larguraMm", larguraSobraCm.multiply(new BigDecimal("10")).intValue());
        LoteMateriaPrima loteRetalho = LoteMateriaPrima.builder()
                .tipoMateriaPrima(lotePrincipal.getTipoMateriaPrima())
                .unidadeDeEstoque(lotePrincipal.getUnidadeDeEstoque())
                .atributos(novosAtributos)
                .loteDeOrigem(lotePrincipal)
                .ordemDeProducaoOrigem(ordemOrigem)
                .custoTotalLote(custoTotalRetalho) // Definindo o custo calculado
                .motivo("Retalho gerado pela Ordem de Produção #" + ordemOrigem.getId()) // Motivo obrigatório
                .build();
        
        loteRetalho = loteMateriaPrimaRepository.save(loteRetalho);

        MovimentacaoRequestDTO entradaRetalhoDTO = com.dcriar.api.dto.request.stock.MovimentacaoRequestDTO.builder()
                .tipo(TipoMovimentacao.ENTRADA_SOBRA)
                .quantidade(quantidadeRealRetalho) // Usando a quantidade correta calculada
                .motivo("Retalho gerado pela Ordem de Produção #" + ordemOrigem.getId())
                .build();
        MovimentacaoEstoqueLote entradaRetalho = MovimentacaoEstoqueLote.from(entradaRetalhoDTO, loteRetalho);
        entradaRetalho.setOrdemDeProducao(ordemOrigem);
        
        movimentacaoEstoqueLoteRepository.save(entradaRetalho);
    }

    private BigDecimal calcularCustoUnitario(LoteMateriaPrima lote) {
        // Soma todas as entradas (COMPRA, SOBRA, AJUSTE positivo) para determinar a quantidade total adquirida/gerada
        BigDecimal quantidadeTotalEntrada = lote.getMovimentacoes().stream()
                .map(MovimentacaoEstoqueLote::getQuantidade)
                .filter(quantidade -> quantidade.compareTo(BigDecimal.ZERO) > 0)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (quantidadeTotalEntrada.compareTo(BigDecimal.ZERO) == 0) {
            // Evitar divisão por zero. Se não houve entrada, o custo unitário é indefinido (ou zero).
            // Isso não deve acontecer em um cenário real consistente.
            return BigDecimal.ZERO;
        }

        return lote.getCustoTotalLote().divide(quantidadeTotalEntrada, 4, RoundingMode.HALF_UP);
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
        BigDecimal saldoAtual = movimentacaoEstoqueLoteRepository.findSaldoByLote(lote);
        if (consumoEmMetros.compareTo(saldoAtual) > 0) {
            throw new SaldoMateriaPrimaInsuficienteException(consumoEmMetros, saldoAtual);
        }
    }


    private Produto findProdutoById(Long id) {
        return produtoRepository.findByIdWithTipoMateriaPrima(id)
                .orElseThrow(() -> new ProdutoNaoEncontradoException(id));
    }

    private LoteMateriaPrima findLoteById(Long id) {
        return loteMateriaPrimaRepository.findByIdWithTipoMateriaPrima(id)
                .orElseThrow(() -> new LoteMateriaPrimaNaoEncontradoException(id));
    }

    private String formatarDimensao(BigDecimal largura, BigDecimal comprimento) {
        return String.format("%scm x %scm",
                largura.stripTrailingZeros().toPlainString(),
                comprimento.stripTrailingZeros().toPlainString());
    }
}
