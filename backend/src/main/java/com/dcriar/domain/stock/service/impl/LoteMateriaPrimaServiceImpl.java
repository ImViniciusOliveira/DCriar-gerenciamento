package com.dcriar.domain.stock.service.impl;

import com.dcriar.api.dto.request.stock.LoteMateriaPrimaRequestDTO;
import com.dcriar.api.dto.request.stock.MovimentacaoRequestDTO;
import com.dcriar.api.dto.response.stock.LoteMateriaPrimaResponseDTO;
import com.dcriar.api.dto.response.stock.MovimentacaoResponseDTO;
import com.dcriar.api.mapper.stock.LoteMateriaPrimaMapper;
import com.dcriar.api.mapper.stock.MovimentacaoMapper;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.entity.MovimentacaoEstoqueLote;
import com.dcriar.domain.stock.entity.TipoMateriaPrima;
import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.dcriar.domain.stock.repository.LoteMateriaPrimaRepository;
import com.dcriar.domain.stock.repository.MovimentacaoEstoqueLoteRepository;
import com.dcriar.domain.stock.repository.TipoMateriaPrimaRepository;
import com.dcriar.domain.stock.repository.specification.LoteMateriaPrimaSpecification;
import com.dcriar.domain.stock.service.LoteMateriaPrimaService;
import com.dcriar.exception.custom.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementação da lógica de negócio para o gerenciamento de Lotes de Matéria-Prima.
 * <p>
 * Esta classe orquestra todas as operações de CRUD e regras de negócio
 * relacionadas aos lotes, incluindo a criação com movimentação inicial, cálculo de custo,
 * registro de movimentações com validação de saldo e enriquecimento de dados de resposta.
 */
@Service
@RequiredArgsConstructor
public class LoteMateriaPrimaServiceImpl implements LoteMateriaPrimaService {

    private static final int MAX_INTEGER_DIGITS_SUPPORTED = 19;

    private final LoteMateriaPrimaRepository loteMateriaPrimaRepository;
    private final TipoMateriaPrimaRepository tipoMateriaPrimaRepository;
    private final MovimentacaoEstoqueLoteRepository movimentacaoEstoqueLoteRepository;
    private final LoteMateriaPrimaMapper loteMateriaPrimaMapper;
    private final MovimentacaoMapper movimentacaoMapper;

