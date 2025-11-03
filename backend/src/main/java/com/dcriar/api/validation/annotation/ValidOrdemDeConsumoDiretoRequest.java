package com.dcriar.api.validation.annotation;

import com.dcriar.api.validation.validator.OrdemDeConsumoDiretoRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = OrdemDeConsumoDiretoRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOrdemDeConsumoDiretoRequest {
    String message() default "Requisição de ordem de consumo direto inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
