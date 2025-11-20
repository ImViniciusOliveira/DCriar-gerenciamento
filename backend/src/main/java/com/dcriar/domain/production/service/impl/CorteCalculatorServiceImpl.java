package com.dcriar.domain.production.service.impl;

import com.dcriar.api.dto.request.production.MargensRequestDTO;
import com.dcriar.domain.product.entity.Dimensoes;
import com.dcriar.domain.product.entity.Produto;
import com.dcriar.domain.production.model.ParametrosCorte;
import com.dcriar.domain.production.service.CorteCalculatorService;
import com.dcriar.domain.stock.entity.LoteMateriaPrima;
import com.dcriar.exception.custom.AtributoLoteInvalidoException;
import com.dcriar.exception.custom.MargemInvalidaException;
import com.dcriar.exception.custom.ProdutoNaoCabeNoLoteException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
        // 1. Obter a largura total do lote e calcular a largura útil, descontando as margens.
        BigDecimal larguraTotalLoteCm = getLarguraEmCm(lotePrincipal.getAtributos());
        BigDecimal margemEsquerda = Optional.ofNullable(margensRequest != null ? margensRequest.getEsquerda() : null).orElse(BigDecimal.ZERO);
        BigDecimal margemDireita = Optional.ofNullable(margensRequest != null ? margensRequest.getDireita() : null).orElse(BigDecimal.ZERO);
        BigDecimal larguraUtilCm = calcularLarguraUtilCm(larguraTotalLoteCm, margemEsquerda, margemDireita);

        if (larguraUtilCm.compareTo(BigDecimal.ZERO) < 0) {
            throw new MargemInvalidaException("A soma das margens laterais não pode exceder a largura do lote.");
        }

        Dimensoes dimensoesProduto = produto.getDimensoes();
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
