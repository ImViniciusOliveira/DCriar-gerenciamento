package com.dcriar.api.validation.annotation;

import com.dcriar.api.validation.validator.OrdemDeConsumoRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = OrdemDeConsumoRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidOrdemDeConsumoRequest {
    String message() default "Requisição de ordem de consumo inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
