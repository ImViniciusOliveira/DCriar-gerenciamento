package com.dcriar.api.validation.annotation;

import com.dcriar.api.validation.validator.VerificacaoCorteRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação de validação para o DTO de Verificação de Corte.
 * Garante que as regras de negócio para simulação e verificação de layout sejam respeitadas.
 */
@Constraint(validatedBy = VerificacaoCorteRequestValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidVerificacaoCorteRequest {
    String message() default "Validação da verificação de corte falhou";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