    /**
     * Cria um novo lote de matéria-prima e registra sua movimentação de entrada inicial.
     * <p>
     * <b>Processo de Orquestração:</b>
     * <ol>
     *     <li>Associa o lote a um tipo de matéria-prima existente.</li>
     *     <li>Calcula o custo por unidade base (ex: custo por cm² ou ml) se o custo total for fornecido (ver {@link #calcularCustoPorUnidadeBase}).</li>
     *     <li>Cria uma movimentação inicial do tipo {@link TipoMovimentacao#ENTRADA_COMPRA} com a quantidade e o custo calculados.</li>
     *     <li>Persiste o lote e sua movimentação inicial de forma transacional.</li>
     * </ol>
     *
     * @param requestDTO O DTO com os dados para a criação do lote.
     * @return O {@link LoteMateriaPrimaResponseDTO} do lote recém-criado, enriquecido com seu saldo inicial.
     * @throws TipoMateriaPrimaNaoEncontradoException se o tipo de matéria-prima especificado não for encontrado.
     * @throws CalculoCustoIncompativelException se a combinação de unidades for incompatível para o cálculo de custo.
     * @throws AtributoLoteInvalidoException se um atributo necessário para o cálculo de custo (como 'larguraMm') for inválido.
     * @throws QuantidadeUnidadesInvalidaException se a quantidade de unidades base for inválida para o cálculo de custo.
     */
    @Override
    @Transactional
    public LoteMateriaPrimaResponseDTO create(LoteMateriaPrimaRequestDTO requestDTO) {
        // 1. Valida e busca o tipo de matéria-prima.
        TipoMateriaPrima tipoMateriaPrima = tipoMateriaPrimaRepository.findById(requestDTO.getTipoMateriaPrimaId())
                .orElseThrow(() -> new TipoMateriaPrimaNaoEncontradoException(requestDTO.getTipoMateriaPrimaId()));
        UnidadeDeMedida unidadeCadastroEstoque = resolverUnidadeCadastroEstoque(requestDTO);
        UnidadeDeMedida unidadeDeEstoqueInterna = resolverUnidadeInternaEstoque(tipoMateriaPrima, unidadeCadastroEstoque);
        validarCompatibilidadeUnidadesDoLote(
                tipoMateriaPrima,
                unidadeDeEstoqueInterna,
                unidadeCadastroEstoque
        );

        LoteMateriaPrima novoLote = LoteMateriaPrima.from(requestDTO, tipoMateriaPrima);
        novoLote.setUnidadeDeEstoque(unidadeDeEstoqueInterna);
        novoLote.setUnidadeCadastroEstoque(unidadeCadastroEstoque);
        BigDecimal quantidadeInicialInterna = converterQuantidadeInicialParaUnidadeInterna(requestDTO, tipoMateriaPrima);

        // 2. Calcula o custo por unidade de consumo (se aplicável).
        BigDecimal custoPorUnidadeBase = calcularCustoPorUnidadeBase(quantidadeInicialInterna, requestDTO.getCustoTotalLote());

        // 3. Cria a movimentação de entrada inicial associada ao lote.
        MovimentacaoRequestDTO movimentacaoDTO = MovimentacaoRequestDTO.builder()
                .tipo(TipoMovimentacao.ENTRADA_COMPRA)
                .quantidade(quantidadeInicialInterna)
                .motivo(requestDTO.getMotivo() != null ? requestDTO.getMotivo() : "Entrada inicial do lote no sistema.")
                .build();
        MovimentacaoEstoqueLote movimentacaoInicial = MovimentacaoEstoqueLote.from(movimentacaoDTO, novoLote);
        movimentacaoInicial.setCustoPorUnidadeBase(custoPorUnidadeBase);

        // 4. Adiciona a movimentação ao lote e persiste ambos.
        novoLote.getMovimentacoes().add(movimentacaoInicial);
        LoteMateriaPrima loteSalvo = loteMateriaPrimaRepository.save(novoLote);

        // 5. Enriquece a resposta com o saldo inicial.
        LoteMateriaPrimaResponseDTO responseDTO = loteMateriaPrimaMapper.toResponseDTO(loteSalvo);
        popularDadosDeApresentacao(responseDTO, loteSalvo, quantidadeInicialInterna);

        return responseDTO;
    }

    @Override
    @Transactional
    public LoteMateriaPrimaResponseDTO update(Long id, LoteMateriaPrimaRequestDTO requestDTO) {
        LoteMateriaPrima lote = findLoteById(id);
        TipoMateriaPrima tipoMateriaPrima = lote.getTipoMateriaPrima();
        if (requestDTO.getTipoMateriaPrimaId() != null) {
            tipoMateriaPrima = tipoMateriaPrimaRepository.findById(requestDTO.getTipoMateriaPrimaId())
                    .orElseThrow(() -> new TipoMateriaPrimaNaoEncontradoException(requestDTO.getTipoMateriaPrimaId()));
        }
        UnidadeDeMedida unidadeDeEstoque = requestDTO.getUnidadeDeEstoque() != null
                ? requestDTO.getUnidadeDeEstoque()
                : lote.getUnidadeDeEstoque();
        UnidadeDeMedida unidadeCadastroEstoque = requestDTO.getUnidadeCadastroEstoque() != null
                ? requestDTO.getUnidadeCadastroEstoque()
                : lote.getUnidadeCadastroEstoque();
        UnidadeDeMedida unidadeInternaEstoque = resolverUnidadeInternaEstoque(tipoMateriaPrima, unidadeCadastroEstoque);
        validarCompatibilidadeUnidadesDoLote(tipoMateriaPrima, unidadeInternaEstoque, unidadeCadastroEstoque);
        lote.updateFrom(requestDTO, tipoMateriaPrima);
        lote.setUnidadeDeEstoque(unidadeInternaEstoque);
        lote.setUnidadeCadastroEstoque(unidadeCadastroEstoque);
        LoteMateriaPrima loteAtualizado = loteMateriaPrimaRepository.save(lote);
        
        // Enriquece a resposta com o saldo atualizado.
        BigDecimal saldo = calcularSaldo(loteAtualizado);
        LoteMateriaPrimaResponseDTO responseDTO = loteMateriaPrimaMapper.toResponseDTO(loteAtualizado);
        popularDadosDeApresentacao(responseDTO, loteAtualizado, saldo);
        return responseDTO;
    }

