package com.dcriar.api.validation.annotation;

import com.dcriar.api.validation.validator.PrecoRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PrecoRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPrecoRequest {
    String message() default "Requisição de preço inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
