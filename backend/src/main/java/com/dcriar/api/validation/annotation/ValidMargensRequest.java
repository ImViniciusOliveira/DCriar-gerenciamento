package com.dcriar.api.validation.annotation;

import com.dcriar.api.dto.request.production.MargensRequestDTO;
import com.dcriar.api.validation.validator.MargensRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotação de validação para garantir que um {@link MargensRequestDTO} seja válido.
 * <p>
 * Esta anotação é aplicada no nível da classe e utiliza o {@link MargensRequestValidator}
 * para implementar a lógica de validação, que verifica se todos os campos de margem
 * (superior, inferior, esquerda, direita) são nulos ou não negativos.
 *
 * @see MargensRequestValidator
 * @see MargensRequestDTO
 */
@Documented
@Constraint(validatedBy = MargensRequestValidator.class)
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMargensRequest {

    String message() default "Margens inválidas: todos os valores devem ser maiores ou iguais a zero.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
