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

        // 1. Obter a largura total do lote e as dimensões do produto.
        BigDecimal larguraTotalLoteCm = getLarguraEmCm(lotePrincipal.getAtributos());
        Dimensoes dimensoesProduto = produtoDeCorte.getDimensoes();
        BigDecimal larguraProduto = dimensoesProduto.getLarguraCm();
        BigDecimal comprimentoProduto = dimensoesProduto.getComprimentoCm();
        BigDecimal margemEsquerda = Optional.ofNullable(margensRequest != null ? margensRequest.getEsquerda() : null).orElse(BigDecimal.ZERO);
        BigDecimal margemDireita = Optional.ofNullable(margensRequest != null ? margensRequest.getDireita() : null).orElse(BigDecimal.ZERO);

        // --- Lógica de Otimização Estrita ---

        // 2. Determinar a orientação ótima SEM margens.
        int produtosPorLinhaNormalSemMargem = calcularProdutosPorLinhaSemMargem(larguraTotalLoteCm, larguraProduto);
        int linhasNormalSemMargem = calcularLinhas(quantidade, produtosPorLinhaNormalSemMargem);
        BigDecimal comprimentoTotalNormalSemMargem = (produtosPorLinhaNormalSemMargem > 0)
                ? comprimentoProduto.multiply(new BigDecimal(linhasNormalSemMargem))
                : BigDecimal.valueOf(Long.MAX_VALUE);

        int produtosPorLinhaRotacionadoSemMargem = calcularProdutosPorLinhaSemMargem(larguraTotalLoteCm, comprimentoProduto);
        int linhasRotacionadoSemMargem = calcularLinhas(quantidade, produtosPorLinhaRotacionadoSemMargem);
        BigDecimal comprimentoTotalRotacionadoSemMargem = (produtosPorLinhaRotacionadoSemMargem > 0)
                ? larguraProduto.multiply(new BigDecimal(linhasRotacionadoSemMargem))
                : BigDecimal.valueOf(Long.MAX_VALUE);

        if (produtosPorLinhaNormalSemMargem == 0 && produtosPorLinhaRotacionadoSemMargem == 0) {
            throw new ProdutoNaoCabeNoLoteException("O produto não cabe na largura do lote em nenhuma orientação.");
        }
        
        boolean orientacaoOtimaEhRotacionado = comprimentoTotalRotacionadoSemMargem.compareTo(comprimentoTotalNormalSemMargem) < 0;

        // 3. Aplicar margens APENAS na orientação ótima.
        int produtosPorLinhaFinal;
        if (orientacaoOtimaEhRotacionado) {
            produtosPorLinhaFinal = calcularProdutosPorLinhaComMargem(larguraTotalLoteCm, comprimentoProduto, margemEsquerda, margemDireita);
        } else {
            produtosPorLinhaFinal = calcularProdutosPorLinhaComMargem(larguraTotalLoteCm, larguraProduto, margemEsquerda, margemDireita);
        }

        // 4. Se a margem invalidou o layout ótimo (retornou 0), lançar exceção.
        if (produtosPorLinhaFinal == 0) {
            throw new MargemInvalidaException("A margem solicitada excede o espaço disponível no layout de corte otimizado.");
        }

        // 5. Define os parâmetros finais com base na orientação ótima.
        boolean rotacionado = orientacaoOtimaEhRotacionado;
        BigDecimal pLarguraFinal = rotacionado ? comprimentoProduto : larguraProduto;
        BigDecimal pComprimentoFinal = rotacionado ? larguraProduto : comprimentoProduto;
        
        BigDecimal larguraUtilCm = larguraTotalLoteCm.subtract(margemEsquerda).subtract(margemDireita);

        return new ParametrosCorte(
                larguraTotalLoteCm,
                pLarguraFinal,
                pComprimentoFinal,
                quantidade,
                margemEsquerda,
                margemDireita,
                larguraUtilCm,
                produtosPorLinhaFinal,
                rotacionado
        );
    }

    @Override
    public ResumoLayoutCorte calcularLayoutDetalhado(ParametrosCorte parametros, BigDecimal ordemComprimentoFinalCm) {
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
            produtosNaUltimaLinha = produtosPorLinha;
            // Se a última linha é completa, o número de linhas completas é o total de linhas.
            numeroLinhasCompletas = calcularLinhas(parametros.quantidade(), produtosPorLinha);
        }

        // --- ETAPA 3: Calcular dimensões das sobras LATERAL (r1) e INFERIOR (r2) ---
        BigDecimal larguraSobraLateral = parametros.larguraTotalLoteCm()
            .subtract(parametros.larguraProduto().multiply(new BigDecimal(produtosPorLinha)))
            .subtract(parametros.margemEsquerda())
            .subtract(parametros.margemDireita());

        BigDecimal larguraSobraInferior = BigDecimal.ZERO;
        if (produtosNaUltimaLinha > 0 && produtosNaUltimaLinha < produtosPorLinha) {
            larguraSobraInferior = parametros.larguraTotalLoteCm()
                .subtract(parametros.larguraProduto().multiply(new BigDecimal(produtosNaUltimaLinha)))
                .subtract(larguraSobraLateral)
                .subtract(parametros.margemEsquerda())
                .subtract(parametros.margemDireita());
        }

        // --- ETAPA 4: Gerar cortes de PRODUTO ---
        int produtosRestantes = parametros.quantidade();
        BigDecimal comprimentoAcumuladoProdutos = BigDecimal.ZERO;
        while (produtosRestantes > 0) {
            int produtosNestaLinha = Math.min(produtosPorLinha, produtosRestantes);
            cortesRealizados.add(criarCorteProduto(parametros.larguraProduto(), parametros.comprimentoProduto(), produtosNestaLinha));
            comprimentoAcumuladoProdutos = comprimentoAcumuladoProdutos.add(parametros.comprimentoProduto());
            produtosRestantes -= produtosNestaLinha;
        }

        // --- ETAPA 5: Gerar cortes de RETALHO ---
        String sobraLateralStr = "";
        if (larguraSobraLateral.compareTo(BigDecimal.ZERO) > 0) {
            cortesRealizados.add(criarCorteRetalho(larguraSobraLateral, comprimentoAcumuladoProdutos, "LATERAL"));
            sobraLateralStr = formatarDimensao(larguraSobraLateral, comprimentoAcumuladoProdutos);
        }

        String sobraInferiorStr = "";
        if (larguraSobraInferior.compareTo(BigDecimal.ZERO) > 0) {
            cortesRealizados.add(criarCorteRetalho(larguraSobraInferior, parametros.comprimentoProduto(), "INFERIOR"));
            sobraInferiorStr = formatarDimensao(larguraSobraInferior, parametros.comprimentoProduto());
        }

        String saldoRoloStr = "";
        BigDecimal comprimentoSaldoRolo = ordemComprimentoFinalCm.subtract(comprimentoAcumuladoProdutos);
        if (comprimentoSaldoRolo.compareTo(BigDecimal.ZERO) > 0) {
            cortesRealizados.add(criarCorteRetalho(parametros.larguraTotalLoteCm(), comprimentoSaldoRolo, "SALDO_ROLO"));
            saldoRoloStr = formatarDimensao(parametros.larguraTotalLoteCm(), comprimentoSaldoRolo);
        }

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
        // Converte o valor de milímetros (mm) para centímetros (cm).
        return new BigDecimal(larguraMmObj.toString()).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
    }

    /**
     * Calcula quantos produtos cabem na largura total, considerando que a soma (produtos + margens)
     * não pode exceder a largura total.
     */
    private int calcularProdutosPorLinhaComMargem(BigDecimal larguraTotalLoteCm, BigDecimal dimensaoProduto, BigDecimal margemEsquerda, BigDecimal margemDireita) {
        if (dimensaoProduto == null || dimensaoProduto.compareTo(BigDecimal.ZERO) <= 0) {
            return 0;
        }

        // 1. Cálculo teórico: Quantos cabem na largura total SEM margem?
        int maxProdutosTeorico = larguraTotalLoteCm.divide(dimensaoProduto, 0, RoundingMode.FLOOR).intValue();
        
        // 2. Validação Estrita: Se (maxProdutos * largura + margens) > larguraTotal, retorna 0.
        BigDecimal margemTotal = margemEsquerda.add(margemDireita);
        BigDecimal larguraOcupada = dimensaoProduto.multiply(new BigDecimal(maxProdutosTeorico));
        
        if (larguraOcupada.add(margemTotal).compareTo(larguraTotalLoteCm) > 0) {
             return 0;
        }
        
        return maxProdutosTeorico;
    }
    
    /**
     * Método auxiliar para verificar se o produto caberia SEM margem.
     * Usado apenas para diagnóstico de erro.
     */
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
