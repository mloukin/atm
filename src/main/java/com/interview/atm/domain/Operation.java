package com.interview.atm.domain;

import com.interview.atm.entities.TransactionEntity;
import com.interview.atm.services.AtmOperationType;
import lombok.Builder;

@Builder
public record Operation (AtmOperationType atmOperationType, TransactionEntity event){
}
