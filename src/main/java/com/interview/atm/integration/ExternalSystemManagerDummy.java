package com.interview.atm.integration;

import com.interview.atm.domain.WithdrawalDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ExternalSystemManagerDummy implements ExternalSystemManager {
    @Override
    public boolean valid(WithdrawalDto dto) {
        log.debug("[DUMMY] processed credit card and balance validation against external systems like (banks, Cal, Max etc)");
        return true;
    }

    @Override
    public void withdrawalReservation(Long cardNumber, Long secretCode, Double amount) {
        log.debug("[DUMMY] processed withdrawal against external systems like (banks, Cal, Max etc)");
    }

    @Override
    public void rollbackWithdrawal(Long cardNumber, Long secretCode, Double amount) {
        log.debug("[DUMMY] processed rollback withdrawal against external systems like (banks, Cal, Max etc)");
    }
}
