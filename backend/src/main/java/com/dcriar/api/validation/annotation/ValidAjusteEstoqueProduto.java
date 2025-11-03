package com.dcriar.api.validation.annotation;

import com.dcriar.api.dto.request.product.AjusteEstoqueProdutoRequestDTO;
import com.dcriar.api.validation.validator.AjusteEstoqueProdutoValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotação de validação para garantir que um {@link AjusteEstoqueProdutoRequestDTO} seja válido.
 * <p>
 * Esta anotação é aplicada no nível da classe e utiliza o {@link AjusteEstoqueProdutoValidator}
 * para implementar a lógica de validação, que verifica se os campos obrigatórios
 * (produtoId, quantidade e motivo) não são nulos ou vazios.
 *
 * @see AjusteEstoqueProdutoValidator
 * @see AjusteEstoqueProdutoRequestDTO
 */
@Documented
@Constraint(validatedBy = AjusteEstoqueProdutoValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAjusteEstoqueProduto {
    String message() default "Requisição de ajuste de estoque de produto inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
