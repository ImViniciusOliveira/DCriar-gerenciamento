package com.dcriar.api.validation.annotation;

import com.dcriar.api.dto.request.product.AjusteEstoqueRequestDTO;
import com.dcriar.api.validation.validator.AjusteEstoqueValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Anotação de validação para garantir que um {@link AjusteEstoqueRequestDTO} seja válido.
 * <p>
 * Esta anotação é aplicada no nível da classe e utiliza o {@link AjusteEstoqueValidator}
 * para implementar a lógica de validação, que verifica se os campos obrigatórios
 * (produtoId, canalVendaId e quantidade) não são nulos.
 *
 * @see AjusteEstoqueValidator
 * @see AjusteEstoqueRequestDTO
 */
@Documented
@Constraint(validatedBy = AjusteEstoqueValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidAjusteEstoque {
    String message() default "Requisição de ajuste de estoque inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
