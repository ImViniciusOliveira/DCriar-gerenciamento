package com.dcriar.api.validation.annotation;

import com.dcriar.api.validation.validator.EstoqueRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EstoqueRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEstoqueRequest {
    String message() default "Requisição de estoque inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
