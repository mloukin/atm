package com.interview.atm.services;

import com.interview.atm.domain.WithdrawalDto;
import com.interview.atm.entities.TransactionEntity;
import com.interview.atm.integration.ExternalSystemManager;
import com.interview.atm.repositories.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class TransactionServiceImplTest {

    private final TransactionRepository transactionRepository = mock(TransactionRepository.class);
    private final ExternalSystemManager externalSystemManager = mock(ExternalSystemManager.class);
    private final ThreadPoolTaskExecutor transactionTaskExecutor = mock(ThreadPoolTaskExecutor.class);
    private final TransactionService transactionService = new TransactionServiceImpl(transactionRepository, externalSystemManager, transactionTaskExecutor);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(transactionService, "withdrawalMaxCount", 5);
        ReflectionTestUtils.setField(transactionService, "withdrawalLimitAmount", 2000);
        ReflectionTestUtils.setField(transactionService, "queueCapacity", 100);
        ReflectionTestUtils.invokeMethod(transactionService, "initialize");

        when(externalSystemManager.valid(any())).thenReturn(true);
        when(transactionRepository.findAllByCardNumberAndTransactionTimeBetweenAndStateNotInOrderByTransactionTime(any(), any(), any(), any())).thenReturn(createTestEntities());
        when(transactionRepository.save(any())).thenReturn(TransactionEntity.builder()
                                                                .id(5L)
                                                                .cardNumber(123456789L)
                                                                .securityCode(123)
                                                                .amount(150)
                                                                .transactionTime(LocalDateTime.of(2024, 1, 19, 14, 23, 10))
                                                                .state(State.NEW).build());
    }

    @Test
    void withdrawal() {
        WithdrawalDto dto = WithdrawalDto.of(123456789L, 123L, 150D);
        assertTrue(transactionService.withdrawal(dto));
        verify(transactionTaskExecutor).submit(any(Runnable.class));
        verify(transactionRepository, times(2)).save(any());


    }

    @Test
    void cancelWithdrawal() {
        WithdrawalDto dto = WithdrawalDto.of(123456789L, 123L, 1300D);
        transactionService.cancelWithdrawal(dto);
        verify(transactionTaskExecutor).submit(any(Runnable.class));
        verify(transactionRepository, times(2)).save(any());
    }

    private List<TransactionEntity> createTestEntities() {
        return List.of(
                TransactionEntity.builder()
                        .id(1L)
                        .cardNumber(123456789L)
                        .transactionTime(LocalDateTime.of(2024, 1, 19, 9, 15, 55))
                        .amount(500D)
                        .state(State.DONE)
                        .securityCode(123)
                        .build(),
                TransactionEntity.builder()
                        .id(2L)
                        .cardNumber(123456789L)
                        .transactionTime(LocalDateTime.of(2024, 1, 19, 12, 0, 10))
                        .amount(1000D)
                        .state(State.DONE)
                        .securityCode(123)
                        .build(),
                TransactionEntity.builder()
                        .id(3L)
                        .cardNumber(123456789L)
                        .transactionTime(LocalDateTime.of(2024, 1, 19, 12, 1, 10))
                        .amount(-1000D)
                        .state(State.CANCELED)
                        .securityCode(123)
                        .build(),
                TransactionEntity.builder()
                        .id(4L)
                        .cardNumber(123456789L)
                        .transactionTime(LocalDateTime.of(2024, 1, 19, 12, 15, 10))
                        .amount(1300D)
                        .state(State.DONE)
                        .securityCode(123)
                        .build()
        );
    }
}