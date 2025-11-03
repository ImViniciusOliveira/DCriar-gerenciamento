package com.dcriar.api.validation.annotation;

import com.dcriar.api.dto.request.sales.ItemVendaRequestDTO;
import com.dcriar.api.validation.validator.ItemVendaRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotação de validação para garantir que um {@link ItemVendaRequestDTO} seja válido.
 * <p>
 * Esta anotação é aplicada no nível da classe e utiliza o {@link ItemVendaRequestValidator}
 * para implementar a lógica de validação, que verifica se o ID do produto não é nulo
 * e se a quantidade é um valor positivo.
 *
 * @see ItemVendaRequestValidator
 * @see ItemVendaRequestDTO
 */
@Documented
@Constraint(validatedBy = ItemVendaRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidItemVendaRequest {
    String message() default "Item de venda inválido.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
