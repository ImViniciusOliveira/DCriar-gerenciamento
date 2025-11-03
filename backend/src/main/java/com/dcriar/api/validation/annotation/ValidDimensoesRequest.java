package com.dcriar.api.validation.annotation;

import com.dcriar.api.dto.request.product.DimensoesRequestDTO;
import com.dcriar.api.validation.validator.DimensoesRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotação de validação para garantir que um {@link DimensoesRequestDTO} seja válido.
 * <p>
 * Esta anotação é aplicada no nível da classe e utiliza o {@link DimensoesRequestValidator}
 * para implementar a lógica de validação, que verifica se os campos de dimensão
 * (larguraCm e comprimentoCm) não são nulos e são valores positivos.
 *
 * @see DimensoesRequestValidator
 * @see DimensoesRequestDTO
 */
@Documented
@Constraint(validatedBy = DimensoesRequestValidator.class)
@Target({ ElementType.TYPE, ElementType.FIELD, ElementType.PARAMETER })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidDimensoesRequest {
    String message() default "Dimensões inválidas: largura e comprimento devem ser valores positivos.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
