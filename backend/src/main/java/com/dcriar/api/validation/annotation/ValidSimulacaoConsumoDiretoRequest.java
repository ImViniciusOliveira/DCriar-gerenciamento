package com.dcriar.api.validation.annotation;

import com.dcriar.api.validation.validator.SimulacaoConsumoDiretoRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = SimulacaoConsumoDiretoRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidSimulacaoConsumoDiretoRequest {
    String message() default "Requisição de simulação de consumo direto inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
