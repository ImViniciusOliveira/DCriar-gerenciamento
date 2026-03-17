package com.dcriar.api.validation.annotation;

import com.dcriar.api.validation.validator.SimulacaoConsumoRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SimulacaoConsumoRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSimulacaoConsumoRequest {
    String message() default "Requisição de simulação de consumo inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
