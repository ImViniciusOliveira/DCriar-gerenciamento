package com.dcriar.api.validation.annotation;

import com.dcriar.api.validation.validator.OrdemDeProducaoRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = OrdemDeProducaoRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOrdemDeProducaoRequest {
    String message() default "Requisição de ordem de produção inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
