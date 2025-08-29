package br.com.b3.accounts.features.movement.models;

import br.com.b3.accounts.util.models.MessageControl;
import br.com.b3.accounts.util.models.MessageInput;
import br.com.b3.accounts.util.models.MessageInputV2;
import lombok.Getter;

@Getter
public class MovementContext {
    private final String version;
    private final String transactionId;
    private final String from;
    private final String correlationId; // Novo campo, pode ser nulo para V1
    private final Object originalMessage;

    private MovementContext(String version, String transactionId, String from, String correlationId, Object originalMessage) {
        this.version = version;
        this.transactionId = transactionId;
        this.from = from;
        this.correlationId = correlationId;
        this.originalMessage = originalMessage;
    }

    // Fábrica para V1 (passa null para correlationId)
    public static MovementContext ofV1(MessageInput<Movement> messageV1) {
        MessageControl control = messageV1.getAccountMovementV1().getMessageControl();
        return new MovementContext("V1", control.getTransactionId(), control.getFrom(), null, messageV1);
    }

    // Fábrica para V2 (extrai e armazena o correlationId)
    public static MovementContext ofV2(MessageInputV2 messageV2) {
        MessageControl control = messageV2.getAccountMovementV2().getMessageControl();
        String corrId = messageV2.getAccountMovementV2().getData().getCorrelationId();
        return new MovementContext("V2", control.getTransactionId(), control.getFrom(), corrId, messageV2);
    }

    /**
     * **NOVO MÉTODO INTELIGENTE**
     * Gera a chave de dededuplicação baseada na versão da mensagem.
     * @return A chave de unicidade para o Redis.
     */
    public String getDeduplicationKey() {
        if ("V1".equals(this.version)) {
            // Lógica antiga para V1
            return this.transactionId + this.from;
        }
        if ("V2".equals(this.version)) {
            // Nova lógica para V2
            return this.transactionId + this.correlationId;
        }
        // Medida de segurança caso uma V3 seja adicionada sem atualizar aqui
        throw new UnsupportedOperationException("Não há lógica de dededuplicação para a versão: " + this.version);
    }
    
    // ... métodos getAsV1() e getAsV2() permanecem os mesmos ...
}










@Service
@RequiredArgsConstructor
public class MovementDeduplicationService {

    // ... campos existentes (expires, PROCESSED_IDS_SET_KEY, jedisPool) ...

    /**
     * Método refatorado para aceitar uma lista de MovementContext.
     * Agora ele é agnóstico à versão da mensagem.
     */
    public List<Boolean> addNewIds(List<MovementContext> contexts) {
        try (Jedis jedis = jedisPool.getResource()) {
            Pipeline pipeline = jedis.pipelined();
            List<Response<Long>> responses = new ArrayList<>();
            
            for (MovementContext ctx : contexts) {
                // A construção da chave é simplificada, pois a responsabilidade agora é do Contexto
                String deduplicationKey = ctx.getDeduplicationKey();
                
                String redisKey = PROCESSED_IDS_SET_KEY.concat(":").concat(deduplicationKey);
                
                responses.add(pipeline.setnx(redisKey, ""));
                pipeline.expire(redisKey, expires);
            }
            
            pipeline.sync();
            return responses.stream().map(r -> r.get() == 1L).toList();
        }
    }
}







@Service
@Slf4j
public class MovementKafkaConnector extends KafkaConsumerClass {

    // ...

    @KafkaListener( /* ... */ )
    public void consumer(List<String> messages, Acknowledgment acknowledgment) {
        // ...
        // A parte de conversão que fizemos no passo anterior continua igual...
        List<MovementContext> contexts = messages.parallelStream()
                .map(this::parseMessageToContext)
                .filter(Objects::nonNull)
                .toList();
        
        // Esta linha agora vai chamar o método adaptado
        List<MovementContext> contextsFiltered = filterMovementsToBeExecute(contexts);
        
        // E o resto do fluxo continua, passando a lista filtrada para o 'execute'
        contextsFiltered.forEach(ctx -> {
            futures.add(this.taskExecutor.submit(() -> this.execute(ctx)));
        });

        // ...
    }

    // ... parseMessageToContext(...) continua igual ...

    /**
     * Método adaptado para trabalhar com List<MovementContext>
     * @param contexts A lista de contextos convertidos.
     * @return Uma nova lista contendo apenas os contextos que não são duplicados.
     */
    private List<MovementContext> filterMovementsToBeExecute(List<MovementContext> contexts) {
        final int batchSize = 100; // O tamanho do seu batch para o Redis
        List<MovementContext> result = new ArrayList<>();
        
        for (int i = 0; i < contexts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, contexts.size());
            List<MovementContext> batch = contexts.subList(i, end);
            
            // A chamada para o serviço de dededuplicação agora passa o batch de contextos
            List<Boolean> pipelineResults = movementDeduplicationService.addNewIds(batch);
            
            for (int j = 0; j < pipelineResults.size(); j++) {
                if (Boolean.TRUE.equals(pipelineResults.get(j))) {
                    result.add(batch.get(j)); // Adiciona apenas os não-duplicados
                } else {
                    log.info("Mensagem duplicada detectada e ignorada. Chave: {}", batch.get(j).getDeduplicationKey());
                }
            }
        }
        return result;
    }

    // O método execute(MovementContext context) será o nosso próximo passo...
}