package com.dcriar.api.validation.validator;

import com.dcriar.api.dto.request.stock.MovimentacaoRequestDTO;
import com.dcriar.api.validation.annotation.ValidMovimentacaoRequest;
import com.dcriar.domain.stock.entity.enums.TipoMovimentacao;

import java.math.BigDecimal;

/**
 * Validador para o DTO {@link MovimentacaoRequestDTO}, acionado pela anotação {@link ValidMovimentacaoRequest}.
 * <p>
 * Este validador verifica as regras de negócio para uma movimentação de estoque:
 * <ul>
 *     <li>O {@code tipo} da movimentação não pode ser nulo.</li>
 *     <li>A {@code quantidade} não pode ser nula nem zero.</li>
 *     <li>O {@code motivo} não pode ser nulo ou vazio.</li>
 *     <li>A {@code quantidade} deve ser positiva para movimentações de ENTRADA.</li>
 *     <li>A {@code quantidade} deve ser negativa para movimentações de SAIDA ou PERDA.</li>
 * </ul>
 */
public class MovimentacaoRequestValidator extends BaseValidator<ValidMovimentacaoRequest, MovimentacaoRequestDTO> {

    @Override
    protected void validate(MovimentacaoRequestDTO dto) {
        TipoMovimentacao tipo = dto.getTipo();
        BigDecimal quantidade = dto.getQuantidade();

        addViolationIf(tipo == null, "O tipo da movimentação é obrigatório.", "tipo");
        addViolationIf(dto.getMotivo() == null || dto.getMotivo().isBlank(), "O motivo da movimentação é obrigatório.", "motivo");
        addViolationIf(quantidade == null || quantidade.compareTo(BigDecimal.ZERO) == 0, "A quantidade da movimentação é obrigatória e deve ser diferente de zero.", "quantidade");

        // A lógica condicional só é executada se os campos base forem válidos
        if (tipo != null && quantidade != null) {
            String tipoName = tipo.name();
            boolean isEntrada = tipoName.startsWith("ENTRADA");
            boolean isSaidaOuPerda = tipoName.startsWith("SAIDA") || tipoName.startsWith("PERDA");

            addViolationIf(isEntrada && quantidade.compareTo(BigDecimal.ZERO) < 0, "Para movimentações de entrada, a quantidade deve ser positiva.", "quantidade");
            addViolationIf(isSaidaOuPerda && quantidade.compareTo(BigDecimal.ZERO) > 0, "Para movimentações de saída ou perda, a quantidade deve ser negativa.", "quantidade");
        }
    }
}
