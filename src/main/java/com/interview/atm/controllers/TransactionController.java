package com.interview.atm.controllers;

import com.interview.atm.services.TransactionService;
import com.interview.atm.domain.WithdrawalDto;
import lombok.AllArgsConstructor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/transactions")
@AllArgsConstructor
public class TransactionController {

    @NonNull
    private final TransactionService transactionService;

    @PostMapping
    public boolean withdrawal(@RequestBody @NonNull WithdrawalDto withdrawalDto) {
        log.debug("requested withdrawal: {}", withdrawalDto);
        return transactionService.withdrawal(withdrawalDto);
    }

    @DeleteMapping
    public void cancelWithdrawal(@RequestBody @NonNull WithdrawalDto withdrawalDto) {
        log.debug("requested cancel withdrawal: {}", withdrawalDto);
        transactionService.cancelWithdrawal(withdrawalDto);
    }
}
