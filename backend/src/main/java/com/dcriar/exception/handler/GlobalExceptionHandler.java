package com.dcriar.exception.handler;

import com.dcriar.api.dto.response.ErrorResponseDTO;
import com.dcriar.exception.custom.*;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Handler de exceções global para toda a aplicação.
 * <p>
 * Anotado com {@link ControllerAdvice}, ele intercepta exceções lançadas pelos controllers
 * e as converte em respostas HTTP padronizadas no formato {@link ErrorResponseDTO}.
 * Isso garante que a API sempre retorne respostas de erro consistentes e estruturadas.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

    //region Exceções de Domínio

    @ExceptionHandler(ValorNumericoExcedeLimiteException.class)
    public ResponseEntity<ErrorResponseDTO> handleValorNumericoExcedeLimite(ValorNumericoExcedeLimiteException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("campo", ex.getNomeDoCampo());
        details.put("valorCalculado", ex.getValorEnviado());
        details.put("limite", ex.getLimiteMaximo());

        logInfoException(ex, details);
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, details);
    }

    /**
     * Trata exceções para entidades ou recursos não encontrados (HTTP 404 Not Found).
     * Intercepta {@link ProdutoNaoEncontradoException}, {@link CanalVendaNaoEncontradoException},
     * {@link LoteMateriaPrimaNaoEncontradoException}, {@link TipoMateriaPrimaNaoEncontradoException},
     * {@link VendaNaoEncontradaException}, {@link ArquivoNaoEncontradoException},
     * {@link CorteRealizadoNaoEncontradoException}, {@link MovimentacaoEstoqueProdutoNaoEncontradoException},
     * {@link OrdemDeProducaoNaoEncontradaException}, {@link PrecoNaoEncontradoException} e {@link EstoqueNaoEncontradoException}.
     *
     * @param ex A exceção de "não encontrado" lançada.
     * @return Um {@link ResponseEntity} contendo um {@link ErrorResponseDTO} com status 404.
     */
    @ExceptionHandler({
            ProdutoNaoEncontradoException.class, CanalVendaNaoEncontradoException.class, LoteMateriaPrimaNaoEncontradoException.class,
            TipoMateriaPrimaNaoEncontradoException.class, VendaNaoEncontradaException.class, ArquivoNaoEncontradoException.class,
            CorteRealizadoNaoEncontradoException.class, MovimentacaoEstoqueProdutoNaoEncontradoException.class,
            OrdemDeProducaoNaoEncontradaException.class, PrecoNaoEncontradoException.class, EstoqueNaoEncontradoException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleNotFoundExceptions(RuntimeException ex) {
        Map<String, String> details = new LinkedHashMap<>();

        if (ex instanceof ProdutoNaoEncontradoException e) { details.put("produtoId", String.valueOf(e.getId())); }
        else if (ex instanceof CanalVendaNaoEncontradoException e) { details.put("canalVendaId", String.valueOf(e.getId())); }
        else if (ex instanceof LoteMateriaPrimaNaoEncontradoException e) { details.put("loteId", String.valueOf(e.getId())); }
        else if (ex instanceof TipoMateriaPrimaNaoEncontradoException e) { details.put("materiaPrimaId", String.valueOf(e.getMateriaPrimaId())); }
        else if (ex instanceof VendaNaoEncontradaException e) { details.put("vendaId", String.valueOf(e.getVendaId())); }
        else if (ex instanceof ArquivoNaoEncontradoException e) {
            if (e.getNomeArquivo() != null) {
                details.put("nomeArquivo", e.getNomeArquivo());
            }
        }
        else if (ex instanceof CorteRealizadoNaoEncontradoException e) { details.put("corteRealizadoId", String.valueOf(e.getId())); }
        else if (ex instanceof MovimentacaoEstoqueProdutoNaoEncontradoException e) { details.put("movimentacaoId", String.valueOf(e.getId())); }
        else if (ex instanceof OrdemDeProducaoNaoEncontradaException e) { details.put("ordemDeProducaoId", String.valueOf(e.getId())); }
        else if (ex instanceof PrecoNaoEncontradoException e) { details.put("precoId", String.valueOf(e.getId())); }
        else if (ex instanceof EstoqueNaoEncontradoException e) {
            details.put("produtoLabel", e.getProdutoLabel());
            details.put("nomeCanalVenda", e.getNomeCanalVenda());
            details.put("produtoId", String.valueOf(e.getProdutoId()));
            details.put("canalVendaId", String.valueOf(e.getCanalVendaId()));
        }

        logInfoException(ex, details);
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, details);
    }

    /**
     * Trata exceções de domínio com resposta HTTP 400 (Bad Request).
     * Intercepta {@link PrecoComercialNaoDefinidoException},
     * {@link TipoPrecoAplicadoInvalidoException},
     * {@link AtributoLoteInvalidoException}, {@link CalculoCustoIncompativelException},
     * {@link DimensoesManuaisInvalidasException}, {@link MargemInvalidaException},
     * {@link LotePrincipalNaoEspecificadoException},
     * {@link ProdutoNaoCabeNoLoteException}, {@link QuantidadeUnidadesInvalidaException},
     * {@link TipoProducaoIncompativelException}, {@link ImpossivelExcluirProducaoException},
     * {@link TipoProdutoInvalidoException}, {@link OperadorEstoqueInvalidoException}.
     *
     * @param ex A exceção de domínio ou parâmetro inválido lançada.
     * @return Um {@link ResponseEntity} contendo um {@link ErrorResponseDTO} com status 400.
     */
    @ExceptionHandler({
            PrecoComercialNaoDefinidoException.class,
            TipoPrecoAplicadoInvalidoException.class,
            PrecoVendaInvalidoException.class,
            AjusteLoteInvalidoException.class,
            AtributoLoteInvalidoException.class, CalculoCustoIncompativelException.class, DimensoesManuaisInvalidasException.class,
            MargemInvalidaException.class, LotePrincipalNaoEspecificadoException.class,
            ProdutoNaoCabeNoLoteException.class, QuantidadeUnidadesInvalidaException.class, TipoProducaoIncompativelException.class,
            ImpossivelExcluirProducaoException.class,
            TipoProdutoInvalidoException.class, OperadorEstoqueInvalidoException.class,
            IncompatibilidadeMaterialException.class, QuantidadeExcedeCapacidadeLoteException.class,
            UnidadeCadastroConsumoInvalidaException.class, UnidadeEstoqueLoteInvalidaException.class,
            UnidadeEstoqueCorteInvalidaException.class, LogicalMapKeyInvalidaException.class,
            OperacaoNaoSuportadaException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleBusinessRuleExceptions(RuntimeException ex) {
        Map<String, String> details = new LinkedHashMap<>();

        // Exceptions de parâmetros de busca
        switch (ex) {
            case TipoProdutoInvalidoException e -> {
                details.put("tipoProdutoFornecido", e.getTipoProdutoFornecido());
                details.put("tiposValidos", "CORTE, CONSUMO");
            }
            case OperadorEstoqueInvalidoException e -> {
                details.put("operadorFornecido", e.getOperadorFornecido());
                details.put("operadoresValidos", "GTE (≥), LTE (≤)");
            }
            case PrecoComercialNaoDefinidoException e -> {
                details.put("codigo", "PRECO_COMERCIAL_NAO_DEFINIDO");
                details.put("produtoId", String.valueOf(e.getProdutoId()));
            }
            case TipoPrecoAplicadoInvalidoException e -> {
                details.put("codigo", "TIPO_PRECO_APLICADO_INVALIDO");
                details.put("valorInformado", e.getValorInformado());
                details.put("valoresAceitos", "PRECO_PADRAO, PRECO_ALTERADO, DESCONTO_TOTAL");
            }
            case PrecoVendaInvalidoException e -> {
                details.put("codigo", e.getCodigo());
                details.put("precoComercialOriginal", String.valueOf(e.getPrecoComercialOriginal()));
                details.put("precoAplicado", String.valueOf(e.getPrecoAplicado()));
            }
            case AjusteLoteInvalidoException e -> details.put("campo", e.getDetalhe());
            case AtributoLoteInvalidoException e -> {
                details.put("codigo", e.getCodigo());
                details.put("campo", e.getCampo());
                if (e.getUnidadeDescricao() != null) {
                    details.put("unidadeDescricao", e.getUnidadeDescricao());
                }
            }
            case CalculoCustoIncompativelException e -> {
                details.put("codigo", "CALCULO_CUSTO_INCOMPATIVEL");
                details.put("unidadeEstoque", e.getUnidadeEstoque().name());
                details.put("unidadeConsumo", e.getUnidadeConsumo().name());
            }
            case DimensoesManuaisInvalidasException e -> {
                details.put("codigo", e.getCodigo());
                if (e.getLarguraCorteManual() != null) {
                    details.put("larguraCorteManual", String.valueOf(e.getLarguraCorteManual()));
                }
                if (e.getLarguraLote() != null) {
                    details.put("larguraLote", String.valueOf(e.getLarguraLote()));
                }
                if (e.getComprimentoCorteManual() != null) {
                    details.put("comprimentoCorteManual", String.valueOf(e.getComprimentoCorteManual()));
                }
                if (e.getComprimentoLote() != null) {
                    details.put("comprimentoLote", String.valueOf(e.getComprimentoLote()));
                }
            }
            case MargemInvalidaException e -> {
                details.put("codigo", e.getCodigo());
                if (e.getLarguraFinal() != null) {
                    details.put("larguraFinal", String.valueOf(e.getLarguraFinal()));
                }
                if (e.getLarguraProdutos() != null) {
                    details.put("larguraProdutos", String.valueOf(e.getLarguraProdutos()));
                }
                if (e.getSomaMargens() != null) {
                    details.put("somaMargens", String.valueOf(e.getSomaMargens()));
                }
                if (e.getMargemEsquerda() != null) {
                    details.put("margemEsquerda", String.valueOf(e.getMargemEsquerda()));
                }
                if (e.getMargemDireita() != null) {
                    details.put("margemDireita", String.valueOf(e.getMargemDireita()));
                }
                if (e.getLarguraLote() != null) {
                    details.put("larguraLote", String.valueOf(e.getLarguraLote()));
                }
                if (e.getComprimentoFinal() != null) {
                    details.put("comprimentoFinal", String.valueOf(e.getComprimentoFinal()));
                }
            }
            case LotePrincipalNaoEspecificadoException ignored -> {
                details.put("codigo", "LOTE_PRINCIPAL_NAO_ESPECIFICADO");
                details.put("campo", "loteId");
                details.put("operacao", "PRODUCAO_POR_CORTE");
            }
            case ProdutoNaoCabeNoLoteException ignored -> {
                details.put("codigo", "PRODUTO_NAO_CABE_NO_LOTE");
                details.put("motivoBloqueio", ex.getMessage());
            }
            case QuantidadeUnidadesInvalidaException e -> {
                details.put("codigo", "QUANTIDADE_UNIDADES_INVALIDA");
                if (e.getTotalUnidadesBase() != null) {
                    details.put("totalUnidadesBase", e.getTotalUnidadesBase().stripTrailingZeros().toPlainString());
                }
            }
            case TipoProducaoIncompativelException e -> {
                details.put("codigo", e.getCodigo());
                if (e.getNomeProduto() != null) {
                    details.put("nomeProduto", e.getNomeProduto());
                }
                if (e.getUnidadeDeConsumo() != null) {
                    details.put("unidadeDeConsumo", e.getUnidadeDeConsumo());
                }
                if (e.getNomeMateriaPrimaProduto() != null) {
                    details.put("nomeMateriaPrimaProduto", e.getNomeMateriaPrimaProduto());
                }
                if (e.getLoteId() != null) {
                    details.put("loteId", String.valueOf(e.getLoteId()));
                }
                if (e.getNomeMateriaPrimaLote() != null) {
                    details.put("nomeMateriaPrimaLote", e.getNomeMateriaPrimaLote());
                }
            }
            case ImpossivelExcluirProducaoException ignored -> {
                details.put("codigo", "IMPOSSIVEL_EXCLUIR_PRODUCAO");
                details.put("motivoBloqueio", ex.getMessage());
            }
            case IncompatibilidadeMaterialException e -> {
                details.put("codigo", "INCOMPATIBILIDADE_MATERIAL");
                details.put("nomeMateriaPrimaProduto", e.getNomeMateriaPrimaProduto());
                details.put("nomeMateriaPrimaLote", e.getNomeMateriaPrimaLote());
            }
            case QuantidadeExcedeCapacidadeLoteException e -> {
                details.put("codigo", "QUANTIDADE_EXCEDE_CAPACIDADE_LOTE");
                details.put("quantidadeSolicitada", String.valueOf(e.getQuantidadeSolicitada()));
                details.put("comprimentoNecessarioNormalCm", String.valueOf(e.getComprimentoNecessarioNormalCm()));
                details.put("comprimentoNecessarioRotacionadoCm", String.valueOf(e.getComprimentoNecessarioRotacionadoCm()));
                details.put("comprimentoDisponivelLoteCm", String.valueOf(e.getComprimentoDisponivelLoteCm()));
            }
            case UnidadeCadastroConsumoInvalidaException e -> {
                details.put("codigo", "UNIDADE_CADASTRO_CONSUMO_INVALIDA");
                details.put("unidadeMateriaPrima", e.getUnidadeMateriaPrima().name());
                details.put("unidadeInformada", e.getUnidadeInformada().name());
                if (e.getUnidadeMenorCompativel() != null) {
                    details.put("unidadeMenorCompativel", e.getUnidadeMenorCompativel().name());
                }
            }
            case UnidadeEstoqueLoteInvalidaException e -> {
                details.put("codigo", "UNIDADE_ESTOQUE_LOTE_INVALIDA");
                details.put("unidadeMateriaPrima", e.getUnidadeMateriaPrima().name());
                details.put("unidadeEstoqueInformada", e.getUnidadeEstoqueInformada().name());
                if (e.getUnidadeMenorCompativel() != null) {
                    details.put("unidadeMenorCompativel", e.getUnidadeMenorCompativel().name());
                }
            }
            case UnidadeEstoqueCorteInvalidaException e -> {
                details.put("codigo", "UNIDADE_ESTOQUE_CORTE_INVALIDA");
                details.put("unidadeDeEstoque", e.getUnidadeDeEstoque().name());
            }
            case LogicalMapKeyInvalidaException e -> {
                details.put("campo", e.getFieldPath());
                details.put("codigo", e.getCodigo());
                if (e.getPrimeiraChave() != null) {
                    details.put("primeiraChave", e.getPrimeiraChave());
                }
                if (e.getSegundaChave() != null) {
                    details.put("segundaChave", e.getSegundaChave());
                }
            }
            case OperacaoNaoSuportadaException e -> {
                details.put("recurso", e.getRecurso());
                details.put("operacao", e.getOperacao());
                details.put("alternativaSugerida", e.getAlternativaSugerida());
            }
            default -> {
            }
        }

        logInfoException(ex, details);
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, details);
    }

    /**
     * Trata exceções de conflito, como criação de recurso duplicado ou recurso em uso (HTTP 409 Conflict).
     * Intercepta {@link TipoMateriaPrimaJaExisteException}, {@link ProdutoEmUsoException},
     * {@link TipoMateriaPrimaEmUsoException}, {@link ProdutoNomeDuplicadoException},
     * {@link ProdutoSkuDuplicadoException}, {@link ExclusaoLoteBloqueadaException},
     * {@link ProdutoCamposBloqueadosException}, {@link TipoMateriaPrimaCamposBloqueadosException},
     * {@link CanalVendaEmUsoException}, {@link CanalVendaNomeDuplicadoException}
     * e {@link LoteCamposBloqueadosException}.
     *
     * @param ex A exceção de conflito lançada.
     * @return Um {@link ResponseEntity} contendo um {@link ErrorResponseDTO} com status 409.
     */
    @ExceptionHandler({
            TipoMateriaPrimaJaExisteException.class, ProdutoEmUsoException.class,
            TipoMateriaPrimaEmUsoException.class, ProdutoNomeDuplicadoException.class,
            ProdutoSkuDuplicadoException.class, ExclusaoLoteBloqueadaException.class,
            ProdutoCamposBloqueadosException.class, TipoMateriaPrimaCamposBloqueadosException.class,
            LoteCamposBloqueadosException.class, CanalVendaEmUsoException.class,
            CanalVendaNomeDuplicadoException.class, AtualizacaoSemAlteracoesException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleConflictExceptions(RuntimeException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        if (ex instanceof TipoMateriaPrimaJaExisteException e) { details.put("nome", e.getNome()); }
        else if (ex instanceof ProdutoEmUsoException e) {
            details.put("nomeProduto", e.getNomeProduto());
            details.put("entidadesEmUsoLabels", formatarColecao(e.getEntidadeLabels()));
            details.put("produtoId", String.valueOf(e.getProdutoId()));
            details.put("entidadesEmUso", formatarColecao(e.getEntidadeIds()));
        }
        else if (ex instanceof TipoMateriaPrimaEmUsoException e) {
            details.put("nomeTipoMateriaPrima", e.getNomeTipoMateriaPrima());
            details.put("produtosEmUsoLabels", formatarColecao(e.getProdutoLabels()));
            details.put("lotesEmUsoLabels", formatarColecao(e.getLoteLabels()));
            details.put("tipoMateriaPrimaId", String.valueOf(e.getTipoMateriaPrimaId()));
            details.put("produtosEmUso", formatarColecao(e.getProdutoIds()));
            details.put("lotesEmUso", formatarColecao(e.getLoteIds()));
        }
        else if (ex instanceof CanalVendaNomeDuplicadoException e) { details.put("nome", e.getNome()); }
        else if (ex instanceof CanalVendaEmUsoException e) {
            details.put("nomeCanalVenda", e.getNomeCanalVenda());
            details.put("estoquesEmUsoLabels", formatarColecao(e.getEstoqueLabels()));
            details.put("vendasEmUsoLabels", formatarColecao(e.getVendaLabels()));
            details.put("ordensDeProducaoEmUsoLabels", formatarColecao(e.getOrdemDeProducaoLabels()));
            details.put("canalVendaId", String.valueOf(e.getCanalVendaId()));
            details.put("estoquesEmUso", formatarColecao(e.getEstoqueIds()));
            details.put("vendasEmUso", formatarColecao(e.getVendaIds()));
            details.put("ordensDeProducaoEmUso", formatarColecao(e.getOrdemDeProducaoIds()));
        }
        else if (ex instanceof ProdutoNomeDuplicadoException e) { details.put("nome", e.getNome()); }
        else if (ex instanceof ProdutoSkuDuplicadoException e) { details.put("sku", e.getSku()); }
        else if (ex instanceof ExclusaoLoteBloqueadaException e) {
            details.put("codigoBloqueio", e.getCodigoBloqueio());
            details.put("identificadorPublico", e.getIdentificadorPublicoRaiz());
            details.put("quantidadeItensBloqueados", String.valueOf(e.getItensBloqueados().size()));
            details.put("itensBloqueadosLabels", e.formatarIdentificadoresBloqueados());
            for (int i = 0; i < e.getItensBloqueados().size(); i++) {
                ExclusaoLoteBloqueadaException.ItemBloqueioLote item = e.getItensBloqueados().get(i);
                String prefixo = "itemBloqueado." + (i + 1);
                details.put(prefixo + ".identificadorPublico", item.identificadorPublicoLote());
                details.put(prefixo + ".cadeia", item.formatarCadeiaRetalhos());
                details.put(prefixo + ".ordensRelacionadas", item.formatarOrdensRelacionadas());
                details.put(prefixo + ".motivo", item.formatarMotivo());
                details.put(prefixo + ".loteId", String.valueOf(item.loteId()));
            }
            details.put("loteId", String.valueOf(e.getLoteRaizId()));
        }
        else if (ex instanceof ProdutoCamposBloqueadosException e) {
            details.put("nomeProduto", e.getNomeProduto());
            preencherDetalhesCamposBloqueados(details, "produtoId", e.getProdutoId(), e);
        }
        else if (ex instanceof TipoMateriaPrimaCamposBloqueadosException e) {
            details.put("nomeTipoMateriaPrima", e.getNomeTipoMateriaPrima());
            preencherDetalhesCamposBloqueados(details, "tipoMateriaPrimaId", e.getTipoMateriaPrimaId(), e);
        }
        else if (ex instanceof LoteCamposBloqueadosException e) {
            details.put("identificadorPublico", e.getIdentificadorPublico());
            preencherDetalhesCamposBloqueados(details, "loteId", e.getLoteId(), e);
        }
        else if (ex instanceof AtualizacaoSemAlteracoesException e) {
            details.put("recurso", e.getRecurso());
            details.put("recursoId", String.valueOf(e.getRecursoId()));
        }

        logInfoException(ex, details);
        return buildErrorResponse(ex, HttpStatus.CONFLICT, details);
    }

    private void preencherDetalhesCamposBloqueados(
            Map<String, String> details,
            String idKey,
            Long idValue,
            AbstractCamposBloqueadosException ex
    ) {
        details.put("camposBloqueados", formatarColecao(ex.getCamposBloqueados()));
        ex.getMotivosBloqueio().forEach((campo, motivo) -> details.put("motivo." + campo, motivo));
        details.put(idKey, String.valueOf(idValue));
    }

    private String formatarColecao(Iterable<?> itens) {
        return java.util.stream.StreamSupport.stream(itens.spliterator(), false)
                .filter(Objects::nonNull)
                .sorted((left, right) -> {
                    if (left instanceof Comparable<?> && right instanceof Comparable<?> && left.getClass().equals(right.getClass())) {
                        @SuppressWarnings("unchecked")
                        Comparable<Object> comparable = (Comparable<Object>) left;
                        return comparable.compareTo(right);
                    }
                    return String.valueOf(left).compareTo(String.valueOf(right));
                })
                .map(String::valueOf)
                .collect(Collectors.joining(", "));
    }

    /**
     * Trata exceções de validação de negócio com múltiplos campos (HTTP 400 Bad Request).
     * Intercepta {@link ProdutoInvalidoException}.
     *
     * @param ex A exceção {@link ProdutoInvalidoException} lançada.
     * @return Um {@link ResponseEntity} contendo um {@link ErrorResponseDTO} com status 400 e detalhes dos erros.
     */
    @ExceptionHandler(ProdutoInvalidoException.class)
    public ResponseEntity<ErrorResponseDTO> handleMultiFieldValidation(ProdutoInvalidoException ex) {
        logInfoException(ex, ex.getErrors());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, ex.getErrors());
    }

    /**
     * Trata exceções de estoque insuficiente para uma operação (HTTP 400 Bad Request).
     * Intercepta {@link EstoqueInsuficienteParaMovimentacaoException}, {@link EstoqueInsuficienteCanalException},
     * {@link AlocacaoEstoqueExcedeTotalException} e {@link SaldoMateriaPrimaInsuficienteException}.
     *
     * @param ex A exceção de estoque insuficiente lançada.
     * @return Um {@link ResponseEntity} contendo um {@link ErrorResponseDTO} com status 400 e detalhes do estoque.
     */
    @ExceptionHandler({
            EstoqueInsuficienteParaMovimentacaoException.class, EstoqueInsuficienteCanalException.class,
            AlocacaoEstoqueExcedeTotalException.class, SaldoMateriaPrimaInsuficienteException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleInsufficientStock(RuntimeException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        if (ex instanceof EstoqueInsuficienteParaMovimentacaoException e) {
            details.put("identificadorPublicoLote", e.getIdentificadorPublicoLote());
            details.put("nomeTipoMateriaPrima", e.getNomeTipoMateriaPrima());
            details.put("quantidadeRequisitada", String.valueOf(Math.abs(e.getQuantidadeRequisitada())));
            details.put("saldoDisponivel", String.valueOf(e.getSaldoDisponivel()));
            details.put("loteId", String.valueOf(e.getLoteId()));
        } else if (ex instanceof EstoqueInsuficienteCanalException e) {
            details.put("produtoLabel", e.getProdutoLabel());
            details.put("nomeCanalVenda", e.getNomeCanalVenda());
            details.put("quantidadeRequisitada", String.valueOf(Math.abs(e.getQuantidadeRequisitada())));
            details.put("estoqueAtual", String.valueOf(e.getEstoqueAtual()));
            details.put("produtoId", String.valueOf(e.getProdutoId()));
            details.put("canalVendaId", String.valueOf(e.getCanalVendaId()));
        } else if (ex instanceof AlocacaoEstoqueExcedeTotalException e) {
            details.put("produtoLabel", e.getProdutoLabel());
            details.put("nomeCanalVenda", e.getNomeCanalVenda());
            details.put("quantidadeParaAlocar", String.valueOf(e.getQuantidadeParaAlocar()));
            details.put("novoTotalDistribuido", String.valueOf(e.getNovoTotalDistribuido()));
            details.put("estoqueFisicoTotal", String.valueOf(e.getEstoqueFisicoTotal()));
            details.put("produtoId", String.valueOf(e.getProdutoId()));
            details.put("canalVendaId", String.valueOf(e.getCanalVendaId()));
        } else if (ex instanceof SaldoMateriaPrimaInsuficienteException e) {
            details.put("identificadorLote", e.getIdentificadorLote());
            details.put("nomeTipoMateriaPrima", e.getNomeTipoMateriaPrima());
            details.put("quantidadeRequisitada", String.valueOf(e.getQuantidadeRequisitada()));
            details.put("saldoDisponivel", String.valueOf(e.getSaldoDisponivel()));
        }
        logInfoException(ex, details);
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, details);
    }

    //endregion

    //region Exceções do Spring Framework

    /**
     * Trata erros de validação de argumentos de método ({@code @Valid}) (HTTP 400 Bad Request).
     * Converte os erros de validação do Spring em um mapa de detalhes para o {@link ErrorResponseDTO}.
     *
     * @param ex A exceção {@link MethodArgumentNotValidException} lançada.
     * @return Um {@link ResponseEntity} contendo um {@link ErrorResponseDTO} com status 400 e detalhes dos erros de campo.
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidationErrors(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        log.info(
                "Erros de validação de argumento de método: method={} uri={} errors={}",
                request.getMethod(),
                request.getRequestURI(),
                errors
        );
        return buildErrorResponse(buildValidationMessage(errors), HttpStatus.BAD_REQUEST, errors);
    }

    /**
     * Trata erros de desserialização de JSON ou requisições com corpo ilegível (HTTP 400 Bad Request).
     * Fornece detalhes sobre o campo que causou o erro de formato, se disponível.
     *
     * @param ex A exceção {@link HttpMessageNotReadableException} lançada.
     * @return Um {@link ResponseEntity} contendo um {@link ErrorResponseDTO} com status 400.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleMalformedJson(HttpMessageNotReadableException ex, HttpServletRequest request) {
        String message;
        Map<String, String> details = new LinkedHashMap<>();

        if (ex.getMostSpecificCause() instanceof InvalidFormatException invalidFormat) {
            String campo = invalidFormat.getPath().stream().map(JsonMappingException.Reference::getFieldName).collect(Collectors.joining("."));
            String valorInformado = String.valueOf(invalidFormat.getValue());
            message = String.format(
                    "O campo '%s' recebeu um valor em formato inválido: '%s'.",
                    campo,
                    valorInformado
            );
            details.put("campo", campo);
            details.put("valorInformado", valorInformado);
        } else {
            message = "O corpo da requisição está malformado ou contém dados inválidos.";
            details.put("causa", simplifyJsonCause(ex.getMostSpecificCause().getMessage()));
        }

        log.info(
                "JSON inválido: method={} uri={} details={}",
                request.getMethod(),
                request.getRequestURI(),
                sanitizeLogMap(details)
        );
        return buildErrorResponse(message, HttpStatus.BAD_REQUEST, details);
    }

    /**
     * Trata parâmetros de requisição obrigatórios ausentes do Spring (HTTP 400 Bad Request).
     * Para outros parâmetros, retorna erro genérico.
     *
     * @param ex A exceção do Spring lançada.
     * @return Um {@link ResponseEntity} contendo um {@link ErrorResponseDTO} com status 400.
     */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingRequestParameter(
            org.springframework.web.bind.MissingServletRequestParameterException ex,
            HttpServletRequest request
    ) {
        String parameterName = ex.getParameterName();

        Map<String, String> details = new LinkedHashMap<>();
        details.put("parametro", parameterName);
        details.put("orientacao", "Informe o parâmetro obrigatório e tente novamente.");

        log.info(
                "Parâmetro obrigatório ausente: method={} uri={} parametro={}",
                request != null ? request.getMethod() : "N/A",
                request != null ? request.getRequestURI() : "N/A",
                parameterName
        );
        return buildErrorResponse(
                String.format("O parâmetro obrigatório '%s' não foi informado na requisição.", parameterName),
                HttpStatus.BAD_REQUEST,
                details
        );
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodArgumentTypeMismatch(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request
    ) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("parametro", ex.getName());
        details.put("valorInformado", String.valueOf(ex.getValue()));

        String message = String.format(
                "O parâmetro '%s' recebeu um valor inválido: '%s'.",
                ex.getName(),
                ex.getValue()
        );

        Class<?> requiredType = ex.getRequiredType();
        if (requiredType != null && requiredType.isEnum()) {
            Object[] enumConstants = requiredType.getEnumConstants();
            if (enumConstants != null && enumConstants.length > 0) {
                String valoresAceitos = java.util.Arrays.stream(enumConstants)
                        .map(Object::toString)
                        .collect(Collectors.joining(", "));
                details.put("valoresAceitos", valoresAceitos);
            }
        }

        log.info(
                "Parâmetro com tipo inválido: method={} uri={} details={}",
                request != null ? request.getMethod() : "N/A",
                request != null ? request.getRequestURI() : "N/A",
                sanitizeLogMap(details)
        );
        return buildErrorResponse(message, HttpStatus.BAD_REQUEST, details);
    }

    /**
     * Trata o uso de métodos HTTP não suportados por um endpoint (HTTP 405 Method Not Allowed).
     * Informa quais métodos HTTP são permitidos para o recurso.
     * Agora também loga a URI da requisição para facilitar diagnóstico de clientes que fazem GET por engano.
     *
     * @param ex A exceção {@link HttpRequestMethodNotSupportedException} lançada.
     * @return Um {@link ResponseEntity} contendo um {@link ErrorResponseDTO} com status 405.
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotAllowed(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        String msg = String.format("Método HTTP '%s' não permitido para este recurso.", ex.getMethod());
        String metodosPermitidos = Objects.requireNonNull(ex.getSupportedHttpMethods()).stream().map(HttpMethod::name).collect(Collectors.joining(", "));
        // Log mais informativo: método + URI + métodos permitidos
        log.info("Método HTTP não permitido: method={} uri={} permitted={}", ex.getMethod(), request.getRequestURI(), metodosPermitidos);
        return buildErrorResponse(msg, HttpStatus.METHOD_NOT_ALLOWED, Map.of("metodosPermitidos", metodosPermitidos));
    }

    @ExceptionHandler(DadosSensiveisCriptografiaException.class)
    public ResponseEntity<ErrorResponseDTO> handleSensitiveDataEncryption(
            DadosSensiveisCriptografiaException ex,
            HttpServletRequest request
    ) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("codigo", ex.getCodigo());

        if ("CHAVE_CRIPTOGRAFIA_NAO_CONFIGURADA".equals(ex.getCodigo())
                || "CHAVE_CRIPTOGRAFIA_INVALIDA".equals(ex.getCodigo())) {
            details.put("orientacao", "Revise a configuração de DATA_ENCRYPTION_KEY antes de iniciar a aplicação.");
        }

        log.error(
                "Falha de criptografia de dados sensíveis: method={} uri={} codigo={} message={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getCodigo(),
                ex.getMessage(),
                ex
        );
        return buildErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, details);
    }

    /**
     * Trata exceções internas controladas de armazenamento de arquivos (HTTP 500 Internal Server Error).
     *
     * @param ex A exceção interna do servidor lançada.
     * @return Um {@link ResponseEntity} contendo um {@link ErrorResponseDTO} com status 500.
     */
    @ExceptionHandler(ArquivoStorageException.class)
    public ResponseEntity<ErrorResponseDTO> handleFileStorageException(ArquivoStorageException ex, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("operacao", ex.getOperacao());
        details.put("nomeArquivo", ex.getNomeArquivo());

        log.error(
                "Erro interno controlado: method={} uri={} type={} details={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                sanitizeLogMap(details),
                ex
        );
        return buildErrorResponse(ex.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR, details);
    }

    /**
     * Manipula erros de integridade (ex: tentar criar produto com nome duplicado)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDatabaseErrors(DataIntegrityViolationException ex, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        details.put("causa", "VIOLACAO_DE_INTEGRIDADE");
        details.put("orientacao", "Verifique se o registro já existe ou se ainda está vinculado a outros dados.");

        log.warn(
                "Conflito de integridade no banco: method={} uri={} rootCause={}",
                request.getMethod(),
                request.getRequestURI(),
                sanitizeLogValue(resolveRootCauseMessage(ex))
        );
        return buildErrorResponse(
                "A operação violou uma regra de integridade dos dados. Verifique se o registro já existe ou se ainda possui vínculos ativos.",
                HttpStatus.CONFLICT,
                details
        );
    }

    /**
     * Manipula uploads maiores que o permitido
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleMaxSizeException(MaxUploadSizeExceededException ex, HttpServletRequest request) {
        Map<String, String> details = new LinkedHashMap<>();
        Long limiteBytes = resolveUploadLimitBytes(ex);
        Long tamanhoRecebidoBytes = resolveUploadReceivedBytes(ex);
        if (limiteBytes != null) {
            details.put("limiteBytes", String.valueOf(limiteBytes));
        }
        if (tamanhoRecebidoBytes != null) {
            details.put("tamanhoRecebidoBytes", String.valueOf(tamanhoRecebidoBytes));
        }
        details.put("orientacao", "Envie um arquivo menor e tente novamente.");

        log.info(
                "Upload excedeu o tamanho máximo: method={} uri={} limiteBytes={}",
                request.getMethod(),
                request.getRequestURI(),
                limiteBytes != null ? limiteBytes : ex.getMaxUploadSize()
        );
        return buildErrorResponse(
                "O arquivo enviado excede o tamanho máximo permitido.",
                HttpStatus.EXPECTATION_FAILED,
                details
        );
    }

    /**
     * Handler genérico para qualquer outra exceção não tratada (HTTP 500 Internal Server Error).
     * Registra a exceção e retorna uma mensagem de erro genérica para o cliente.
     *
     * @param ex A exceção genérica capturada.
     * @return Um {@link ResponseEntity} contendo um {@link ErrorResponseDTO} com status 500.
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error(
                "Erro inesperado: method={} uri={} type={}",
                request.getMethod(),
                request.getRequestURI(),
                ex.getClass().getSimpleName(),
                ex
        );
        String msg = "Ocorreu um erro interno inesperado. Tente novamente mais tarde.";
        return buildErrorResponse(msg, HttpStatus.INTERNAL_SERVER_ERROR, Map.of("exception", ex.getClass().getSimpleName()));
    }

    /**
     * Trata especificamente a exceção lançada quando o ResourceHttpRequestHandler não encontra
     * um recurso estático solicitado (por exemplo, /actuator/health sendo interpretado como recurso estático).
     * Retorna 404 Not Found em vez de 500 para que clientes recebam a resposta correta.
     */
    @ExceptionHandler(org.springframework.web.servlet.resource.NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResourceFound(org.springframework.web.servlet.resource.NoResourceFoundException ex) {
        log.debug("Recurso estático não encontrado: {}", ex.getMessage());
        // NoResourceFoundException não expõe getRequestPath em todas as versões; usamos a mensagem como detalhe.
        return buildErrorResponse("Recurso não encontrado", HttpStatus.NOT_FOUND, Map.of("detail", ex.getMessage()));
    }

    //endregion

    //region Métodos Auxiliares

    /**
     * Constrói uma resposta de erro padronizada a partir de uma mensagem e detalhes específicos.
     *
     * @param message A mensagem principal do erro.
     * @param status O status HTTP a ser retornado.
     * @param details Um mapa de detalhes adicionais do erro.
     * @return Um {@link ResponseEntity} contendo o {@link ErrorResponseDTO} e o status HTTP.
     */
    private ResponseEntity<ErrorResponseDTO> buildErrorResponse(String message, HttpStatus status, Map<String, String> details) {
        ErrorResponseDTO dto = new ErrorResponseDTO(Instant.now(), status.value(), status.getReasonPhrase(), message, details);
        return new ResponseEntity<>(dto, status);
    }

    /**
     * Constrói uma resposta de erro padronizada a partir de uma exceção e detalhes específicos.
     * Utiliza a mensagem da exceção como mensagem principal do erro.
     *
     * @param ex A exceção que foi capturada.
     * @param status O status HTTP a ser retornado.
     * @param details Um mapa de detalhes adicionais do erro.
     * @return Um {@link ResponseEntity} contendo o {@link ErrorResponseDTO} e o status HTTP.
     */
    private ResponseEntity<ErrorResponseDTO> buildErrorResponse(Exception ex, HttpStatus status, Map<String, String> details) {
        return buildErrorResponse(ex.getMessage(), status, details);
    }

    private String buildValidationMessage(Map<String, String> errors) {
        if (errors.isEmpty()) {
            return "Há campos inválidos na requisição. Revise os dados informados.";
        }

        if (errors.size() == 1) {
            return errors.values().iterator().next();
        }

        return String.format(
                "Há %d campos inválidos na requisição. Revise os detalhes informados.",
                errors.size()
        );
    }

    private Map<String, String> sanitizeLogMap(Map<String, String> details) {
        Map<String, String> sanitized = new LinkedHashMap<>();
        details.forEach((key, value) -> sanitized.put(key, sanitizeLogValue(value)));
        return sanitized;
    }

    private String sanitizeLogValue(String value) {
        if (value == null) {
            return null;
        }

        return value
                .replace(System.lineSeparator(), " ")
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void logInfoException(Exception ex, Map<String, String> details) {
        logExceptionWithDetails(ex, details);
    }

    private void logExceptionWithDetails(Exception ex, Map<String, String> details) {
        log.info("{}: {}. Detalhes: {}", ex.getClass().getSimpleName(), ex.getMessage(), details);
    }

    private String simplifyJsonCause(String cause) {
        String sanitized = sanitizeLogValue(cause);

        if (sanitized == null || sanitized.isBlank()) {
            return "JSON malformado.";
        }

        if (sanitized.startsWith("Unexpected end-of-input")) {
            return "O JSON foi encerrado antes do final esperado.";
        }

        return sanitized;
    }

    private String resolveRootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        while (current.getCause() != null) {
            current = current.getCause();
        }
        return current.getMessage();
    }

    private Long resolveUploadLimitBytes(MaxUploadSizeExceededException ex) {
        if (ex.getMaxUploadSize() > 0) {
            return ex.getMaxUploadSize();
        }

        String rootCause = sanitizeLogValue(resolveRootCauseMessage(ex));
        if (rootCause == null) {
            return null;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("configured maximum \\((\\d+)\\)")
                .matcher(rootCause);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }

    private Long resolveUploadReceivedBytes(MaxUploadSizeExceededException ex) {
        String rootCause = sanitizeLogValue(resolveRootCauseMessage(ex));
        if (rootCause == null) {
            return null;
        }

        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("size \\((\\d+)\\)")
                .matcher(rootCause);
        return matcher.find() ? Long.parseLong(matcher.group(1)) : null;
    }

    //endregion
}