    /**
     * Calcula o custo por unidade base (de consumo) de um lote de matéria-prima.
     * <p>
     * <b>Regras de Cálculo:</b>
     * <ul>
     *     <li>Se {@code custoTotalLote} for nulo, o cálculo é ignorado e o método retorna nulo.</li>
     *     <li>Para materiais de consumo, o cálculo sempre usa a quantidade já convertida para a menor unidade interna compatível do grupo.</li>
     *     <li>Para os demais materiais, o cálculo usa a quantidade na unidade efetiva do lote.</li>
     * </ul>
     *
     * @return O custo por unidade base como um {@link BigDecimal}, ou nulo se o custo total não for fornecido.
     * @throws QuantidadeUnidadesInvalidaException se a quantidade total de unidades base for zero ou negativa.
     */
    private BigDecimal calcularCustoPorUnidadeBase(BigDecimal quantidadeInicialInterna, BigDecimal custoTotalLote) {
        if (custoTotalLote == null) {
            return null; // Custo não informado, não há o que calcular.
        }

        if (quantidadeInicialInterna.compareTo(BigDecimal.ZERO) <= 0) {
            throw new QuantidadeUnidadesInvalidaException(quantidadeInicialInterna);
        }

        // Divide o custo total pela quantidade total de unidades de consumo.
        BigDecimal custoPorUnidadeBase = custoTotalLote.divide(quantidadeInicialInterna, 8, RoundingMode.HALF_UP);

        // Validação explícita para evitar overflow no banco de dados
        if (custoPorUnidadeBase.precision() - custoPorUnidadeBase.scale() > MAX_INTEGER_DIGITS_SUPPORTED) {
            throw new ValorNumericoExcedeLimiteException("Custo por Unidade Base", custoPorUnidadeBase, MAX_INTEGER_DIGITS_SUPPORTED);
        }

        return custoPorUnidadeBase;
    }

    private void validarCompatibilidadeUnidadesDoLote(
            TipoMateriaPrima tipoMateriaPrima,
            UnidadeDeMedida unidadeDeEstoque,
            UnidadeDeMedida unidadeCadastroEstoque
    ) {
        UnidadeDeMedida unidadePrincipal = tipoMateriaPrima.getUnidadeDeConsumo();
        if (unidadePrincipal.rejeitaComoUnidadeDeEstoque(unidadeDeEstoque)) {
            throw UnidadeEstoqueLoteInvalidaException.unidadeIncompativel(unidadePrincipal, unidadeDeEstoque);
        }
        if (unidadePrincipal.rejeitaComoUnidadeDeEstoque(unidadeCadastroEstoque)) {
            throw UnidadeEstoqueLoteInvalidaException.unidadeIncompativel(unidadePrincipal, unidadeCadastroEstoque);
        }
    }

    private UnidadeDeMedida resolverUnidadeCadastroEstoque(LoteMateriaPrimaRequestDTO requestDTO) {
        return requestDTO.getUnidadeCadastroEstoque() != null
                ? requestDTO.getUnidadeCadastroEstoque()
                : requestDTO.getUnidadeDeEstoque();
    }

    private UnidadeDeMedida resolverUnidadeInternaEstoque(TipoMateriaPrima tipoMateriaPrima, UnidadeDeMedida unidadeCadastroEstoque) {
        UnidadeDeMedida unidadePrincipal = tipoMateriaPrima.getUnidadeDeConsumo();
        if (unidadePrincipal.isConsumo() && !unidadePrincipal.isPermiteCorte()) {
            return unidadePrincipal.getUnidadeInternaDeCalculo();
        }
        return unidadeCadastroEstoque;
    }

    private BigDecimal converterQuantidadeInicialParaUnidadeInterna(LoteMateriaPrimaRequestDTO requestDTO, TipoMateriaPrima tipoMateriaPrima) {
        UnidadeDeMedida unidadePrincipal = tipoMateriaPrima.getUnidadeDeConsumo();
        UnidadeDeMedida unidadeCadastroEstoque = resolverUnidadeCadastroEstoque(requestDTO);
        if (unidadePrincipal.isConsumo() && !unidadePrincipal.isPermiteCorte()) {
            return unidadePrincipal.converterQuantidadeParaUnidadeInterna(
                    requestDTO.getQuantidadeInicial(),
                    unidadeCadastroEstoque
            );
        }
        return requestDTO.getQuantidadeInicial();
    }

