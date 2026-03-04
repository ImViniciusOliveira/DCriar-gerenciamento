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
import com.dcriar.exception.custom.AtributoLoteInvalidoException;
import com.dcriar.exception.custom.MargemInvalidaException;
import com.dcriar.exception.custom.ProdutoNaoCabeNoLoteException;
import com.dcriar.exception.custom.TipoProducaoIncompativelException;
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

    @Override
    public ParametrosCorte extrairParametrosCorte(
            int quantidade,
            Produto produto,
            LoteMateriaPrima lotePrincipal,
            MargensRequestDTO margensRequest
    ) {
        if (!(produto instanceof ProdutoDeCorte produtoDeCorte)) {
            throw new TipoProducaoIncompativelException("Cálculo de corte só é aplicável a produtos do tipo 'CORTE'.");
        }

        // 1. Obter a largura total do lote e calcular a largura útil, descontando as margens.
        BigDecimal larguraTotalLoteCm = getLarguraEmCm(lotePrincipal.getAtributos());
        BigDecimal margemEsquerda = Optional.ofNullable(margensRequest != null ? margensRequest.getEsquerda() : null).orElse(BigDecimal.ZERO);
        BigDecimal margemDireita = Optional.ofNullable(margensRequest != null ? margensRequest.getDireita() : null).orElse(BigDecimal.ZERO);
        BigDecimal larguraUtilCm = calcularLarguraUtilCm(larguraTotalLoteCm, margemEsquerda, margemDireita);

        if (larguraUtilCm.compareTo(BigDecimal.ZERO) < 0) {
            throw new MargemInvalidaException("A soma das margens laterais não pode exceder a largura do lote.");
        }

        Dimensoes dimensoesProduto = produtoDeCorte.getDimensoes();
        BigDecimal larguraProduto = dimensoesProduto.getLarguraCm();
        BigDecimal comprimentoProduto = dimensoesProduto.getComprimentoCm();

        // --- Lógica de Otimização por Rotação ---
        // O objetivo é encontrar a orientação (normal ou rotacionada) que consome o menor comprimento do rolo de matéria-prima.

        // 2. Simulação 1: Orientação Normal (produto não rotacionado)
        int produtosPorLinhaNormal = calcularProdutosPorLinha(larguraUtilCm, larguraProduto);
        int linhasNormal = calcularLinhas(quantidade, produtosPorLinhaNormal);
        // Se a orientação for impossível (não cabe nenhum produto), atribui-se um custo infinito para desqualificá-la.
        BigDecimal comprimentoTotalNormal = (produtosPorLinhaNormal > 0)
                ? comprimentoProduto.multiply(new BigDecimal(linhasNormal))
                : BigDecimal.valueOf(Long.MAX_VALUE);

        // 3. Simulação 2: Orientação Rotacionada (produto rotacionado 90 graus)
        int produtosPorLinhaRotacionado = calcularProdutosPorLinha(larguraUtilCm, comprimentoProduto);
        int linhasRotacionado = calcularLinhas(quantidade, produtosPorLinhaRotacionado);
        // Atribui-se um custo infinito se a orientação for impossível.
        BigDecimal comprimentoTotalRotacionado = (produtosPorLinhaRotacionado > 0)
                ? larguraProduto.multiply(new BigDecimal(linhasRotacionado))
                : BigDecimal.valueOf(Long.MAX_VALUE);

        // 4. Decisão: Escolhe a orientação que resulta no menor consumo de comprimento.
        boolean rotacionado;
        if (produtosPorLinhaNormal == 0 && produtosPorLinhaRotacionado == 0) {
            throw new ProdutoNaoCabeNoLoteException("O produto não cabe na largura útil do lote em nenhuma orientação.");
        } else {
            // Compara o comprimento total consumido em cada simulação.
            rotacionado = comprimentoTotalRotacionado.compareTo(comprimentoTotalNormal) < 0;
        }

        // 5. Define os parâmetros finais com base na orientação escolhida.
        BigDecimal pLarguraFinal = rotacionado ? comprimentoProduto : larguraProduto;
        BigDecimal pComprimentoFinal = rotacionado ? larguraProduto : comprimentoProduto;
        int pProdutosPorLinhaFinal = rotacionado ? produtosPorLinhaRotacionado : produtosPorLinhaNormal;

        return new ParametrosCorte(
                larguraTotalLoteCm,
                pLarguraFinal,
                pComprimentoFinal,
                quantidade,
                margemEsquerda,
                margemDireita,
                larguraUtilCm,
                pProdutosPorLinhaFinal,
                rotacionado
        );
    }

    @Override
    public ResumoLayoutCorte calcularLayoutDetalhado(ParametrosCorte parametros, BigDecimal ordemComprimentoFinalCm) {
        List<CorteRealizadoResponseDTO> cortesRealizados = new ArrayList<>();
        int produtosRestantes = parametros.quantidade();
        RetalhoLateralTracker tracker = new RetalhoLateralTracker();
        BigDecimal comprimentoAcumuladoProdutos = BigDecimal.ZERO;

        int numeroLinhasCompletas = 0;
        int produtosNaUltimaLinha = 0;

        while (produtosRestantes > 0) {
            int produtosNestaLinha = Math.min(parametros.produtosPorLinha(), produtosRestantes);
            if (produtosNestaLinha <= 0) break;

            if (produtosNestaLinha == parametros.produtosPorLinha()) {
                numeroLinhasCompletas++;
            } else {
                produtosNaUltimaLinha = produtosNestaLinha;
            }

            cortesRealizados.add(criarCorteProduto(parametros.larguraProduto(), parametros.comprimentoProduto(), produtosNestaLinha));

            BigDecimal larguraProdutosOcupada = parametros.larguraProduto().multiply(new BigDecimal(produtosNestaLinha));
            BigDecimal larguraRetalhoLinha = parametros.larguraUtilCm().subtract(larguraProdutosOcupada);
            BigDecimal comprimentoLinha = parametros.comprimentoProduto();

            processarRetalhoLateral(tracker, larguraRetalhoLinha, comprimentoLinha, cortesRealizados);

            comprimentoAcumuladoProdutos = comprimentoAcumuladoProdutos.add(comprimentoLinha);
            produtosRestantes -= produtosNestaLinha;
        }

        // Se não houve linha parcial, a última linha completa é a última linha.
        if (produtosNaUltimaLinha == 0 && numeroLinhasCompletas > 0) {
            produtosNaUltimaLinha = parametros.produtosPorLinha();
            numeroLinhasCompletas--;
        }

        fecharSequenciaDeRetalhoLateral(tracker, cortesRealizados);

        BigDecimal comprimentoRetalhoFinal = ordemComprimentoFinalCm.subtract(comprimentoAcumuladoProdutos);
        String sobraFinalStr = "";
        if (comprimentoRetalhoFinal.compareTo(BigDecimal.ZERO) > 0) {
            cortesRealizados.add(criarCorteRetalho(parametros.larguraTotalLoteCm(), comprimentoRetalhoFinal, "FINAL"));
            sobraFinalStr = formatarDimensao(parametros.larguraTotalLoteCm(), comprimentoRetalhoFinal);
        }

        // Extrair sobra lateral da lista de cortes (se existir)
        String sobraLateralStr = cortesRealizados.stream()
                .filter(c -> "RETALHO".equals(c.getTipo()) && "LATERAL".equals(c.getRetalhoCategoria()))
                .map(c -> formatarDimensao(c.getLarguraCm(), c.getComprimentoCm()))
                .findFirst()
                .orElse("");

        return ResumoLayoutCorte.builder()
                .cortes(cortesRealizados)
                .produtosPorLinha(parametros.produtosPorLinha())
                .numeroLinhasCompletas(numeroLinhasCompletas)
                .produtosNaUltimaLinha(produtosNaUltimaLinha)
                .sobraLateral(sobraLateralStr)
                .sobraFinal(sobraFinalStr)
                .build();
    }

    // --- Métodos Auxiliares de Layout ---

    private static class RetalhoLateralTracker {
        BigDecimal larguraSequencia = null;
        BigDecimal comprimentoSequencia = BigDecimal.ZERO;
    }

    private void processarRetalhoLateral(RetalhoLateralTracker tracker, BigDecimal larguraRetalhoLinha, BigDecimal comprimentoLinha, List<CorteRealizadoResponseDTO> cortesRealizados) {
        if (larguraRetalhoLinha.compareTo(BigDecimal.ZERO) > 0) {
            if (tracker.larguraSequencia != null && larguraRetalhoLinha.compareTo(tracker.larguraSequencia) == 0) {
                tracker.comprimentoSequencia = tracker.comprimentoSequencia.add(comprimentoLinha);
            } else {
                fecharSequenciaDeRetalhoLateral(tracker, cortesRealizados);
                tracker.larguraSequencia = larguraRetalhoLinha;
                tracker.comprimentoSequencia = comprimentoLinha;
            }
        } else {
            fecharSequenciaDeRetalhoLateral(tracker, cortesRealizados);
        }
    }

    private void fecharSequenciaDeRetalhoLateral(RetalhoLateralTracker tracker, List<CorteRealizadoResponseDTO> cortesRealizados) {
        if (tracker.larguraSequencia != null && tracker.comprimentoSequencia.compareTo(BigDecimal.ZERO) > 0) {
            cortesRealizados.add(criarCorteRetalho(tracker.larguraSequencia, tracker.comprimentoSequencia, "LATERAL"));
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
        // Converte o valor de milímetros (mm) para centímetros (cm).
        return new BigDecimal(larguraMmObj.toString()).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
    }

    private BigDecimal calcularLarguraUtilCm(BigDecimal larguraTotalLoteCm, BigDecimal margemEsquerda, BigDecimal margemDireita) {
        return larguraTotalLoteCm.subtract(margemEsquerda).subtract(margemDireita);
    }

    private int calcularProdutosPorLinha(BigDecimal larguraUtilCm, BigDecimal dimensaoProduto) {
        if (dimensaoProduto == null || dimensaoProduto.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }
        return larguraUtilCm.divide(dimensaoProduto, 0, RoundingMode.FLOOR).intValue();
    }

    private int calcularLinhas(int quantidade, int produtosPorLinha) {
        if (produtosPorLinha <= 0) {
            return 0;
        }
        return (int) Math.ceil((double) quantidade / produtosPorLinha);
    }
}
