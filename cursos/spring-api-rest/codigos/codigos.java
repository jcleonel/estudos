@Component
@RequiredArgsConstructor
public class MovementValidator {

    private final Validator validator;

    public void validateAndThrow(MovementContext context) throws ApplicationDomainException {
        if ("V1".equals(context.getVersion())) {
            // Valida o tipo específico V1
            Set<ConstraintViolation<MessageInput<Movement>>> violations = validator.validate(context.getAsV1());
            // Chama o método auxiliar para tratar as violações
            buildAndThrowApplicationException(violations);
        } else if ("V2".equals(context.getVersion())) {
            // Valida o tipo específico V2
            Set<ConstraintViolation<MessageInputV2>> violations = validator.validate(context.getAsV2());
            // Chama o mesmo método auxiliar
            buildAndThrowApplicationException(violations);
        } else {
            throw new IllegalArgumentException("Versão de mensagem não suportada para validação: " + context.getVersion());
        }
    }

    /**
     * Método auxiliar privado que constrói e lança a exceção a partir de um Set de violações.
     * Usando Set<?> ele consegue aceitar qualquer tipo de Set (de V1, V2, etc.).
     *
     * @param violations O conjunto de violações encontrado.
     * @throws ApplicationDomainException sempre que a lista de violações não estiver vazia.
     */
    private void buildAndThrowApplicationException(Set<?> violations) throws ApplicationDomainException {
        if (!violations.isEmpty()) {
            List<ApplicationErrorDescription> errorsDetails = violations.stream()
                    .map(v -> (ConstraintViolation<?>) v) // Faz um cast seguro para o tipo com wildcard
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