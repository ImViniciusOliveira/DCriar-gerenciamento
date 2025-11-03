package com.dcriar.api.validation.annotation;

import com.dcriar.api.validation.validator.CorteRealizadoRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CorteRealizadoRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCorteRealizadoRequest {
    String message() default "Requisição de corte realizado inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
