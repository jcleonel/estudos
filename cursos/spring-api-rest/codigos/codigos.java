// ... (início da classe com os métodos V1 existentes) ...

public class MovementRequestResponseMapper {
    
    // ... (todos os métodos toMessageOutputComplete, buildError, etc. para V1 continuam aqui) ...

    // =====================================================================
    // NOVOS MÉTODOS PARA RESPOSTAS V2
    // =====================================================================

    /**
     * Constrói uma mensagem de SUCESSO completa no formato de resposta V2.
     */
    public static AccountMovementV2Response<MovementMessageOutput> toMessageOutputV2(
            String topic, MovementContext context, int totalUpdates) {

        MessageInputV2 originalV2 = context.getAsV2();
        MovementV2 dataV2 = originalV2.getAccountMovementV2().getData();
        MessageControl originalControl = originalV2.getAccountMovementV2().getMessageControl();

        // O payload da resposta pode ser o mesmo da V1 (MovementMessageOutput),
        // ou um novo 'MovementMessageOutputV2' se os campos forem diferentes.
        // Vamos reutilizar por simplicidade.
        MovementMessageOutput outputData = new MovementMessageOutput(
                dataV2.getRegisterAccountCode(),
                dataV2.getTickerSymbolTypeCode(),
                dataV2.getTickerSymbol(),
                dataV2.getTotalMovementQuantity(),
                totalUpdates,
                "Processamento terminado com sucesso",
                "SUCCESS"
        );

        MessageControl newControl = new MessageControl(
                topic, originalControl.getTo(), originalControl.getFrom(), false,
                "producerSystemModule", LocalDateTime.now().toString(),
                originalControl.getTransactionId(), "1",
                "AccountMovementV2Response.json", List.of()
        );

        AccountMovementV2<MovementMessageOutput> accountV2 = new AccountMovementV2<>(newControl, outputData);
        return new AccountMovementV2Response<>(accountV2);
    }

    /**
     * Constrói uma mensagem de ERRO (ApplicationDomainException) no formato de resposta V2.
     */
    public static AccountMovementV2Response<MovementMessageOutput> toApplicationDomainExceptionV2(
            String errorTopic, ApplicationError error, MovementContext context) {
        
        // Lógica similar à de sucesso, mas construindo um payload de erro
        // ...
        // Retorna um new AccountMovementV2Response<>(...) com a lista de erros preenchida
        return null; // Placeholder para a implementação completa
    }

    // ... (outros métodos de erro para V2, como toValidationExceptionV2, etc.) ...
}





@Service
@Slf4j
public class MovementKafkaConnector extends KafkaConsumerClass {

    // ... (injeções e métodos que já ajustamos) ...

    public void execute(MovementContext context) {
        try {
            // ... (validação e chamada ao pddService.process(context)) ...
            PddDataInput pddDataInput = movementPddService.process(context);

            // Chamada final para postar a resposta de SUCESSO
            postResponseMessage(context, pddDataInput);

        } catch (Exception e) {
            log.error("Erro ao executar processamento para TransactionId: {}. Erro: {}", context.getTransactionId(), e.getMessage(), e);
            
            // Chamada final para postar a resposta de ERRO
            postErrorMessage(context, e);
        }
        // ...
    }

    /**
     * Envia a mensagem de SUCESSO para o tópico de resposta, no formato correto (V1 ou V2).
     */
    private void postResponseMessage(MovementContext context, PddDataInput pddDataInput) {
        log.info("Enviando resposta de sucesso para a versão {}. TransactionId: {}", context.getVersion(), context.getTransactionId());

        if ("V2".equals(context.getVersion())) {
            var responseV2 = MovementRequestResponseMapper.toMessageOutputV2(
                    this.topic, context, pddDataInput.getTotalUpdates());
            this.producer.postMessage(responseV2); // Producer envia o objeto V2
        } else {
            var responseV1 = MovementRequestResponseMapper.toMessageOutputComplete(
                    this.topic, context.getAsV1(), pddDataInput.getTotalUpdates());
            this.producer.postMessage(responseV1); // Producer envia o objeto V1
        }
    }

    /**
     * Envia a mensagem de ERRO para o tópico de erro, no formato correto (V1 ou V2).
     */
    private void postErrorMessage(MovementContext context, Exception exception) {
        log.info("Enviando resposta de erro para a versão {}. TransactionId: {}", context.getVersion(), context.getTransactionId());

        // Lógica para mapear diferentes tipos de exceção para diferentes mappers
        Object errorResponse = null;
        if (exception instanceof ApplicationDomainException) {
            ApplicationDomainException ex = (ApplicationDomainException) exception;
            if ("V2".equals(context.getVersion())) {
                errorResponse = MovementRequestResponseMapper.toApplicationDomainExceptionV2(
                        this.errorTopic, ex.getError(), context);
            } else {
                errorResponse = MovementRequestResponseMapper.toApplicationDomainException(
                        this.errorTopic, ex.getError(), context.getAsV1());
            }
        } 
        // ... else if (exception instanceof ValidationException) { ... }
        // ... (repetir o padrão para outros tipos de exceção) ...

        if (errorResponse != null) {
            this.producer.postMessageError(errorResponse);
        }
    }
}