    @Override
    @Transactional(readOnly = true)
    public LoteMateriaPrimaResponseDTO findById(Long id) {
        LoteMateriaPrima lote = findLoteById(id);
        BigDecimal saldo = calcularSaldo(lote);

        LoteMateriaPrimaResponseDTO responseDTO = loteMateriaPrimaMapper.toResponseDTO(lote);
        popularDadosDeApresentacao(responseDTO, lote, saldo);

        return responseDTO;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<LoteMateriaPrimaResponseDTO> findAll(Long tipoMateriaPrimaId, Boolean apenasLotesPrincipais, Pageable pageable) {
        Specification<LoteMateriaPrima> spec = LoteMateriaPrimaSpecification.comFiltros(tipoMateriaPrimaId, apenasLotesPrincipais);

        Page<LoteMateriaPrima> lotesPage = loteMateriaPrimaRepository.findAll(spec, pageable);

        // Mapeia a Page de entidades para uma Page de DTOs
        return lotesPage.map(lote -> {
            BigDecimal saldo = calcularSaldo(lote);
            LoteMateriaPrimaResponseDTO dto = loteMateriaPrimaMapper.toResponseDTO(lote);
            popularDadosDeApresentacao(dto, lote, saldo);
            dto.setAtributos(lote.getAtributos());
            return dto;
        });
    }

    @Override
    @Transactional
    public MovimentacaoResponseDTO registrarMovimentacao(Long loteId, MovimentacaoRequestDTO requestDTO) {
        // 1. Busca o lote e calcula seu saldo atual.
        LoteMateriaPrima lote = findLoteById(loteId);
        BigDecimal saldoAtual = calcularSaldo(lote);

        // 2. Valida o saldo apenas para movimentações de saída (quantidade < 0).
        if (requestDTO.getQuantidade().compareTo(BigDecimal.ZERO) < 0 &&
                saldoAtual.add(requestDTO.getQuantidade()).compareTo(BigDecimal.ZERO) < 0) {
            throw new EstoqueInsuficienteParaMovimentacaoException(
                    lote.getId(),
                    requestDTO.getQuantidade().abs().doubleValue(),
                    saldoAtual.doubleValue()
            );
        }

        // 3. Cria e persiste a nova movimentação.
        MovimentacaoEstoqueLote novaMovimentacao = MovimentacaoEstoqueLote.builder()
                .lote(lote)
                .tipo(requestDTO.getTipo())
                .quantidade(requestDTO.getQuantidade())
                .motivo(requestDTO.getMotivo())
                .build();

        MovimentacaoEstoqueLote movimentacaoSalva = movimentacaoEstoqueLoteRepository.save(novaMovimentacao);

        return movimentacaoMapper.toResponseDTO(movimentacaoSalva);
    }

    @Override
    @Transactional(readOnly = true)
    public List<MovimentacaoResponseDTO> listarMovimentacoesPorLote(Long loteId) {
        LoteMateriaPrima lote = findLoteById(loteId);
        return movimentacaoEstoqueLoteRepository.findAllByLote(lote).stream()
                .map(movimentacaoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void delete(Long id) {
        LoteMateriaPrima lote = findLoteById(id);
        
        // Verifica se houve alguma saída (consumo) deste lote.
        // Se tiver apenas entradas (criação, sobras, estornos), permite a exclusão.
        Map<String, Long> movimentacoesSaida = lote.getMovimentacoes().stream()
                .filter(mov -> mov.getQuantidade().compareTo(BigDecimal.ZERO) < 0)
                .collect(Collectors.groupingBy(
                        mov -> mov.getTipo().name(),
                        Collectors.counting()
                ));

        if (!movimentacoesSaida.isEmpty()) {
            throw new ExclusaoLoteBloqueadaException(id, movimentacoesSaida);
        }

        loteMateriaPrimaRepository.delete(lote);
    }

    private LoteMateriaPrima findLoteById(Long id) {
        return loteMateriaPrimaRepository.findById(id)
                .orElseThrow(() -> new LoteMateriaPrimaNaoEncontradoException(id));
    }

    private BigDecimal calcularSaldo(LoteMateriaPrima lote) {
        return movimentacaoEstoqueLoteRepository.findSaldoByLote(lote);
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
}
