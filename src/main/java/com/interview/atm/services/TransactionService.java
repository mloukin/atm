package com.interview.atm.services;

import com.interview.atm.domain.WithdrawalDto;
import lombok.NonNull;

public interface TransactionService {
    boolean withdrawal(@NonNull WithdrawalDto dto);
    void cancelWithdrawal(@NonNull WithdrawalDto dto);
}
