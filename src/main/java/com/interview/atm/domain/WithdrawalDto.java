package com.interview.atm.domain;

import lombok.ToString;
import lombok.Value;

import static com.interview.atm.utils.Utils.obfuscateCreditCardNumber;
import static com.interview.atm.utils.Utils.prefix;

@Value(staticConstructor = "of")
@ToString
public class WithdrawalDto {
    Long cardNumber;
    Long secretCode;
    Double amount;

    @ToString.Include(name = "cardNumber")
    private String getLast4DigitsOfCardNumber() {
        return prefix + obfuscateCreditCardNumber(cardNumber);
    }

    @ToString.Include(name = "secretCode")
    private String replaceSecretCode() {
       return prefix;
    }
}
