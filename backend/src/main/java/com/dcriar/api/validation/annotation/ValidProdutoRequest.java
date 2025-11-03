package com.dcriar.api.validation.annotation;

import com.dcriar.api.validation.validator.ProdutoRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Anotação de validação para o DTO {@link com.dcriar.api.dto.request.product.ProdutoRequestDTO}.
 * <p>
 * Aciona o {@link ProdutoRequestValidator} para garantir que todos os campos obrigatórios
 * do produto, incluindo suas dimensões, sejam fornecidos e válidos.
 */
@Constraint(validatedBy = ProdutoRequestValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidProdutoRequest {

    String message() default "Dados do produto inválidos";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
