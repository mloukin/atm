package com.interview.atm.domain;

import com.interview.atm.entities.TransactionEntity;
import lombok.Builder;
import lombok.ToString;
import lombok.Value;

import static com.interview.atm.utils.Utils.obfuscateCreditCardNumber;
import static com.interview.atm.utils.Utils.prefix;

@Value
@ToString
@Builder
public class AggregatedTransaction {
    Long cardNumber;
    int withdrawalCount;
    Double totalAmount;
    TransactionEntity transaction;
    public boolean isNewTransactionExist() {
        return transaction != null;
    }

    @ToString.Include(name = "cardNumber")
    private String getLastDigitsOfCardNumber() {
        return prefix + obfuscateCreditCardNumber(cardNumber);
    }

    @ToString.Include(name = "transaction")
    private String ifNewTransactionExist() {
        return isNewTransactionExist() ? "exist new transaction" : "no other new transactions";
    }
}
