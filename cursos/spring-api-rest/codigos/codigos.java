package br.com.b3.accounts.features.movement.mappers;

import br.com.b3.accounts.features.movement.models.Movement;
import br.com.b3.accounts.features.movement.models.MovementMessageOutput;
import br.com.b3.accounts.util.models.AccountBalanceV1;
import br.com.b3.accounts.util.models.AccountMovementV1;
import br.com.b3.accounts.util.models.ApplicationError;
import br.com.b3.accounts.util.models.ApplicationErrorDescription;
import br.com.b3.accounts.util.models.ApplicationErrorListEnum;
import br.com.b3.accounts.util.models.MessageControl;
import br.com.b3.accounts.util.models.MessageInput;
import br.com.b3.accounts.util.models.PddDataInput;
import br.com.b3.accounts.util.models.PddMessageInput;
import br.com.b3.accounts.util.models.v2.AccountMovementV2;
import br.com.b3.accounts.util.models.v2.MessageInputV2;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.BiFunction;

public class MovementRequestResponseMapper {

    private static final String ERROR_STATUS = "ERROR";
    private static final String SUCCESS_STATUS = "SUCCESS";
    private static final String SUCCESS_MESSAGE = "Processamento terminado com sucesso";

    // --- Métodos de Sucesso ---

    public static MessageInput<MovementMessageOutput> toMessageOutputComplete(
            String topic, MessageInput<Movement> convertedMessage, int pddDataInputTotalUpdates) {
        return buildMessageOutputComplete(topic, convertedMessage, pddDataInputTotalUpdates, SUCCESS_MESSAGE, SUCCESS_STATUS, List.of());
    }

    public static MessageInputV2<MovementMessageOutput> toMessageOutputCompleteV2(
            String topic, MessageInputV2<Movement> convertedMessage, int pddDataInputTotalUpdates) {
        return buildMessageOutputCompleteV2(topic, convertedMessage, pddDataInputTotalUpdates, SUCCESS_MESSAGE, SUCCESS_STATUS, List.of());
    }

    public static MessageInput<MovementMessageOutput> buildMessageOutputComplete(
            String topic, MessageInput<Movement> movementMessage, Integer updatedTotalOfItems, String message,
            String status, List<ApplicationError> errors) {
        return buildGenericMessageOutput(topic, movementMessage.getAccountMovementV1().getData(),
                movementMessage.getAccountMovementV1().getMessageControl(), updatedTotalOfItems, message, status, errors,
                (newControl, outputData) -> new MessageInput<>(new AccountMovementV1<>(newControl, outputData)));
    }

    public static MessageInputV2<MovementMessageOutput> buildMessageOutputCompleteV2(
            String topic, MessageInputV2<Movement> movementMessage, Integer updatedTotalOfItems, String message,
            String status, List<ApplicationError> errors) {
        return buildGenericMessageOutput(topic, movementMessage.getAccountMovementV2().getData(),
                movementMessage.getAccountMovementV2().getMessageControl(), updatedTotalOfItems, message, status, errors,
                (newControl, outputData) -> new MessageInputV2<>(new AccountMovementV2<>(newControl, outputData)));
    }

    // --- Métodos PDD ---

    public static PddMessageInput toMessageInputPdd(
            String topic, MessageInput<Movement> movementMessage, PddDataInput pddDataInput, List<ApplicationError> errors) {
        return buildGenericPddMessage(topic, movementMessage.getAccountMovementV1().getData(),
                movementMessage.getAccountMovementV1().getMessageControl(), errors, pddDataInput);
    }

    public static PddMessageInput toMessageInputPddV2(
            String topic, MessageInputV2<Movement> movementMessage, PddDataInput pddDataInput, List<ApplicationError> errors) {
        return buildGenericPddMessage(topic, movementMessage.getAccountMovementV2().getData(),
                movementMessage.getAccountMovementV2().getMessageControl(), errors, pddDataInput);
    }

    // --- Métodos de Tratamento de Exceções ---

    public static MessageInput<MovementMessageOutput> toValidationException(
            String errorTopic, String errorMessage, MessageInput<Movement> convertedMessage) {
        return buildError(errorTopic, ApplicationErrorListEnum.PROCESSING_ERROR_MESSAGE.getError(), convertedMessage, createValidationError(errorMessage));
    }

    public static MessageInputV2<MovementMessageOutput> toValidationExceptionV2(
            String errorTopic, String errorMessage, MessageInputV2<Movement> convertedMessage) {
        return buildErrorV2(errorTopic, ApplicationErrorListEnum.PROCESSING_ERROR_MESSAGE.getError(), convertedMessage, createValidationError(errorMessage));
    }

    public static MessageInput<MovementMessageOutput> toApplicationDomainException(
            String errorTopic, ApplicationError exceptionMessage, MessageInput<Movement> convertedMessage) {
        return buildError(errorTopic, ApplicationErrorListEnum.PROCESSING_ERROR_MESSAGE.getError(), convertedMessage, exceptionMessage);
    }

