package com.dcriar.api.validation.annotation;

import com.dcriar.api.dto.request.sales.VendaRequestDTO;
import com.dcriar.api.validation.validator.VendaRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotação de validação para garantir que um {@link VendaRequestDTO} seja válido.
 * <p>
 * Esta anotação é aplicada no nível da classe e utiliza o {@link VendaRequestValidator}
 * para implementar a lógica de validação, que verifica se os campos obrigatórios
 * (como canalVendaId e a lista de itens) não são nulos ou vazios.
 *
 * @see VendaRequestValidator
 * @see VendaRequestDTO
 */
@Documented
@Constraint(validatedBy = VendaRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidVendaRequest {
    String message() default "Requisição de venda inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
