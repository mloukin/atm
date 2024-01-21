package com.interview.atm.repositories;

import com.interview.atm.entities.TransactionEntity;
import com.interview.atm.services.State;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface TransactionRepository extends JpaRepository<TransactionEntity, Long> {
    List<TransactionEntity> findAllByCardNumberAndTransactionTimeBetweenAndStateNotInOrderByTransactionTime(Long cardNumber,LocalDateTime from, LocalDateTime to, List<State> states);
}
