package com.dcriar.domain.production.service.impl;

import com.dcriar.api.dto.request.production.MargensRequestDTO;
import com.dcriar.api.dto.response.production.CorteRealizadoResponseDTO;
import com.dcriar.domain.product.entity.Dimensoes;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.product.entity.ProdutoDeCorte;
import com.dcriar.domain.production.model.ParametrosCorte;
import com.dcriar.domain.production.model.ResumoLayoutCorte;
import com.dcriar.domain.production.service.CorteCalculatorService;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.domain.stock.entity.enums.UnidadeDeMedida;
import com.dcriar.exception.custom.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Implementação do motor de cálculo geométrico para o planejamento de ordens de corte.
 * <p>
 * A principal responsabilidade desta classe é determinar a orientação ótima de um produto
 * (normal ou rotacionado) sobre um lote de matéria-prima para <strong>minimizar o consumo de comprimento</strong>,
 * e extrair todos os parâmetros necessários para a execução do corte.
 */
@Service
@RequiredArgsConstructor
public class CorteCalculatorServiceImpl implements CorteCalculatorService {

    // Contexto base compartilhado entre os fluxos automático e manual.
    private record ContextoBaseCorte(
            BigDecimal larguraTotalLoteCm,
            Optional<BigDecimal> comprimentoTotalLoteCm,
            BigDecimal larguraProduto,
            BigDecimal comprimentoProduto
    ) {
    }

    private ContextoBaseCorte extrairContextoBaseCorte(Produto produto, LoteMateriaPrima lotePrincipal) {
        if (!(produto instanceof ProdutoDeCorte produtoDeCorte)) {
            throw TipoProducaoIncompativelException.calculoCorteApenasParaProdutoDeCorte(produto.getNome());
        }

        BigDecimal larguraTotalLoteCm = getLarguraEmCm(lotePrincipal.getAtributos());
        Optional<BigDecimal> comprimentoTotalLoteCm = getComprimentoFisicoDisponivelEmCm(lotePrincipal, larguraTotalLoteCm);
        Dimensoes dimensoesProduto = produtoDeCorte.getDimensoes();

        return new ContextoBaseCorte(
                larguraTotalLoteCm,
                comprimentoTotalLoteCm,
                dimensoesProduto.getLarguraCm(),
                dimensoesProduto.getComprimentoCm()
        );
    }

