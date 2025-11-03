package com.dcriar.api.validation.annotation;

import com.dcriar.api.dto.request.stock.MovimentacaoRequestDTO;
import com.dcriar.api.validation.validator.MovimentacaoRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Anotação de validação para garantir que um {@link MovimentacaoRequestDTO} seja válido.
 * <p>
 * Esta anotação é aplicada no nível da classe e utiliza o {@link MovimentacaoRequestValidator}
 * para implementar a lógica de validação, que verifica se os campos obrigatórios
 * (tipo, quantidade e motivo) não são nulos ou vazios, e se a quantidade é diferente de zero.
 *
 * @see MovimentacaoRequestValidator
 * @see MovimentacaoRequestDTO
 */
@Documented
@Constraint(validatedBy = MovimentacaoRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMovimentacaoRequest {
    String message() default "Requisição de movimentação inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
