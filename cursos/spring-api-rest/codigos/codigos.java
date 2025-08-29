package br.com.b3.accounts.features.movement.validator; // Pacote original

import br.com.b3.accounts.exception.ApplicationDomainException;
import br.com.b3.accounts.features.movement.models.MovementContext; // Importar o Contexto
import br.com.b3.accounts.util.models.ApplicationError;
import br.com.b3.accounts.util.models.ApplicationErrorDescription;
import br.com.b3.accounts.util.models.ApplicationErrorListEnum;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MovementValidator {

    private final Validator validator;

    /**
     * Valida as anotações de uma mensagem de movimento (V1 ou V2)
     * contida dentro de um MovementContext.
     *
     * @param context O contexto contendo a mensagem original a ser validada.
     * @throws ApplicationDomainException Se alguma violação de validação for encontrada.
     */
    public void validateAndThrow(MovementContext context) throws ApplicationDomainException {
        // Usamos um Set com wildcard (?) para armazenar as violações,
        // já que o tipo do objeto validado (V1 ou V2) pode variar.
        Set<ConstraintViolation<?>> violations;

        // Verifica a versão e chama o validador com o tipo de objeto correto.
        if ("V1".equals(context.getVersion())) {
            violations = validator.validate(context.getAsV1());
        } else if ("V2".equals(context.getVersion())) {
            violations = validator.validate(context.getAsV2());
        } else {
            // Lança uma exceção se uma versão desconhecida for passada.
            throw new IllegalArgumentException("Versão de mensagem não suportada para validação: " + context.getVersion());
        }

        // A lógica de tratamento de erro a partir daqui é a mesma de antes,
        // pois ela opera sobre a interface genérica ConstraintViolation.
        if (!violations.isEmpty()) {
            List<ApplicationErrorDescription> errorsDetails = violations.stream()
                    .map(validation -> {
                        ApplicationErrorDescription errorDescription = new ApplicationErrorDescription(
                                ApplicationErrorListEnum.VALIDATION_ERROR);
                        errorDescription.setErrorDescription(
                                String.format("Erro na propriedade '%s': %s",
                                        validation.getPropertyPath(), validation.getMessage()));
                        return errorDescription;
                    }).toList();

            ApplicationError applicationError = new ApplicationError("VALIDATION_ERROR", errorsDetails);
            throw new ApplicationDomainException(applicationError);
        }
    }
}