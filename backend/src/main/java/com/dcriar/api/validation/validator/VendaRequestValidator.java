package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.sales.ItemVendaRequestDTO;
import com.dcriar.api.dto.request.sales.VendaRequestDTO;
import com.dcriar.api.validation.annotation.ValidVendaRequest;
import com.dcriar.domain.common.util.BrazilDocumentNormalizer;
import com.dcriar.domain.common.util.BrazilStateSupport;
import com.dcriar.domain.common.util.CountrySupport;
import com.dcriar.domain.common.util.TrimTextNormalizer;

import java.util.List;

/**
 * Validador para o DTO {@link VendaRequestDTO}, acionado pela anotação {@link ValidVendaRequest}.
 * <p>
 * Este validador verifica se os campos essenciais da requisição de venda estão presentes:
 * <ul>
 *     <li>O {@code canalVendaId} não pode ser nulo.</li>
 *     <li>A lista de {@code itens} não pode ser nula nem vazia.</li>
 * </ul>
 * A validação de cada item individual na lista é delegada para suas respectivas anotações.
 */
public class VendaRequestValidator extends BaseValidator<ValidVendaRequest, VendaRequestDTO> {

    private static final int MAX_NOME_COMPLETO = 200;
    private static final int MAX_APELIDO = 120;
    private static final int MAX_ENDERECO = 255;
    private static final int MAX_NUMERO = 40;
    private static final int MAX_BAIRRO = 120;
    private static final int MAX_PAIS = 120;
    private static final int MAX_CIDADE = 120;
    private static final int MAX_ESTADO = 60;
    private static final int MAX_DOCUMENTO = 40;
    private static final int MAX_CODIGO_POSTAL = 20;
    private static final int MAX_OBSERVACAO = 100;

    @Override
    protected void validate(VendaRequestDTO dto) {
        addViolationIf(dto.getCanalVendaId() == null, "O ID do canal de venda é obrigatório.", "canalVendaId");

        List<ItemVendaRequestDTO> itens = dto.getItens();
        addViolationIf(itens == null || itens.isEmpty(), "A lista de itens não pode estar vazia.", "itens");

        validateMaxLength(dto.getNomeCompleto(), MAX_NOME_COMPLETO, "nomeCompleto", "Nome completo");
        validateMaxLength(dto.getPais(), MAX_PAIS, "pais", "País");
        validateMaxLength(dto.getApelido(), MAX_APELIDO, "apelido", "Apelido");
        validateMaxLength(dto.getEndereco(), MAX_ENDERECO, "endereco", "Endereço");
        validateMaxLength(dto.getNumero(), MAX_NUMERO, "numero", "Número");
        validateMaxLength(dto.getBairro(), MAX_BAIRRO, "bairro", "Bairro");
        validateMaxLength(dto.getCidade(), MAX_CIDADE, "cidade", "Cidade");
        validateMaxLength(dto.getEstado(), MAX_ESTADO, "estado", "Estado");
        validateMaxLength(dto.getCpf(), MAX_DOCUMENTO, "cpf", "Documento");
        validateMaxLength(dto.getCep(), MAX_CODIGO_POSTAL, "cep", "Código postal");
        validateMaxLength(dto.getObservacao(), MAX_OBSERVACAO, "observacao", "Observação");

        boolean usaLocalidadeBrasil = CountrySupport.isBrazil(dto.getPais());

        String cpf = dto.getCpf();
        addViolationIf(usaLocalidadeBrasil && cpf != null && !BrazilDocumentNormalizer.isValidCpf(cpf),
                buildCpfMessage(cpf),
                "cpf");

        String cep = dto.getCep();
        addViolationIf(usaLocalidadeBrasil && cep != null && !BrazilDocumentNormalizer.isValidCep(cep),
                buildCepMessage(cep),
                "cep");

        String estado = dto.getEstado();
        addViolationIf(usaLocalidadeBrasil && estado != null && !BrazilStateSupport.isValid(estado),
                buildEstadoMessage(estado),
                "estado");
    }

    private void validateMaxLength(String value, int maxLength, String fieldName, String label) {
        String trimmed = TrimTextNormalizer.trimToNull(value);
        addViolationIf(trimmed != null && trimmed.length() > maxLength,
                label + " não pode ter mais de " + maxLength + " caracteres. Valor informado: " + quote(trimmed) + ".",
                fieldName);
    }

    private String buildCpfMessage(String cpf) {
        String normalized = BrazilDocumentNormalizer.normalizeCpf(cpf);
        return "CPF inválido: " + quote(cpf) + ". Valor normalizado: " + quote(normalized)
                + ". Informe um CPF com 11 dígitos válidos.";
    }

    private String buildCepMessage(String cep) {
        String normalized = BrazilDocumentNormalizer.normalizeCep(cep);
        return "CEP inválido: " + quote(cep) + ". Valor normalizado: " + quote(normalized)
                + ". Informe 8 dígitos.";
    }

    private String buildEstadoMessage(String estado) {
        return "Estado inválido: " + quote(estado)
                + ". Informe a sigla UF ou o nome de um estado brasileiro.";
    }

    private String quote(String value) {
        return value == null ? "\"\"" : "\"" + value + "\"";
    }
}
