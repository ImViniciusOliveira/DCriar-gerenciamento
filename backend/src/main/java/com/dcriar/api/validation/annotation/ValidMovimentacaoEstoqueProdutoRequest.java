package com.dcriar.api.validation.annotation;

import com.dcriar.api.validation.validator.MovimentacaoEstoqueProdutoRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MovimentacaoEstoqueProdutoRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMovimentacaoEstoqueProdutoRequest {
    String message() default "Requisição de movimentação de estoque de produto inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
