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

import java.time.Instant;
import java.util.HashMap;
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
        Map<String, String> details = new HashMap<>();
        details.put("campo", ex.getNomeDoCampo());
        details.put("valorCalculado", ex.getValorEnviado());
        details.put("limite", ex.getLimiteMaximo());

        log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
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
        Map<String, String> details = new HashMap<>();

        if (ex instanceof ProdutoNaoEncontradoException e) { details.put("produtoId", String.valueOf(e.getId())); }
        else if (ex instanceof CanalVendaNaoEncontradoException e) { details.put("canalVendaId", String.valueOf(e.getId())); }
        else if (ex instanceof LoteMateriaPrimaNaoEncontradoException e) { details.put("loteId", String.valueOf(e.getId())); }
        else if (ex instanceof TipoMateriaPrimaNaoEncontradoException e) { details.put("materiaPrimaId", String.valueOf(e.getMateriaPrimaId())); }
        else if (ex instanceof VendaNaoEncontradaException e) { details.put("vendaId", String.valueOf(e.getVendaId())); }
        else if (ex instanceof ArquivoNaoEncontradoException) { details.put("info", ex.getMessage()); }
        else if (ex instanceof CorteRealizadoNaoEncontradoException e) { details.put("corteRealizadoId", String.valueOf(e.getId())); }
        else if (ex instanceof MovimentacaoEstoqueProdutoNaoEncontradoException e) { details.put("movimentacaoId", String.valueOf(e.getId())); }
        else if (ex instanceof OrdemDeProducaoNaoEncontradaException e) { details.put("ordemDeProducaoId", String.valueOf(e.getId())); }
        else if (ex instanceof PrecoNaoEncontradoException e) { details.put("precoId", String.valueOf(e.getId())); }
        else if (ex instanceof EstoqueNaoEncontradoException e) { details.put("produtoId", String.valueOf(e.getProdutoId())); details.put("canalVendaId", String.valueOf(e.getCanalVendaId())); }

        log.warn("{}: {}. Detalhes: {}", ex.getClass().getSimpleName(), ex.getMessage(), details);
        return buildErrorResponse(ex, HttpStatus.NOT_FOUND, details);
    }

    /**
     * Trata exceções de domínio com resposta HTTP 400 (Bad Request).
     * Intercepta {@link PrecoVarejoNaoDefinidoException},
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
            PrecoVarejoNaoDefinidoException.class,
            AtributoLoteInvalidoException.class, CalculoCustoIncompativelException.class, DimensoesManuaisInvalidasException.class,
            MargemInvalidaException.class, LotePrincipalNaoEspecificadoException.class,
            ProdutoNaoCabeNoLoteException.class, QuantidadeUnidadesInvalidaException.class, TipoProducaoIncompativelException.class,
            ImpossivelExcluirProducaoException.class,
            TipoProdutoInvalidoException.class, OperadorEstoqueInvalidoException.class,
            IncompatibilidadeMaterialException.class, QuantidadeExcedeCapacidadeLoteException.class,
            UnidadeCadastroConsumoInvalidaException.class, UnidadeEstoqueLoteInvalidaException.class,
            UnidadeEstoqueCorteInvalidaException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleBusinessRuleExceptions(RuntimeException ex) {
        Map<String, String> details = new HashMap<>();

        // Exceptions de parâmetros de busca
        if (ex instanceof TipoProdutoInvalidoException e) {
            details.put("tipoProdutoFornecido", e.getTipoProdutoFornecido());
            details.put("tiposValidos", "CORTE, CONSUMO");
        } else if (ex instanceof OperadorEstoqueInvalidoException e) {
            details.put("operadorFornecido", e.getOperadorFornecido());
            details.put("operadoresValidos", "GTE (≥), LTE (≤)");
        } else {
            details.put("info", ex.getMessage());
        }

        log.warn("{}: {}", ex.getClass().getSimpleName(), ex.getMessage());
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, details);
    }

    /**
     * Trata exceções de conflito, como criação de recurso duplicado ou recurso em uso (HTTP 409 Conflict).
     * Intercepta {@link TipoMateriaPrimaJaExisteException}, {@link ProdutoEmUsoException},
     * {@link TipoMateriaPrimaEmUsoException}, {@link ProdutoNomeDuplicadoException},
     * {@link ProdutoSkuDuplicadoException} e {@link ExclusaoLoteBloqueadaException}.
     *
     * @param ex A exceção de conflito lançada.
     * @return Um {@link ResponseEntity} contendo um {@link ErrorResponseDTO} com status 409.
     */
    @ExceptionHandler({
            TipoMateriaPrimaJaExisteException.class, ProdutoEmUsoException.class,
            TipoMateriaPrimaEmUsoException.class, ProdutoNomeDuplicadoException.class,
            ProdutoSkuDuplicadoException.class, ExclusaoLoteBloqueadaException.class
    })
    public ResponseEntity<ErrorResponseDTO> handleConflictExceptions(RuntimeException ex) {
        Map<String, String> details = new HashMap<>();
        if (ex instanceof TipoMateriaPrimaJaExisteException e) { details.put("nome", e.getNome()); }
        else if (ex instanceof ProdutoEmUsoException e) { details.put("produtoId", String.valueOf(e.getProdutoId())); details.put("entidadesEmUso", e.getEntidadeIds().toString()); }
        else if (ex instanceof TipoMateriaPrimaEmUsoException e) { details.put("tipoMateriaPrimaId", String.valueOf(e.getTipoMateriaPrimaId())); details.put("lotesEmUso", e.getLoteIds().toString()); }
        else if (ex instanceof ProdutoNomeDuplicadoException e) { details.put("nome", e.getNome()); }
        else if (ex instanceof ProdutoSkuDuplicadoException e) { details.put("sku", e.getSku()); }
        else if (ex instanceof ExclusaoLoteBloqueadaException e) { details.put("info", e.getMessage()); }

        log.warn("{}: {}. Detalhes: {}", ex.getClass().getSimpleName(), ex.getMessage(), details);
        return buildErrorResponse(ex, HttpStatus.CONFLICT, details);
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
        log.warn("{}: {}. Detalhes: {}", ex.getClass().getSimpleName(), ex.getMessage(), ex.getErrors());
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
        Map<String, String> details = new HashMap<>();
        if (ex instanceof EstoqueInsuficienteParaMovimentacaoException e) {
            details.put("loteId", String.valueOf(e.getLoteId()));
            details.put("quantidadeRequisitada", String.valueOf(Math.abs(e.getQuantidadeRequisitada())));
            details.put("saldoDisponivel", String.valueOf(e.getSaldoDisponivel()));
        } else if (ex instanceof EstoqueInsuficienteCanalException e) {
            details.put("produtoId", String.valueOf(e.getProdutoId()));
            details.put("canalVendaId", String.valueOf(e.getCanalVendaId()));
            details.put("quantidadeRequisitada", String.valueOf(Math.abs(e.getQuantidadeRequisitada())));
            details.put("estoqueAtual", String.valueOf(e.getEstoqueAtual()));
        } else if (ex instanceof AlocacaoEstoqueExcedeTotalException e) {
            details.put("quantidadeParaAlocar", String.valueOf(e.getQuantidadeParaAlocar()));
            details.put("novoTotalDistribuido", String.valueOf(e.getNovoTotalDistribuido()));
            details.put("estoqueFisicoTotal", String.valueOf(e.getEstoqueFisicoTotal()));
        } else if (ex instanceof SaldoMateriaPrimaInsuficienteException e) {
            details.put("quantidadeRequisitada", String.valueOf(e.getQuantidadeRequisitada()));
            details.put("saldoDisponivel", String.valueOf(e.getSaldoDisponivel()));
        }
        log.warn("{}: {}. Detalhes: {}", ex.getClass().getSimpleName(), ex.getMessage(), details);
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
    public ResponseEntity<ErrorResponseDTO> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error ->
                errors.put(error.getField(), error.getDefaultMessage()));

        log.warn("Erros de validação de argumento de método: {}", errors);
        return buildErrorResponse("Erro de validação. Verifique os campos informados.", HttpStatus.BAD_REQUEST, errors);
    }

    /**
     * Trata erros de desserialização de JSON ou requisições com corpo ilegível (HTTP 400 Bad Request).
     * Fornece detalhes sobre o campo que causou o erro de formato, se disponível.
     *
     * @param ex A exceção {@link HttpMessageNotReadableException} lançada.
     * @return Um {@link ResponseEntity} contendo um {@link ErrorResponseDTO} com status 400.
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleMalformedJson(HttpMessageNotReadableException ex) {
        String msg = "JSON malformado ou sintaxe inválida na requisição.";
        String detalhe;
        if (ex.getMostSpecificCause() instanceof InvalidFormatException invalidFormat) {
            String campo = invalidFormat.getPath().stream().map(JsonMappingException.Reference::getFieldName).collect(Collectors.joining("."));
            detalhe = String.format("Campo '%s' recebeu valor inválido: '%s'.", campo, invalidFormat.getValue());
        } else {
            ex.getMostSpecificCause();
            detalhe = ex.getMostSpecificCause().getMessage();
        }
        log.warn("JSON inválido: {}", detalhe);
        return buildErrorResponse(msg, HttpStatus.BAD_REQUEST, Map.of("erro", detalhe));
    }

    /**
     * Trata parâmetros de requisição obrigatórios ausentes do Spring (HTTP 400 Bad Request).
     * Para outros parâmetros, retorna erro genérico.
     *
     * @param ex A exceção do Spring lançada.
     * @return Um {@link ResponseEntity} contendo um {@link ErrorResponseDTO} com status 400.
     */
    @ExceptionHandler(org.springframework.web.bind.MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingRequestParameter(org.springframework.web.bind.MissingServletRequestParameterException ex) {
        String parameterName = ex.getParameterName();

        // Para outros parâmetros, retorna erro genérico
        Map<String, String> details = new HashMap<>();
        details.put("parametro", parameterName);
        details.put("mensagem", ex.getMessage());

        log.warn("Parâmetro obrigatório ausente: {}", parameterName);
        return buildErrorResponse(ex, HttpStatus.BAD_REQUEST, details);
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
        log.warn("Método HTTP não permitido: method={} uri={} permitted={}", ex.getMethod(), request.getRequestURI(), metodosPermitidos);
        return buildErrorResponse(msg, HttpStatus.METHOD_NOT_ALLOWED, Map.of("metodosPermitidos", metodosPermitidos));
    }

    /**
     * Trata exceções internas do servidor (HTTP 500 Internal Server Error).
     * Intercepta {@link JsonMergeException} e {@link ArquivoStorageException}.
     *
     * @param ex A exceção interna do servidor lançada.
     * @return Um {@link ResponseEntity} contendo um {@link ErrorResponseDTO} com status 500.
     */
    @ExceptionHandler({JsonMergeException.class, ArquivoStorageException.class})
    public ResponseEntity<ErrorResponseDTO> handleInternalServerExceptions(RuntimeException ex) {
        log.error("Erro interno do servidor: ", ex);
        String msg = "Ocorreu um erro interno inesperado. Tente novamente mais tarde.";
        return buildErrorResponse(msg, HttpStatus.INTERNAL_SERVER_ERROR, Map.of("detalhe", ex.getMessage()));
    }

    /**
     * Manipula erros de integridade (ex: tentar criar produto com nome duplicado)
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDatabaseErrors(DataIntegrityViolationException ex) {
        log.error("Conflito de dados no banco de dados.", ex);
        return buildErrorResponse(
                "Conflito de dados. Este registro já existe ou viola uma regra de integridade.",
                HttpStatus.CONFLICT,
                null
        );
    }

    /**
     * Manipula uploads maiores que o permitido
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponseDTO> handleMaxSizeException(MaxUploadSizeExceededException ex) {
        log.warn("Tentativa de upload de arquivo excedeu o tamanho máximo permitido.", ex);
        return buildErrorResponse(
                "O arquivo enviado excede o tamanho máximo permitido.",
                HttpStatus.EXPECTATION_FAILED,
                null
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
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex) {
        log.error("Erro inesperado: ", ex);
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

    //endregion
}
