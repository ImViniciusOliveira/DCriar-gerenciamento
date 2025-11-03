package com.dcriar.api.validation.annotation;

import com.dcriar.api.dto.request.stock.TipoMateriaPrimaRequestDTO;
import com.dcriar.api.validation.validator.TipoMateriaPrimaRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Anotação de validação para garantir que um {@link TipoMateriaPrimaRequestDTO} seja válido.
 * <p>
 * Esta anotação é aplicada no nível da classe e utiliza o {@link TipoMateriaPrimaRequestValidator}
 * para implementar a lógica de validação, que verifica se os campos obrigatórios
 * (nome e unidadeDeConsumo) não são nulos ou vazios.
 *
 * @see TipoMateriaPrimaRequestValidator
 * @see TipoMateriaPrimaRequestDTO
 */
@Documented
@Constraint(validatedBy = TipoMateriaPrimaRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidTipoMateriaPrimaRequest {
    String message() default "Requisição de tipo de matéria-prima inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
