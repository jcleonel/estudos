@Service
@RequiredArgsConstructor
@Slf4j
public class MovementPddService {

    private final MovementUseCase useCase;
    private final MovementProducerService producer;

    @Value("${module.pdd.kafka.producer.topics}")
    private String pddTopic;

    /**
     * Método usado pelo Controller. Permanece recebendo V1, mas agora
     * cria um Contexto antes de chamar o novo método 'process'.
     */
    public PddDataInput processMovement(MessageInput<Movement> messageInput) {
        final String transactionId = messageInput.getAccountMovementV1().getMessageControl().getTransactionId();
        log.info("Requisição PDD (síncrona) para movimentação de custódia. transactionId: {}", transactionId);
        
        // Cria o contexto a partir da mensagem V1
        MovementContext context = MovementContext.ofV1(messageInput);
        
        // Chama o método 'process' refatorado
        PddDataInput inputPdd = process(context);
        
        postMessagePdd(messageInput, inputPdd); // Este método pode continuar como está por enquanto
        log.info("Requisição PDD para movimentação de custódia finalizada com sucesso. transactionId: {}", transactionId);
        return inputPdd;
    }

    /**
     * MÉTODO 'PROCESS' REFATORADO.
     * Agora recebe o MovementContext e orquestra a chamada para o UseCase.
     */
    public PddDataInput process(MovementContext context) {
        final String transactionId = context.getTransactionId();
        log.info("Iniciando processamento de negócio para a versão {}. transactionId: {}", context.getVersion(), transactionId);

        PddDataInput pddDataInput;

        if ("V1".equals(context.getVersion())) {
            // Se for V1, chama o método 'execute' existente no UseCase
            pddDataInput = useCase.execute(context.getAsV1());
        } else if ("V2".equals(context.getVersion())) {
            // Se for V2, chamará uma nova lógica de negócio no UseCase
            // Por enquanto, apenas definimos a chamada. Implementaremos o executeV2 no próximo passo.
            log.info("Direcionando para o fluxo de negócio da V2...");
            pddDataInput = useCase.executeV2(context.getAsV2()); // Placeholder para o próximo passo
        } else {
            throw new IllegalArgumentException("Versão de mensagem não suportada para processamento: " + context.getVersion());
        }

        log.info("Processamento de negócio para transactionId: {} finalizado com sucesso.", transactionId);
        return pddDataInput;
    }

    // O método postMessagePdd pode ser mantido por agora, pois ele depende do 'messageInput' V1
    // que pode ser extraído do resultado do UseCase se necessário, ou refatorado depois.
    public void postMessagePdd(MessageInput<Movement> messageInput, PddDataInput pddDataInput) {
        // ... (lógica existente) ...
    }
}










