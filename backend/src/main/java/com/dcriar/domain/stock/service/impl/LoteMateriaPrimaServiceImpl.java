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
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
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

        LoteMateriaPrima novoLote = LoteMateriaPrima.from(requestDTO, tipoMateriaPrima);

        // 2. Calcula o custo por unidade de consumo (se aplicável).
        BigDecimal custoPorUnidadeBase = calcularCustoPorUnidadeBase(requestDTO, tipoMateriaPrima);

        // 3. Cria a movimentação de entrada inicial associada ao lote.
        MovimentacaoRequestDTO movimentacaoDTO = MovimentacaoRequestDTO.builder()
                .tipo(TipoMovimentacao.ENTRADA_COMPRA)
                .quantidade(requestDTO.getQuantidadeInicial())
                .motivo(requestDTO.getMotivo() != null ? requestDTO.getMotivo() : "Entrada inicial do lote no sistema.")
                .build();
        MovimentacaoEstoqueLote movimentacaoInicial = MovimentacaoEstoqueLote.from(movimentacaoDTO, novoLote);
        movimentacaoInicial.setCustoPorUnidadeBase(custoPorUnidadeBase);

        // 4. Adiciona a movimentação ao lote e persiste ambos.
        novoLote.getMovimentacoes().add(movimentacaoInicial);
        LoteMateriaPrima loteSalvo = loteMateriaPrimaRepository.save(novoLote);

        // 5. Enriquece a resposta com o saldo inicial.
        LoteMateriaPrimaResponseDTO responseDTO = loteMateriaPrimaMapper.toResponseDTO(loteSalvo);
        responseDTO.setSaldoEstoque(requestDTO.getQuantidadeInicial());

        return responseDTO;
    }

    /**
     * Atualiza os dados cadastrais de um lote de matéria-prima.
     * <p>
     * <b>Atenção:</b> Este método não altera movimentações de estoque existentes nem recalcula custos.
     * Ele é destinado a corrigir informações cadastrais do lote, como código, fornecedor ou atributos.
     *
     * @param id O ID do lote a ser atualizado.
     * @param requestDTO O DTO com os dados para atualização.
     * @return O {@link LoteMateriaPrimaResponseDTO} do lote atualizado, enriquecido com o saldo de estoque.
     * @throws LoteMateriaPrimaNaoEncontradoException se o lote não for encontrado.
     * @throws TipoMateriaPrimaNaoEncontradoException se um novo tipo de matéria-prima for especificado e não for encontrado.
     */
    @Override
    @Transactional
    public LoteMateriaPrimaResponseDTO update(Long id, LoteMateriaPrimaRequestDTO requestDTO) {
        LoteMateriaPrima lote = findLoteById(id);
        TipoMateriaPrima tipoMateriaPrima = null;
        if (requestDTO.getTipoMateriaPrimaId() != null) {
            tipoMateriaPrima = tipoMateriaPrimaRepository.findById(requestDTO.getTipoMateriaPrimaId())
                    .orElseThrow(() -> new TipoMateriaPrimaNaoEncontradoException(requestDTO.getTipoMateriaPrimaId()));
        }
        lote.updateFrom(requestDTO, tipoMateriaPrima);
        LoteMateriaPrima loteAtualizado = loteMateriaPrimaRepository.save(lote);
        
        // Enriquece a resposta com o saldo atualizado.
        BigDecimal saldo = calcularSaldo(loteAtualizado);
        LoteMateriaPrimaResponseDTO responseDTO = loteMateriaPrimaMapper.toResponseDTO(loteAtualizado);
        responseDTO.setSaldoEstoque(saldo);
        return responseDTO;
    }

    /**
     * Calcula o custo por unidade base (de consumo) de um lote de matéria-prima.
     * <p>
     * <b>Regras de Cálculo:</b>
     * <ul>
     *     <li>Se {@code custoTotalLote} for nulo, o cálculo é ignorado e o método retorna nulo.</li>
     *     <li><b>De Metro Linear para CM²:</b> Se a unidade de estoque for {@link UnidadeDeMedida#METRO_LINEAR},
     *     o sistema espera uma unidade de consumo de {@link UnidadeDeMedida#CENTIMETRO_QUADRADO} e exige o atributo 'larguraMm'
     *     para calcular a área total (largura x comprimento) e derivar o custo por cm².</li>
     *     <li><b>De Litro para ML:</b> Se a unidade de estoque for {@link UnidadeDeMedida#LITRO}, o sistema converte a quantidade para mililitros.</li>
     *     <li>Para outras combinações, assume-se uma conversão 1:1.</li>
     * </ul>
     *
     * @param dto O DTO de requisição do lote, contendo custo total e quantidade inicial.
     * @param tipo O tipo de matéria-prima associado ao lote.
     * @return O custo por unidade base como um {@link BigDecimal}, ou nulo se o custo total não for fornecido.
     * @throws CalculoCustoIncompativelException se a combinação de unidades for incompatível.
     * @throws AtributoLoteInvalidoException se o atributo 'larguraMm' for necessário e inválido.
     * @throws QuantidadeUnidadesInvalidaException se a quantidade total de unidades base for zero ou negativa.
     */
    private BigDecimal calcularCustoPorUnidadeBase(LoteMateriaPrimaRequestDTO dto, TipoMateriaPrima tipo) {
        if (dto.getCustoTotalLote() == null) {
            return null; // Custo não informado, não há o que calcular.
        }

        BigDecimal totalUnidadesBase;
        UnidadeDeMedida unidadeConsumo = tipo.getUnidadeDeConsumo();

        switch (dto.getUnidadeDeEstoque()) {
            case METRO_LINEAR -> {
                if (unidadeConsumo != UnidadeDeMedida.CENTIMETRO_QUADRADO) {
                    throw new CalculoCustoIncompativelException(UnidadeDeMedida.METRO_LINEAR, UnidadeDeMedida.CENTIMETRO_QUADRADO);
                }
                Object larguraMmObj = dto.getAtributos().get("larguraMm");
                if (!(larguraMmObj instanceof Number)) {
                    throw new AtributoLoteInvalidoException(String.format(
                            "Para lotes em %s, o atributo 'larguraMm' é obrigatório e deve ser um número para o cálculo de custo.",
                            UnidadeDeMedida.METRO_LINEAR.getDescricao()));
                }
                BigDecimal larguraCm = new BigDecimal(((Number) larguraMmObj).intValue()).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
                BigDecimal comprimentoCm = dto.getQuantidadeInicial().multiply(new BigDecimal("100"));
                totalUnidadesBase = larguraCm.multiply(comprimentoCm);
            }
            case LITRO -> {
                if (unidadeConsumo != UnidadeDeMedida.MILILITRO) {
                    throw new CalculoCustoIncompativelException(UnidadeDeMedida.LITRO, UnidadeDeMedida.MILILITRO);
                }
                totalUnidadesBase = dto.getQuantidadeInicial().multiply(new BigDecimal("1000"));
            }
            default -> totalUnidadesBase = dto.getQuantidadeInicial();
        }

        if (totalUnidadesBase.compareTo(BigDecimal.ZERO) <= 0) {
            throw new QuantidadeUnidadesInvalidaException();
        }

        // Divide o custo total pela quantidade total de unidades de consumo.
        return dto.getCustoTotalLote().divide(totalUnidadesBase, 8, RoundingMode.HALF_UP);
    }

    /**
     * Busca um lote de matéria-prima pelo seu ID e enriquece a resposta com o saldo de estoque.
     *
     * @param id O ID do lote a ser buscado.
     * @return O {@link LoteMateriaPrimaResponseDTO} do lote encontrado, com o campo {@code saldoEstoque} calculado.
     * @throws LoteMateriaPrimaNaoEncontradoException se o lote com o ID especificado não for encontrado.
     */
    @Override
    @Transactional(readOnly = true)
    public LoteMateriaPrimaResponseDTO findById(Long id) {
        LoteMateriaPrima lote = findLoteById(id);
        BigDecimal saldo = calcularSaldo(lote);

        LoteMateriaPrimaResponseDTO responseDTO = loteMateriaPrimaMapper.toResponseDTO(lote);
        responseDTO.setSaldoEstoque(saldo);

        return responseDTO;
    }

    /**
     * Lista todos os lotes de matéria-prima, com filtros opcionais, e enriquece cada um com seu saldo de estoque.
     *
     * @param tipoMateriaPrimaId O ID do tipo de matéria-prima para filtrar (opcional).
     * @param apenasLotesPrincipais Se true, filtra apenas lotes que não são sobras ({@code loteDeOrigemId} é nulo) (opcional).
     * @return Uma lista de {@link LoteMateriaPrimaResponseDTO}, cada um com o campo {@code saldoEstoque} calculado.
     */
    @Override
    @Transactional(readOnly = true)
    public List<LoteMateriaPrimaResponseDTO> findAll(Long tipoMateriaPrimaId, Boolean apenasLotesPrincipais) {
        Specification<LoteMateriaPrima> spec = LoteMateriaPrimaSpecification.comFiltros(tipoMateriaPrimaId, apenasLotesPrincipais);

        return loteMateriaPrimaRepository.findAll(spec).stream()
                .map(lote -> {
                    BigDecimal saldo = calcularSaldo(lote);
                    LoteMateriaPrimaResponseDTO dto = loteMateriaPrimaMapper.toResponseDTO(lote);
                    dto.setSaldoEstoque(saldo);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    /**
     * Registra uma nova movimentação de estoque (entrada ou saída) para um lote de matéria-prima.
     * <p>
     * <b>Regra de Negócio:</b> A validação de saldo é realizada <strong>apenas</strong> para movimentações de saída
     * (quantidade negativa). Se a saída resultar em um saldo negativo, a operação é bloqueada.
     *
     * @param loteId O ID do lote de matéria-prima.
     * @param requestDTO O DTO com os dados da movimentação (tipo, quantidade, motivo).
     * @return O {@link MovimentacaoResponseDTO} da movimentação registrada.
     * @throws LoteMateriaPrimaNaoEncontradoException se o lote com o ID especificado não for encontrado.
     * @throws EstoqueInsuficienteParaMovimentacaoException se não houver saldo suficiente para uma movimentação de saída.
     */
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

    /**
     * Lista todo o histórico de movimentações de um lote de matéria-prima específico.
     *
     * @param loteId O ID do lote cujo histórico será consultado.
     * @return Uma lista de {@link MovimentacaoResponseDTO} representando todas as movimentações do lote.
     * @throws LoteMateriaPrimaNaoEncontradoException se o lote com o ID especificado não for encontrado.
     */
    @Override
    @Transactional(readOnly = true)
    public List<MovimentacaoResponseDTO> listarMovimentacoesPorLote(Long loteId) {
        LoteMateriaPrima lote = findLoteById(loteId);
        return movimentacaoEstoqueLoteRepository.findAllByLote(lote).stream()
                .map(movimentacaoMapper::toResponseDTO)
                .collect(Collectors.toList());
    }

    /**
     * Busca uma entidade {@link LoteMateriaPrima} pelo seu ID.
     * Método auxiliar para evitar duplicação de código e centralizar o tratamento de "não encontrado".
     *
     * @param id O ID do lote a ser buscado.
     * @return A entidade {@link LoteMateriaPrima} encontrada.
     * @throws LoteMateriaPrimaNaoEncontradoException se o lote com o ID especificado não for encontrado.
     */
    private LoteMateriaPrima findLoteById(Long id) {
        return loteMateriaPrimaRepository.findById(id)
                .orElseThrow(() -> new LoteMateriaPrimaNaoEncontradoException(id));
    }

    /**
     * Calcula o saldo de estoque atual para um determinado lote de matéria-prima.
     * <p>
     * O saldo é a soma das quantidades de todas as movimentações associadas ao lote.
     *
     * @param lote O lote para o qual o saldo será calculado.
     * @return O saldo de estoque atual como um {@link BigDecimal}.
     */
    private BigDecimal calcularSaldo(LoteMateriaPrima lote) {
        return movimentacaoEstoqueLoteRepository.findSaldoByLote(lote);
    }
}
