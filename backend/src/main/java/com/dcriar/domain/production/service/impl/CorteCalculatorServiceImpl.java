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
import com.dcriar.exception.custom.DimensoesManuaisInvalidasException;
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

    // Contexto base compartilhado entre os fluxos automático e manual.
    private record ContextoBaseCorte(
            BigDecimal larguraTotalLoteCm,
            BigDecimal larguraProduto,
            BigDecimal comprimentoProduto
    ) {}

    private ContextoBaseCorte extrairContextoBaseCorte(Produto produto, LoteMateriaPrima lotePrincipal) {
        if (!(produto instanceof ProdutoDeCorte produtoDeCorte)) {
            throw new TipoProducaoIncompativelException("Cálculo de corte só é aplicável a produtos do tipo 'CORTE'.");
        }

        BigDecimal larguraTotalLoteCm = getLarguraEmCm(lotePrincipal.getAtributos());
        Dimensoes dimensoesProduto = produtoDeCorte.getDimensoes();

        return new ContextoBaseCorte(
                larguraTotalLoteCm,
                dimensoesProduto.getLarguraCm(),
                dimensoesProduto.getComprimentoCm()
        );
    }

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
        BigDecimal larguraProduto = contextoBase.larguraProduto();
        BigDecimal comprimentoProduto = contextoBase.comprimentoProduto();
        BigDecimal margemEsquerda = Optional.ofNullable(margensRequest != null ? margensRequest.getEsquerda() : null).orElse(BigDecimal.ZERO);
        BigDecimal margemDireita = Optional.ofNullable(margensRequest != null ? margensRequest.getDireita() : null).orElse(BigDecimal.ZERO);
        BigDecimal margensLateraisTotais = margemEsquerda.add(margemDireita);

        BigDecimal larguraProdutoComMargens = larguraProduto.add(margensLateraisTotais);
        if (larguraProdutoComMargens.compareTo(larguraTotalLoteCm) > 0) {
            // Verifica se na orientação rotacionada caberia
            BigDecimal comprimentoProdutoComMargens = comprimentoProduto.add(margensLateraisTotais);
            if (comprimentoProdutoComMargens.compareTo(larguraTotalLoteCm) > 0) {
                throw new ProdutoNaoCabeNoLoteException(larguraProdutoComMargens, comprimentoProdutoComMargens, larguraTotalLoteCm);
            }
        }


        // 2. Determinar a orientação ótima SEM margens
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

        // 3. Definir os parâmetros do layout ótimo
        int produtosPorLinhaOtima = orientacaoOtimaEhRotacionado ? produtosPorLinhaRotacionadoSemMargem : produtosPorLinhaNormalSemMargem;
        BigDecimal larguraProdutoNaOrientacaoOtima = orientacaoOtimaEhRotacionado ? comprimentoProduto : larguraProduto;
        BigDecimal comprimentoProdutoNaOrientacaoOtima = orientacaoOtimaEhRotacionado ? larguraProduto : comprimentoProduto;

        // 4. Calcular as dimensões finais do bloco de produtos e do retalho, aplicando as margens.
        BigDecimal larguraBlocoProdutosFinal;
        if (quantidade < produtosPorLinhaNormalSemMargem) {
            larguraBlocoProdutosFinal = larguraProduto.multiply(new BigDecimal(quantidade));
        } else {
            larguraBlocoProdutosFinal = larguraTotalLoteCm;
        }
        BigDecimal larguraRetalhoLateralFinal = larguraTotalLoteCm.subtract(larguraBlocoProdutosFinal);

        // Se o retalho lateral ficar negativo (margem excede o lote), trunca em 0
        if (larguraRetalhoLateralFinal.compareTo(BigDecimal.ZERO) < 0) {
            larguraRetalhoLateralFinal = BigDecimal.ZERO;
        }

        // 5. Retornar os parâmetros finais para o próximo método.
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

    @Override
    public ParametrosCorte extrairParametrosCorteManual(
            int quantidade,
            Produto produto,
            LoteMateriaPrima lotePrincipal,
            BigDecimal larguraCorteManualCm
    ) {
        ContextoBaseCorte contextoBase = extrairContextoBaseCorte(produto, lotePrincipal);

        // 1. Obter dimensões do lote e produto (sem rotação no manual)
        BigDecimal larguraTotalLoteCm = contextoBase.larguraTotalLoteCm();
        BigDecimal larguraProduto = contextoBase.larguraProduto();
        BigDecimal comprimentoProduto = contextoBase.comprimentoProduto();

        if (larguraCorteManualCm.compareTo(larguraTotalLoteCm) > 0) {
            throw new DimensoesManuaisInvalidasException(larguraCorteManualCm, larguraTotalLoteCm);
        }

        // 2. Estimar produtos por linha baseado na largura manual
        int produtosPorLinha = larguraCorteManualCm.divide(larguraProduto, 0, RoundingMode.FLOOR).intValue();

        // 3. Calcular retalho lateral (R1): diferença entre lote e corte manual
        BigDecimal larguraRetalhoLateralFinal = larguraTotalLoteCm.subtract(larguraCorteManualCm);

        // 4. Retornar parâmetros para modo manual
        return new ParametrosCorte(
                larguraTotalLoteCm,              // largura REAL do lote
                larguraProduto,                   // produto na orientação original
                comprimentoProduto,               // produto na orientação original
                quantidade,                       // quantidade solicitada
                BigDecimal.ZERO,                  // sem margem esquerda
                BigDecimal.ZERO,                  // sem margem direita
                produtosPorLinha,                 // estimativa
                false,                            // não rotaciona no manual
                larguraCorteManualCm,             // largura do corte manual
                larguraRetalhoLateralFinal        // R1 calculado
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
        // Converte o valor de milímetros (mm) para centímetros (cm).
        return new BigDecimal(larguraMmObj.toString()).divide(new BigDecimal("10"), 2, RoundingMode.HALF_UP);
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
