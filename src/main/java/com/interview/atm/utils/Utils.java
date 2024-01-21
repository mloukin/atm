package com.interview.atm.utils;

import lombok.NonNull;

public final class Utils {
    private Utils() {
    }

    public static String prefix = "*****";

    public static int obfuscateCreditCardNumber(@NonNull Long cardNumber) {
        return (int) (cardNumber % Math.pow(10, 6));
    }
}