    public static MessageInputV2<MovementMessageOutput> toApplicationDomainExceptionV2(
            String errorTopic, ApplicationError exceptionMessage, MessageInputV2<Movement> convertedMessage) {
        return buildErrorV2(errorTopic, ApplicationErrorListEnum.PROCESSING_ERROR_MESSAGE.getError(), convertedMessage, exceptionMessage);
    }

    public static MessageInput<MovementMessageOutput> toException(String errorTopic, MessageInput<Movement> convertedMessage) {
        return buildError(errorTopic, ApplicationErrorListEnum.PROCESSING_ERROR_MESSAGE.getError(), convertedMessage, createProcessingError());
    }

    public static MessageInputV2<MovementMessageOutput> toExceptionV2(String errorTopic, MessageInputV2<Movement> convertedMessage) {
        return buildErrorV2(errorTopic, ApplicationErrorListEnum.PROCESSING_ERROR_MESSAGE.getError(), convertedMessage, createProcessingError());
    }

    // AQUI ESTÃO OS MÉTODOS REINSERIDOS
    public static MessageInput<MovementMessageOutput> toExceptionInInitializerError(
            String errorTopic, MessageInput<Movement> convertedMessage) {
        return buildError(errorTopic, ApplicationErrorListEnum.INTERNAL_SERVER_ERROR.toString(),
                convertedMessage, createInternalServerError());
    }

    public static MessageInputV2<MovementMessageOutput> toExceptionInInitializerErrorV2(
            String errorTopic, MessageInputV2<Movement> convertedMessage) {
        return buildErrorV2(errorTopic, ApplicationErrorListEnum.INTERNAL_SERVER_ERROR.toString(),
                convertedMessage, createInternalServerError());
    }

    // --- Métodos de Construção de Erro ---
    
    public static MessageInput<MovementMessageOutput> buildError(
            String errorTopic, String errorMessage, MessageInput<Movement> convertedMessage, ApplicationError applicationError) {
        return buildMessageOutputComplete(errorTopic, convertedMessage, 0, errorMessage, ERROR_STATUS, List.of(applicationError));
    }

    public static MessageInputV2<MovementMessageOutput> buildErrorV2(
            String errorTopic, String errorMessage, MessageInputV2<Movement> convertedMessage, ApplicationError applicationError) {
        return buildMessageOutputCompleteV2(errorTopic, convertedMessage, 0, errorMessage, ERROR_STATUS, List.of(applicationError));
    }

    // --- MÉTODOS PRIVADOS E GENÉRICOS (O CORAÇÃO DA SOLUÇÃO) ---

    private static <T> T buildGenericMessageOutput(
            String topic, Movement movement, MessageControl originalControl, Integer updatedTotalOfItems,
            String message, String status, List<ApplicationError> errors,
            BiFunction<MessageControl, MovementMessageOutput, T> messageFactory) {
        MessageControl newControl = new MessageControl(topic, originalControl.getTo(), originalControl.getFrom(), false,
                "producerSystemModule", LocalDateTime.now().toString(),
                originalControl.getTransactionId(), "1", "messageSchema", errors);
        MovementMessageOutput outputData = new MovementMessageOutput(movement.getRegisterAccountCode(),
                movement.getTickerSymbolTypeCode(), movement.getTickerSymbol(), movement.getTotalMovementQuantity(),
                updatedTotalOfItems, message, status);
        return messageFactory.apply(newControl, outputData);
    }

    private static PddMessageInput buildGenericPddMessage(
            String topic, Movement movement, MessageControl originalControl,
            List<ApplicationError> errors, PddDataInput pddDataInput) {
        MessageControl newControl = new MessageControl(topic, originalControl.getTo(), originalControl.getFrom(), false,
                "producerSystemModule", LocalDateTime.now().toString(),
                originalControl.getTransactionId(), "1", "messageSchema", errors);
        PddDataInput newPddDataInput = new PddDataInput(movement.getTickerSymbol(), movement.getRegisterAccountCode(),
                movement.getTickerSymbolTypeCode(), pddDataInput.getTransactions());
        return new PddMessageInput(new AccountBalanceV1(newControl, newPddDataInput));
    }

    // --- Funções Auxiliares para criação de Erros ---

    private static ApplicationError createValidationError(String errorMessage) {
        ApplicationErrorDescription description = new ApplicationErrorDescription();
        description.setErrorCode("0000");
        description.setErrorTitle(errorMessage);
        description.setErrorDescription(errorMessage);
        return new ApplicationError("VALIDATION_ERROR", List.of(description));
    }

    private static ApplicationError createProcessingError() {
        return new ApplicationError("INITIALIZER_ERROR",
                List.of(new ApplicationErrorDescription(ApplicationErrorListEnum.PROCESSING_ERROR_MESSAGE)));
    }
    
    // MÉTODO HELPER ADICIONADO PARA O ERRO FALTANTE
    private static ApplicationError createInternalServerError() {
        return new ApplicationError("INITIALIZER_ERROR",
                List.of(new ApplicationErrorDescription(ApplicationErrorListEnum.INTERNAL_SERVER_ERROR)));
    }
}