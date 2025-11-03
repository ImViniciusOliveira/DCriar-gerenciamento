package com.dcriar.api.validation.annotation;

import com.dcriar.api.validation.validator.OrdemDeCorteRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação de validação para o DTO de Ordem de Corte.
 * Garante que, se o modo de cálculo for MANUAL, as dimensões finais sejam fornecidas.
 */
@Constraint(validatedBy = OrdemDeCorteRequestValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOrdemDeCorteRequest {
    String message() default "Validação da ordem de corte falhou";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