    /**
     * Extrai os parâmetros de corte otimizados para uma dada produção em modo automático.
     * <p>
     * O fluxo de cálculo é o seguinte:
     * <ol>
     *     <li>Calcula a largura útil do lote, subtraindo as margens laterais.</li>
     *     <li>Testa as orientações normal e rotacionada do produto para ver qual delas consome menos comprimento linear do lote.</li>
     *     <li>Com a orientação ótima definida, calcula o número de produtos que cabem por linha.</li>
     *     <li>Calcula a largura final do bloco de produtos (produtos + margens) e o retalho lateral resultante.</li>
     * </ol>
     *
     * @param quantidade      A quantidade de produtos a serem produzidos.
     * @param produto         O produto a ser cortado.
     * @param lotePrincipal   O lote de matéria-prima a ser utilizado.
     * @param margensRequest  As margens de segurança a serem aplicadas no corte.
     * @return um objeto {@link ParametrosCorte} contendo os dados calculados para o layout de corte mais eficiente.
     */
    @Override
    public ParametrosCorte extrairParametrosCorte(
            int quantidade,
            Produto produto,
            LoteMateriaPrima lotePrincipal,
            MargensRequestDTO margensRequest
    ) {
        ContextoBaseCorte contextoBase = extrairContextoBaseCorte(produto, lotePrincipal);

        // 1. Obter dimensões e margens
        BigDecimal larguraTotalLoteCm = contextoBase.larguraTotalLoteCm();
        Optional<BigDecimal> comprimentoTotalLoteCm = contextoBase.comprimentoTotalLoteCm();
        BigDecimal larguraProduto = contextoBase.larguraProduto();
        BigDecimal comprimentoProduto = contextoBase.comprimentoProduto();
        BigDecimal margemEsquerda = Optional.ofNullable(margensRequest != null ? margensRequest.getEsquerda() : null).orElse(BigDecimal.ZERO);
        BigDecimal margemDireita = Optional.ofNullable(margensRequest != null ? margensRequest.getDireita() : null).orElse(BigDecimal.ZERO);
        BigDecimal margemSuperior = Optional.ofNullable(margensRequest != null ? margensRequest.getSuperior() : null).orElse(BigDecimal.ZERO);
        BigDecimal margemInferior = Optional.ofNullable(margensRequest != null ? margensRequest.getInferior() : null).orElse(BigDecimal.ZERO);
        BigDecimal margensVerticais = margemSuperior.add(margemInferior);

        // 2. Determinar orientação ótima (considerando apenas a largura disponível para produtos, sem margens)
        BigDecimal larguraDisponivelParaProdutos = larguraTotalLoteCm.subtract(margemEsquerda).subtract(margemDireita);
        if (larguraDisponivelParaProdutos.compareTo(BigDecimal.ZERO) <= 0) {
            throw ProdutoNaoCabeNoLoteException.margensLateraisExcedemLarguraLote();
        }

        int produtosPorLinhaNormal = calcularProdutosPorLinhaSemMargem(larguraDisponivelParaProdutos, larguraProduto);
        int produtosPorLinhaRotacionado = calcularProdutosPorLinhaSemMargem(larguraDisponivelParaProdutos, comprimentoProduto);

        if (produtosPorLinhaNormal == 0 && produtosPorLinhaRotacionado == 0) {
            throw ProdutoNaoCabeNoLoteException.produtoNaoCabeEmNenhumaOrientacao();
        }

        int linhasNormal = calcularLinhas(quantidade, produtosPorLinhaNormal);
        BigDecimal comprimentoTotalNormal = (produtosPorLinhaNormal > 0)
                ? comprimentoProduto.multiply(new BigDecimal(linhasNormal)).add(margensVerticais)
                : BigDecimal.valueOf(Long.MAX_VALUE);

        int linhasRotacionado = calcularLinhas(quantidade, produtosPorLinhaRotacionado);
        BigDecimal comprimentoTotalRotacionado = (produtosPorLinhaRotacionado > 0)
                ? larguraProduto.multiply(new BigDecimal(linhasRotacionado)).add(margensVerticais)
                : BigDecimal.valueOf(Long.MAX_VALUE);
        BigDecimal comprimentoTotalNormalCalculado = comprimentoTotalNormal;
        BigDecimal comprimentoTotalRotacionadoCalculado = comprimentoTotalRotacionado;

        // Validar contra o comprimento do lote, se existir
        if (comprimentoTotalLoteCm.isPresent() && comprimentoTotalNormal.compareTo(comprimentoTotalLoteCm.get()) > 0) {
            comprimentoTotalNormal = BigDecimal.valueOf(Long.MAX_VALUE);
        }
        if (comprimentoTotalLoteCm.isPresent() && comprimentoTotalRotacionado.compareTo(comprimentoTotalLoteCm.get()) > 0) {
            comprimentoTotalRotacionado = BigDecimal.valueOf(Long.MAX_VALUE);
        }

        if (comprimentoTotalNormal.equals(BigDecimal.valueOf(Long.MAX_VALUE)) && comprimentoTotalRotacionado.equals(BigDecimal.valueOf(Long.MAX_VALUE))) {
            throw QuantidadeExcedeCapacidadeLoteException.ambasOrientacoes(
                    quantidade,
                    comprimentoTotalNormalCalculado,
                    comprimentoTotalRotacionadoCalculado,
                    comprimentoTotalLoteCm.orElse(BigDecimal.ZERO)
            );
        }

        // --- Lógica: prioriza menor comprimento, depois maior retalho lateral ---
        BigDecimal larguraBlocoProdutosNormal = larguraProduto.multiply(new BigDecimal(Math.min(quantidade, produtosPorLinhaNormal)));
        BigDecimal larguraBlocoProdutosRotacionado = comprimentoProduto.multiply(new BigDecimal(Math.min(quantidade, produtosPorLinhaRotacionado)));

        boolean orientacaoOtimaEhRotacionado;
        if (comprimentoTotalRotacionado.compareTo(comprimentoTotalNormal) < 0) {
            orientacaoOtimaEhRotacionado = true;
        } else if (comprimentoTotalRotacionado.compareTo(comprimentoTotalNormal) > 0) {
            orientacaoOtimaEhRotacionado = false;
        } else {
            // Comprimento igual: prioriza maior retalho lateral (menor larguraBlocoProdutos)
            orientacaoOtimaEhRotacionado = larguraBlocoProdutosRotacionado.compareTo(larguraBlocoProdutosNormal) < 0;
        }

        int produtosPorLinhaOtima = orientacaoOtimaEhRotacionado ? produtosPorLinhaRotacionado : produtosPorLinhaNormal;
        BigDecimal larguraProdutoNaOrientacaoOtima = orientacaoOtimaEhRotacionado ? comprimentoProduto : larguraProduto;
        BigDecimal comprimentoProdutoNaOrientacaoOtima = orientacaoOtimaEhRotacionado ? larguraProduto : comprimentoProduto;

        // 3. Calcular a largura real do bloco de produtos e o retalho lateral
        int produtosNaLinha = Math.min(quantidade, produtosPorLinhaOtima);
        BigDecimal larguraBlocoProdutosFinal = larguraProdutoNaOrientacaoOtima.multiply(new BigDecimal(produtosNaLinha))
                .add(margemEsquerda).add(margemDireita);

        if (larguraBlocoProdutosFinal.compareTo(larguraTotalLoteCm) > 0) {
            larguraBlocoProdutosFinal = larguraTotalLoteCm;
        }

        BigDecimal larguraRetalhoLateralFinal = larguraTotalLoteCm.subtract(larguraBlocoProdutosFinal);
        if (larguraRetalhoLateralFinal.compareTo(BigDecimal.ZERO) < 0) {
            larguraRetalhoLateralFinal = BigDecimal.ZERO;
        }

        // 4. Retornar os parâmetros finais
        return new ParametrosCorte(
                larguraTotalLoteCm,
                larguraProdutoNaOrientacaoOtima,
                comprimentoProdutoNaOrientacaoOtima,
                quantidade,
                margemEsquerda,
                margemDireita,
                produtosPorLinhaOtima,
                orientacaoOtimaEhRotacionado,
                larguraBlocoProdutosFinal,
                larguraRetalhoLateralFinal
        );
    }

