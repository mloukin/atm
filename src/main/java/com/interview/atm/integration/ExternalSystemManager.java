package com.interview.atm.integration;

import com.interview.atm.domain.WithdrawalDto;

public interface ExternalSystemManager {
    boolean valid(WithdrawalDto dto);
    void withdrawalReservation(Long cardNumber, Long secretCode, Double amount);
    void rollbackWithdrawal(Long cardNumber, Long secretCode, Double amount);
}
