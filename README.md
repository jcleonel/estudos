@Data @NoArgsConstructor @AllArgsConstructor
public class MessageInputV2<T> {
    @JsonProperty("AccountMovementV2")
    @NotNull @Valid
    private AccountMovementV2<T> accountMovementV2;
}

@Data @NoArgsConstructor @AllArgsConstructor
public class AccountMovementV2<T> {
    @NotNull @Valid
    private MessageControl messageControl;

    @NotNull @Valid
    private T data;
}

@Data @NoArgsConstructor @AllArgsConstructor
public class MovementV2 {
    private String correlationId;
    private String registerAccountCode;
    private String tickerSymbolTypeCode;
    private String tickerSymbol;
    private Integer totalMovementQuantity;
    private String accountMovementType;
    private List<MovementTransactionV2> transactions;
    private List<Balance> balances;
}

@Data @NoArgsConstructor @AllArgsConstructor
public class MovementTransactionV2 {
    private String movementAccountCode;
    private String portfolioPositionTypeCode;
    private String portfolioPositionTypeName; // Novo campo
    private String movementTypeCode;
    private List<Investor> investors;
}

@Data @NoArgsConstructor @AllArgsConstructor
public class Balance {
    private String balanceType;
    private String movementTypeCode;
}
