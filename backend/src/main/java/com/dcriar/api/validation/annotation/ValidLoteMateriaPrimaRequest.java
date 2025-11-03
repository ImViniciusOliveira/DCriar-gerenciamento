package com.dcriar.api.validation.annotation;

import com.dcriar.api.dto.request.stock.LoteMateriaPrimaRequestDTO;
import com.dcriar.api.validation.validator.LoteMateriaPrimaRequestValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

/**
 * Anotação de validação para garantir que um {@link LoteMateriaPrimaRequestDTO} seja válido.
 * <p>
 * Esta anotação é aplicada no nível da classe e utiliza o {@link LoteMateriaPrimaRequestValidator}
 * para implementar a lógica de validação, que verifica se os campos obrigatórios
 * (como tipoMateriaPrimaId, unidadeDeEstoque, etc.) não são nulos e se os valores
 * numéricos são positivos.
 *
 * @see LoteMateriaPrimaRequestValidator
 * @see LoteMateriaPrimaRequestDTO
 */
@Documented
@Constraint(validatedBy = LoteMateriaPrimaRequestValidator.class)
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidLoteMateriaPrimaRequest {
    String message() default "Requisição de lote de matéria-prima inválida.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