    public ParametrosCorte extrairParametrosCorteManual(
            int quantidade,
            Produto produto,
            LoteMateriaPrima lotePrincipal,
            BigDecimal larguraCorteManualCm,
            BigDecimal comprimentoCorteManualCm
    ) {
        ContextoBaseCorte contextoBase = extrairContextoBaseCorte(produto, lotePrincipal);

        BigDecimal larguraTotalLoteCm = contextoBase.larguraTotalLoteCm();
        BigDecimal larguraProduto = contextoBase.larguraProduto();
        BigDecimal comprimentoProduto = contextoBase.comprimentoProduto();
        Optional<BigDecimal> comprimentoTotalLoteCm = contextoBase.comprimentoTotalLoteCm();

        if (larguraCorteManualCm.compareTo(larguraTotalLoteCm) > 0) {
            throw DimensoesManuaisInvalidasException.larguraMaiorQueLote(larguraCorteManualCm, larguraTotalLoteCm);
        }
        if (comprimentoTotalLoteCm.isPresent() && comprimentoCorteManualCm.compareTo(comprimentoTotalLoteCm.get()) > 0) {
            throw DimensoesManuaisInvalidasException.comprimentoMaiorQueLote(
                    comprimentoCorteManualCm,
                    comprimentoTotalLoteCm.get()
            );
        }

        BigDecimal larguraRetalhoLateralFinal = larguraTotalLoteCm.subtract(larguraCorteManualCm);

        return new ParametrosCorte(
                larguraTotalLoteCm,
                larguraProduto,
                comprimentoProduto,
                quantidade,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                quantidade,
                false,
                larguraCorteManualCm,
                larguraRetalhoLateralFinal
        );
    }

