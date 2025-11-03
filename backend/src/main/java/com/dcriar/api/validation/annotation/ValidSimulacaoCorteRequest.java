package com.dcriar.api.validation.annotation;

import com.dcriar.api.validation.validator.SimulacaoCorteRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SimulacaoCorteRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSimulacaoCorteRequest {
    String message() default "Requisição de simulação de corte inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
