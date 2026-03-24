package com.dcriar.api.validation.annotation;

import com.dcriar.api.dto.request.stock.CalcularAjusteLoteRequestDTO;
import com.dcriar.api.validation.validator.CalcularAjusteLoteRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação de validação para o cálculo de ajuste operacional de lote.
 */
@Documented
@Constraint(validatedBy = CalcularAjusteLoteRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCalcularAjusteLoteRequest {
    String message() default "Requisição de cálculo de ajuste de lote inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