    @Override
    public ResumoLayoutCorte calcularLayoutDetalhado(ParametrosCorte parametros, BigDecimal ordemComprimentoFinalCm, boolean isModoManual) {
        List<CorteRealizadoResponseDTO> cortesRealizados = new ArrayList<>();

        if (isModoManual) {
            return calcularLayoutManual(parametros, ordemComprimentoFinalCm, cortesRealizados);
        }

        int produtosPorLinha = parametros.produtosPorLinha();
        if (produtosPorLinha <= 0) {
            return ResumoLayoutCorte.builder()
                    .cortes(cortesRealizados)
                    .produtosPorLinha(0)
                    .numeroLinhasCompletas(0)
                    .produtosNaUltimaLinha(0)
                    .sobraLateral("")
                    .sobraInferior("")
                    .saldoRolo("")
                    .build();
        }

        int numeroLinhasCompletas = parametros.quantidade() / produtosPorLinha;
        int produtosNaUltimaLinha = parametros.quantidade() % produtosPorLinha;
        if (produtosNaUltimaLinha == 0 && parametros.quantidade() > 0) {
            numeroLinhasCompletas = parametros.quantidade() / produtosPorLinha;
        }

        // --- Agrupamento de cortes realizados ---
        if (numeroLinhasCompletas > 0) {
            cortesRealizados.add(CorteRealizadoResponseDTO.builder()
                .larguraCm(parametros.larguraProduto())
                .comprimentoCm(parametros.comprimentoProduto())
                .quantidade(produtosPorLinha)
                .tipo("PRODUTO")
                .repeticoes(numeroLinhasCompletas)
                .build());
        }
        if (produtosNaUltimaLinha > 0) {
            cortesRealizados.add(CorteRealizadoResponseDTO.builder()
                .larguraCm(parametros.larguraProduto())
                .comprimentoCm(parametros.comprimentoProduto())
                .quantidade(produtosNaUltimaLinha)
                .tipo("PRODUTO")
                .repeticoes(1)
                .build());
        }

        // --- ETAPA 5: Gerar cortes de RETALHO ---
        String sobraLateralStr = "";
        if (parametros.larguraRetalhoLateralCm().compareTo(BigDecimal.ZERO) > 0) {
            cortesRealizados.add(criarCorteRetalho(parametros.larguraRetalhoLateralCm(), ordemComprimentoFinalCm, "LATERAL"));
            sobraLateralStr = formatarDimensao(parametros.larguraRetalhoLateralCm(), ordemComprimentoFinalCm);
        }

        String sobraInferiorStr = "";
        if (!isModoManual && produtosNaUltimaLinha > 0 && produtosNaUltimaLinha < produtosPorLinha) {
            BigDecimal larguraSobraInferior = parametros.larguraTotalLoteCm()
                    .subtract(parametros.larguraProduto().multiply(new BigDecimal(produtosNaUltimaLinha)))
                    .subtract(parametros.larguraRetalhoLateralCm())
                    .subtract(parametros.margemEsquerda())
                    .subtract(parametros.margemDireita());

            if (larguraSobraInferior.compareTo(BigDecimal.ZERO) > 0) {
                cortesRealizados.add(criarCorteRetalho(larguraSobraInferior, parametros.comprimentoProduto(), "INFERIOR"));
                sobraInferiorStr = formatarDimensao(larguraSobraInferior, parametros.comprimentoProduto());
            }
        }

        String saldoRoloStr = "";

        return ResumoLayoutCorte.builder()
                .cortes(cortesRealizados)
                .produtosPorLinha(produtosPorLinha)
                .numeroLinhasCompletas(numeroLinhasCompletas)
                .produtosNaUltimaLinha(produtosNaUltimaLinha)
                .sobraLateral(sobraLateralStr)
                .sobraInferior(sobraInferiorStr)
                .saldoRolo(saldoRoloStr)
                .build();
    }

    private ResumoLayoutCorte calcularLayoutManual(
            ParametrosCorte parametros,
            BigDecimal ordemComprimentoFinalCm,
            List<CorteRealizadoResponseDTO> cortesRealizados
    ) {
        if (parametros.quantidade() > 0) {
            cortesRealizados.add(CorteRealizadoResponseDTO.builder()
                    .larguraCm(parametros.larguraProduto())
                    .comprimentoCm(parametros.comprimentoProduto())
                    .quantidade(parametros.quantidade())
                    .tipo("PRODUTO")
                    .repeticoes(1)
                    .build());
        }

        String sobraLateralStr = "";
        if (parametros.larguraRetalhoLateralCm().compareTo(BigDecimal.ZERO) > 0) {
            cortesRealizados.add(criarCorteRetalho(parametros.larguraRetalhoLateralCm(), ordemComprimentoFinalCm, "LATERAL"));
            sobraLateralStr = formatarDimensao(parametros.larguraRetalhoLateralCm(), ordemComprimentoFinalCm);
        }

        return ResumoLayoutCorte.builder()
                .cortes(cortesRealizados)
                .produtosPorLinha(parametros.quantidade())
                .numeroLinhasCompletas(parametros.quantidade() > 0 ? 1 : 0)
                .produtosNaUltimaLinha(0)
                .sobraLateral(sobraLateralStr)
                .sobraInferior("")
                .saldoRolo("")
                .build();
    }

