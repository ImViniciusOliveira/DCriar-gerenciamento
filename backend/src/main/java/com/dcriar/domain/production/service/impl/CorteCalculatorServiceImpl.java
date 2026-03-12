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
            throw new TipoProducaoIncompativelException("Cálculo de corte só é aplicável a produtos do tipo 'CORTE'.");
        }

        BigDecimal larguraTotalLoteCm = getLarguraEmCm(lotePrincipal.getAtributos());
        Optional<BigDecimal> comprimentoTotalLoteCm = getComprimentoOpcionalEmCm(lotePrincipal.getAtributos());
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
            throw new ProdutoNaoCabeNoLoteException("A soma das margens laterais é maior ou igual à largura do lote.");
        }

        int produtosPorLinhaNormal = calcularProdutosPorLinhaSemMargem(larguraDisponivelParaProdutos, larguraProduto);
        int produtosPorLinhaRotacionado = calcularProdutosPorLinhaSemMargem(larguraDisponivelParaProdutos, comprimentoProduto);

        if (produtosPorLinhaNormal == 0 && produtosPorLinhaRotacionado == 0) {
            throw new ProdutoNaoCabeNoLoteException("O produto não cabe na largura do lote em nenhuma orientação, mesmo após descontar as margens.");
        }

        int linhasNormal = calcularLinhas(quantidade, produtosPorLinhaNormal);
        BigDecimal comprimentoTotalNormal = (produtosPorLinhaNormal > 0)
                ? comprimentoProduto.multiply(new BigDecimal(linhasNormal)).add(margensVerticais)
                : BigDecimal.valueOf(Long.MAX_VALUE);

        int linhasRotacionado = calcularLinhas(quantidade, produtosPorLinhaRotacionado);
        BigDecimal comprimentoTotalRotacionado = (produtosPorLinhaRotacionado > 0)
                ? larguraProduto.multiply(new BigDecimal(linhasRotacionado)).add(margensVerticais)
                : BigDecimal.valueOf(Long.MAX_VALUE);

        // Validar contra o comprimento do lote, se existir
        if (comprimentoTotalLoteCm.isPresent() && comprimentoTotalNormal.compareTo(comprimentoTotalLoteCm.get()) > 0) {
            comprimentoTotalNormal = BigDecimal.valueOf(Long.MAX_VALUE);
        }
        if (comprimentoTotalLoteCm.isPresent() && comprimentoTotalRotacionado.compareTo(comprimentoTotalLoteCm.get()) > 0) {
            comprimentoTotalRotacionado = BigDecimal.valueOf(Long.MAX_VALUE);
        }

        if (comprimentoTotalNormal.equals(BigDecimal.valueOf(Long.MAX_VALUE)) && comprimentoTotalRotacionado.equals(BigDecimal.valueOf(Long.MAX_VALUE))) {
            throw new QuantidadeExcedeCapacidadeLoteException("A quantidade solicitada excede a capacidade do lote em ambas as orientações.");
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
            throw new DimensoesManuaisInvalidasException(larguraCorteManualCm, larguraTotalLoteCm);
        }
        if (comprimentoTotalLoteCm.isPresent() && comprimentoCorteManualCm.compareTo(comprimentoTotalLoteCm.get()) > 0) {
            throw new DimensoesManuaisInvalidasException(comprimentoCorteManualCm, comprimentoTotalLoteCm.get());
        }

        int produtosPorLinha = larguraCorteManualCm.divide(larguraProduto, 0, RoundingMode.FLOOR).intValue();
        BigDecimal larguraRetalhoLateralFinal = larguraTotalLoteCm.subtract(larguraCorteManualCm);

        return new ParametrosCorte(
                larguraTotalLoteCm,
                larguraProduto,
                comprimentoProduto,
                quantidade,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                produtosPorLinha,
                false,
                larguraCorteManualCm,
                larguraRetalhoLateralFinal
        );
    }

    @Override
    public ResumoLayoutCorte calcularLayoutDetalhado(ParametrosCorte parametros, BigDecimal ordemComprimentoFinalCm, boolean isModoManual) {
        List<CorteRealizadoResponseDTO> cortesRealizados = new ArrayList<>();

        // --- ETAPA 1: Validação de Segurança (Guard Clause) ---
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

        // --- ETAPA 2: Calcular contagem de linhas e produtos ---
        int numeroLinhasCompletas = parametros.quantidade() / produtosPorLinha;
        int produtosNaUltimaLinha = parametros.quantidade() % produtosPorLinha;
        if (produtosNaUltimaLinha == 0 && parametros.quantidade() > 0) {
            numeroLinhasCompletas = parametros.quantidade() / produtosPorLinha;
        }

        // --- ETAPA 3: Aplicar margens de comprimento ao bloco principal ---

        // --- ETAPA 4: Gerar cortes de PRODUTO ---
        int produtosRestantes = parametros.quantidade();
        while (produtosRestantes > 0) {
            int produtosNestaLinha = Math.min(produtosPorLinha, produtosRestantes);
            cortesRealizados.add(criarCorteProduto(parametros.larguraProduto(), parametros.comprimentoProduto(), produtosNestaLinha));
            produtosRestantes -= produtosNestaLinha;
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

        // --- ETAPA 6: Construir o resumo final ---
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

    // --- Métodos Auxiliares de Layout ---

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

    private String formatarDimensao(BigDecimal largura, BigDecimal comprimento) {
        return String.format("%scm x %scm",
                largura.stripTrailingZeros().toPlainString(),
                comprimento.stripTrailingZeros().toPlainString());
    }

    // --- Métodos Auxiliares de Cálculo Base ---

    private BigDecimal getLarguraEmCm(Map<String, Object> atributos) {
        Object larguraMmObj = atributos.get("larguraMm");
        if (!(larguraMmObj instanceof Number)) {
            throw new AtributoLoteInvalidoException("O atributo 'larguraMm' do lote é inválido ou não existe.");
        }
        return new BigDecimal(larguraMmObj.toString()).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
    }

    private Optional<BigDecimal> getComprimentoOpcionalEmCm(Map<String, Object> atributos) {
        Object comprimentoMmObj = atributos.get("comprimentoMm");
        if (!(comprimentoMmObj instanceof Number)) {
            return Optional.empty();
        }
        return Optional.of(new BigDecimal(comprimentoMmObj.toString()).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP));
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
