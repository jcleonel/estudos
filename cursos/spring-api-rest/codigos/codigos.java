package br.com.b3.accounts.features.movement.models; // Ou um pacote compartilhado

import br.com.b3.accounts.util.models.MessageControl;
import br.com.b3.accounts.util.models.MessageInput;
import br.com.b3.accounts.util.models.MessageInputV2;
import lombok.Getter;

/**
 * Objeto transportador que encapsula uma mensagem de movimento,
 * seja V1 ou V2, para processamento unificado no início do fluxo do consumidor.
 */
@Getter
public class MovementContext {
    private final String version;
    private final String transactionId;
    private final String from;
    private final Object originalMessage; // Armazena MessageInput<Movement> ou MessageInputV2

    private MovementContext(String version, String transactionId, String from, Object originalMessage) {
        this.version = version;
        this.transactionId = transactionId;
        this.from = from;
        this.originalMessage = originalMessage;
    }

    // Fábrica estática para criar um contexto a partir de uma mensagem V1
    public static MovementContext ofV1(MessageInput<Movement> messageV1) {
        MessageControl control = messageV1.getAccountMovementV1().getMessageControl();
        return new MovementContext("V1", control.getTransactionId(), control.getFrom(), messageV1);
    }

    // Fábrica estática para criar um contexto a partir de uma mensagem V2
    public static MovementContext ofV2(MessageInputV2 messageV2) {
        MessageControl control = messageV2.getAccountMovementV2().getMessageControl();
        return new MovementContext("V2", control.getTransactionId(), control.getFrom(), messageV2);
    }

    // Métodos de conveniência para obter o payload no tipo correto de forma segura
    @SuppressWarnings("unchecked")
    public MessageInput<Movement> getAsV1() {
        if (!"V1".equals(version)) {
            throw new IllegalStateException("Não é possível obter como V1 uma mensagem da versão " + version);
        }
        return (MessageInput<Movement>) originalMessage;
    }

    public MessageInputV2 getAsV2() {
        if (!"V2".equals(version)) {
            throw new IllegalStateException("Não é possível obter como V2 uma mensagem da versão " + version);
        }
        return (MessageInputV2) originalMessage;
    }
}





import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
// ... outras importações ...
import br.com.b3.accounts.features.movement.models.MovementContext;
import br.com.b3.accounts.util.models.MessageInputV2;


@Service
@Slf4j
public class MovementKafkaConnector extends KafkaConsumerClass {

    // ... Injeções existentes (messagePackUtil, producer, etc.) ...

    // Adicionar um ObjectMapper para a detecção de versão e deserialização da V2
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


    @KafkaListener( /* ... */ )
    public void consumer(List<String> messages, Acknowledgment acknowledgment) {
        long startProcessFlowableBatch = System.nanoTime();
        String hashThread = UUID.randomUUID().toString();
        try {
            MDC.put("transactionId", hashThread);
            log.info("[{}] Recebido lote de {} mensagens", hashThread, messages.size());

            // ======================= CÓDIGO MODIFICADO =======================

            // A linha de conversão original é substituída por esta:
            List<MovementContext> contexts = messages.parallelStream()
                    .map(this::parseMessageToContext) // Mapeia cada JSON para um MovementContext
                    .filter(Objects::nonNull)          // Remove mensagens que falharam na conversão
                    .toList();
            
            log.info("[{}] Mensagens convertidas para contexto. V1: {}, V2: {}.",
                    hashThread,
                    contexts.stream().filter(c -> "V1".equals(c.getVersion())).count(),
                    contexts.stream().filter(c -> "V2".equals(c.getVersion())).count()
            );

            // =================================================================

            // O resto do código virá aqui...
            // List<MovementContext> contextsFiltered = filterMovementsToBeExecute(contexts);
            // ...

        } catch (Exception error) {
            // ...
        } finally {
            // ...
        }
    }

    /**
     * Novo método privado que detecta a versão, desserializa para o objeto correto
     * e o encapsula em um MovementContext.
     *
     * @param rawJson A mensagem em string recebida do Kafka.
     * @return Um MovementContext preenchido ou null se a conversão falhar.
     */
    private MovementContext parseMessageToContext(String rawJson) {
        try {
            JsonNode rootNode = objectMapper.readTree(rawJson);

            if (rootNode.has("AccountMovementV1")) {
                // Se é V1, usa o messagePackUtil existente
                MessageInput<Movement> msgV1 = this.messagePackUtil.convertMessage(rawJson, Movement.class);
                return MovementContext.ofV1(msgV1);

            } else if (rootNode.has("AccountMovementV2")) {
                // Se é V2, usa o objectMapper para os novos models
                MessageInputV2 msgV2 = objectMapper.readValue(rawJson, MessageInputV2.class);
                return MovementContext.ofV2(msgV2);

            } else {
                log.warn("Versão da mensagem não identificada. Conteúdo: {}", rawJson);
                return null;
            }
        } catch (Exception e) {
            log.error("Falha ao converter mensagem para MovementContext. Conteúdo: {}", rawJson, e);
            return null; // A falha será filtrada pelo .filter(Objects::nonNull)
        }
    }

    // ... resto da classe (execute, filterMovementsToBeExecute, etc.)
}