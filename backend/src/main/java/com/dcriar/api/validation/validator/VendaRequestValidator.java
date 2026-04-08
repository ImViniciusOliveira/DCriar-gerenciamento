package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.sales.ItemVendaRequestDTO;
import com.dcriar.api.dto.request.sales.VendaRequestDTO;
import com.dcriar.api.validation.annotation.ValidVendaRequest;
import com.dcriar.domain.common.util.BrazilDocumentNormalizer;
import com.dcriar.domain.common.util.BrazilStateSupport;
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
    private static final int MAX_CIDADE = 120;
    private static final int MAX_ESTADO = 60;
    private static final int MAX_OBSERVACAO = 1000;

    @Override
    protected void validate(VendaRequestDTO dto) {
        addViolationIf(dto.getCanalVendaId() == null, "O ID do canal de venda é obrigatório.", "canalVendaId");

        List<ItemVendaRequestDTO> itens = dto.getItens();
        addViolationIf(itens == null || itens.isEmpty(), "A lista de itens não pode estar vazia.", "itens");

        validateMaxLength(dto.getNomeCompleto(), MAX_NOME_COMPLETO, "nomeCompleto", "Nome completo");
        validateMaxLength(dto.getApelido(), MAX_APELIDO, "apelido", "Apelido");
        validateMaxLength(dto.getEndereco(), MAX_ENDERECO, "endereco", "Endereço");
        validateMaxLength(dto.getNumero(), MAX_NUMERO, "numero", "Número");
        validateMaxLength(dto.getBairro(), MAX_BAIRRO, "bairro", "Bairro");
        validateMaxLength(dto.getCidade(), MAX_CIDADE, "cidade", "Cidade");
        validateMaxLength(dto.getEstado(), MAX_ESTADO, "estado", "Estado");
        validateMaxLength(dto.getObservacao(), MAX_OBSERVACAO, "observacao", "Observação");

        String cpf = dto.getCpf();
        addViolationIf(cpf != null && !BrazilDocumentNormalizer.isValidCpf(cpf), "CPF inválido.", "cpf");

        String cep = dto.getCep();
        addViolationIf(cep != null && !BrazilDocumentNormalizer.isValidCep(cep), "CEP inválido. Informe 8 dígitos.", "cep");

        String estado = dto.getEstado();
        addViolationIf(estado != null && !BrazilStateSupport.isValid(estado), "Estado inválido.", "estado");
        addViolationIf(estado == null && dto.getCidade() != null, "Estado é obrigatório quando a cidade for informada.", "estado");
        addViolationIf(estado != null && dto.getCidade() == null, "Cidade é obrigatória quando o estado for informado.", "cidade");
    }

    private void validateMaxLength(String value, int maxLength, String fieldName, String label) {
        String trimmed = TrimTextNormalizer.trimToNull(value);
        addViolationIf(trimmed != null && trimmed.length() > maxLength,
                label + " não pode ter mais de " + maxLength + " caracteres.",
                fieldName);
    }
}