    // --- Métodos Auxiliares de Layout ---

    private CorteRealizadoResponseDTO criarCorteRetalho(BigDecimal largura, BigDecimal comprimento, String retalhoCategoria) {
        return CorteRealizadoResponseDTO.builder()
                .larguraCm(largura)
                .comprimentoCm(comprimento)
                .quantidade(1)
                .tipo("RETALHO")
                .retalhoCategoria(retalhoCategoria)
                .build();
    }

    private String formatarDimensao(BigDecimal largura, BigDecimal comprimento) {
        return String.format("%scm x %scm",
                largura.stripTrailingZeros().toPlainString(),
                comprimento.stripTrailingZeros().toPlainString());
    }

    // --- Métodos Auxiliares de Cálculo Base ---

    private BigDecimal getLarguraEmCm(Map<String, Object> atributos) {
        Object larguraMmObj = atributos.get("larguraMm");
        if (!(larguraMmObj instanceof Number)) {
            throw AtributoLoteInvalidoException.larguraMmInvalidaOuAusente();
        }
        return new BigDecimal(larguraMmObj.toString()).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Deriva o comprimento físico disponível do lote em centímetros.
     * <p>
     * Este método não faz a baixa do lote nem cria retalhos; ele apenas traduz o saldo atual
     * do lote para um comprimento utilizável pelo motor de layout, respeitando a unidade real
     * em que o lote é armazenado.
     */
    private Optional<BigDecimal> getComprimentoFisicoDisponivelEmCm(LoteMateriaPrima lote, BigDecimal larguraTotalLoteCm) {
        Map<String, Object> atributos = lote.getAtributos();
        Object comprimentoMmObj = atributos.get("comprimentoMm");
        if (!(comprimentoMmObj instanceof Number)) {
            return getComprimentoAPartirDoSaldo(lote, larguraTotalLoteCm);
        }
        return Optional.of(new BigDecimal(comprimentoMmObj.toString()).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP));
    }

    private Optional<BigDecimal> getComprimentoAPartirDoSaldo(LoteMateriaPrima lote, BigDecimal larguraTotalLoteCm) {
        BigDecimal saldoAtual = lote.getSaldoCalculado();
        if (saldoAtual == null || saldoAtual.compareTo(BigDecimal.ZERO) <= 0) {
            return Optional.empty();
        }

        UnidadeDeMedida unidadeDeEstoque = lote.getUnidadeDeEstoque();
        return switch (unidadeDeEstoque) {
            case METRO_LINEAR -> Optional.of(saldoAtual.multiply(new BigDecimal("100")));
            case CENTIMETRO_LINEAR -> Optional.of(saldoAtual);
            case METRO_QUADRADO -> Optional.of(
                    saldoAtual.multiply(new BigDecimal("10000"))
                            .divide(larguraTotalLoteCm, 2, RoundingMode.HALF_UP)
            );
            case CENTIMETRO_QUADRADO -> Optional.of(
                    saldoAtual.divide(larguraTotalLoteCm, 2, RoundingMode.HALF_UP)
            );
            default -> Optional.empty();
        };
    }

    private int calcularProdutosPorLinhaSemMargem(BigDecimal larguraTotalLoteCm, BigDecimal dimensaoProduto) {
        if (dimensaoProduto == null || dimensaoProduto.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return larguraTotalLoteCm.divide(dimensaoProduto, 0, RoundingMode.FLOOR).intValue();
    }

    private int calcularLinhas(int quantidade, int produtosPorLinha) {
        if (produtosPorLinha <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) quantidade / produtosPorLinha);
    }
}
