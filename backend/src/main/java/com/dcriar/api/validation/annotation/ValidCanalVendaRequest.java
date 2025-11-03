package com.dcriar.api.validation.annotation;

import com.dcriar.api.validation.validator.CanalVendaRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = CanalVendaRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCanalVendaRequest {
    String message() default "Requisição de canal de venda inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
