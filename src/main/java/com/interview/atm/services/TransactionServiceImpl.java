package com.interview.atm.services;

import com.interview.atm.domain.AggregatedTransaction;
import com.interview.atm.domain.Operation;
import com.interview.atm.domain.WithdrawalDto;
import com.interview.atm.entities.TransactionEntity;
import com.interview.atm.integration.ExternalSystemManager;
import com.interview.atm.repositories.TransactionRepository;
import jakarta.annotation.PostConstruct;
import jakarta.transaction.Transactional;
import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.interview.atm.services.AtmOperationType.CANCEL;
import static com.interview.atm.services.AtmOperationType.WITHDRAWAL;
import static com.interview.atm.utils.Utils.obfuscateCreditCardNumber;
import static java.util.stream.Collectors.groupingBy;

@Slf4j
@Transactional
@Service
@RequiredArgsConstructor(onConstructor = @__({@Autowired}))
public class TransactionServiceImpl implements TransactionService {

    @NonNull
    private final TransactionRepository transactionRepository;
    @NonNull
    private final ExternalSystemManager externalSystemManager;
    @NonNull
    private final ThreadPoolTaskExecutor transactionTaskExecutor;

    @Value("${withdrawal.max.count:5}")
    private int withdrawalMaxCount;

    @Value("${withdrawal.limit.totalAmount:2000}")
    private double withdrawalLimitAmount;

    @Value("${queue.capacity:100000}")
    private int queueCapacity;

    private static final Set<State> states = Set.of(State.NEW, State.PENDING);
    private static final List<State> PROHIBITED_STATES = List.of(State.REJECTED);
    private TransactionExecutor transactionExecutor;

    @PostConstruct
    private void initialize() {
        transactionExecutor = new TransactionExecutor();
        ExecutorService executorService = Executors.newFixedThreadPool(1);
        executorService.submit(transactionExecutor);
    }

    @Override
    public boolean withdrawal(@NonNull WithdrawalDto dto) {
        log.debug("try to withdrawal for {}", dto);
        if (externalSystemManager.valid(dto) && validateWithdrawalLimitations(dto)) {
            TransactionEntity newTransaction = transactionRepository.save(TransactionEntity.builder()
                    .cardNumber(dto.getCardNumber())
                    .securityCode(dto.getSecretCode())
                    .amount(dto.getAmount())
                    .transactionTime(LocalDateTime.now())
                    .state(State.NEW).build());

            log.debug("created the new withdrawal transaction: {}", newTransaction);

            Operation event = Operation.builder()
                    .atmOperationType(WITHDRAWAL)
                    .event(newTransaction)
                    .build();
            try {
                transactionExecutor.getQueue().put(event);
                return true;
            } catch (InterruptedException e) {
                String errorMsg = "internal error (withdrawal transaction): %s".formatted(e.getMessage());
                log.error(errorMsg, e);
                transactionRepository.save(TransactionEntity.builder()
                        .id(newTransaction.getId())
                        .cardNumber(dto.getCardNumber())
                        .securityCode(dto.getSecretCode())
                        .amount(dto.getAmount())
                        .transactionTime(LocalDateTime.now())
                        .comment(errorMsg)
                        .state(State.REJECTED).build());
                return false;
            }
        } else {
            log.debug("""
                    withdrawal can't be processed for {}
                    Follow reasons could be:
                    1. Credit card is not allowed by external system
                    2. Withdrawal attempts reached limit {}
                    3. Withdrawal amount reached limit {} per one day
                    """, dto, withdrawalMaxCount, withdrawalLimitAmount);
        }
        return false;
    }

    @Override
    public void cancelWithdrawal(@NonNull WithdrawalDto dto) {
        log.debug("try to cancel previews withdrawal operation for {}", dto);
        if (externalSystemManager.valid(dto) && isWithdrawalTransactionExist(transactionRepository, dto)) {
            TransactionEntity newTransaction = transactionRepository.save(TransactionEntity.builder()
                    .cardNumber(dto.getCardNumber())
                    .securityCode(dto.getSecretCode())
                    .amount(-1 * dto.getAmount())
                    .transactionTime(LocalDateTime.now())
                    .state(State.NEW).build());

            log.debug("created the new cancel withdrawal transaction: {}", newTransaction);

            Operation event = Operation.builder()
                    .atmOperationType(AtmOperationType.CANCEL)
                    .event(newTransaction)
                    .build();
            try {
                transactionExecutor.getQueue().put(event);
            } catch (InterruptedException e) {
                String errorMsg = "internal error (cancel withdrawal): %s".formatted(e.getMessage());
                log.error(errorMsg, e);
                transactionRepository.save(TransactionEntity.builder()
                        .id(newTransaction.getId())
                        .cardNumber(dto.getCardNumber())
                        .securityCode(dto.getSecretCode())
                        .amount(dto.getAmount())
                        .transactionTime(LocalDateTime.now())
                        .comment(errorMsg)
                        .state(State.REJECTED).build());
            }
        } else {
            log.debug("""
                    cancel withdrawal cannot be processed for {}
                    Follow reasons could be:
                    1. Credit card is not allowed by external system
                    2. No withdrawal transaction with same amount was not found""", dto);
        }
    }

    private boolean validateWithdrawalLimitations(WithdrawalDto dto) {
        AggregatedTransaction resolvedData = resolveActualWithdrawalTransactions(transactionRepository, dto);
        log.debug("current summarized state for {} is {}", dto, resolvedData);
        return resolvedData.getWithdrawalCount() < withdrawalMaxCount  &&
                !resolvedData.isNewTransactionExist() &&
                resolvedData.getTotalAmount() + dto.getAmount() <= withdrawalLimitAmount;
    }

    private static AggregatedTransaction resolveActualWithdrawalTransactions(TransactionRepository transactionRepository, WithdrawalDto dto) {
        List<TransactionEntity> transactions = loadActualTransactions(dto.getCardNumber(), transactionRepository)
                .stream()
                .filter(t -> !PROHIBITED_STATES.contains(t.getState()))
                .toList();

        return AggregatedTransaction.builder()
                .cardNumber(dto.getCardNumber())
                .withdrawalCount(resolveCorrectTransactionsCount(transactions))
                .totalAmount(resolveTotalAmount(transactions))
                .transaction(resolveNewTransaction(transactions))
                .build();
    }

    private static int resolveCorrectTransactionsCount(List<TransactionEntity> transactions) {
        Map<State, List<TransactionEntity>> data = transactions
                .stream()
                .collect(groupingBy(TransactionEntity::getState));
       return data.getOrDefault(State.DONE, Collections.emptyList()).size() - data.getOrDefault(State.CANCELED, Collections.emptyList()).size();
    }

    private static double resolveTotalAmount(List<TransactionEntity> transactions) {
        return transactions.stream()
                .map(TransactionEntity::getAmount)
                .reduce(0.0, Double::sum);
    }

    private static TransactionEntity resolveNewTransaction(List<TransactionEntity> transactions) {
        return transactions.stream()
                .filter(t -> states.contains(t.getState()))
                .findFirst()
                .orElse(null);
    }

    private static boolean isWithdrawalTransactionExist(TransactionRepository transactionRepository, WithdrawalDto dto) {
        List<TransactionEntity> transactions = loadActualTransactions(dto.getCardNumber(), transactionRepository);
        TransactionEntity transaction = !CollectionUtils.isEmpty(transactions) ? transactions.get(transactions.size() - 1) : null;
        return transaction != null && transaction.getState() == State.DONE && dto.getAmount().compareTo(transaction.getAmount()) == 0;
    }

    private static List<TransactionEntity> loadActualTransactions(Long cardNumber, TransactionRepository transactionRepository) {
        LocalDateTime to = LocalDateTime.now(ZoneId.of("UTC")).toLocalDate().atTime(23, 23, 59);
        LocalDateTime from = to.toLocalDate().atStartOfDay();
        if (log.isDebugEnabled()) {
            log.debug("retrieve non REJECTED transactions for card number ******{} between {} and {}", obfuscateCreditCardNumber(cardNumber), from, to);
        }
        return transactionRepository.findAllByCardNumberAndTransactionTimeBetweenAndStateNotInOrderByTransactionTime(cardNumber, from, to, PROHIBITED_STATES);
    }

    @Getter
    private class TransactionExecutor implements Runnable {
        private final BlockingQueue<Operation> queue = new ArrayBlockingQueue<>(queueCapacity);
        private final AtomicBoolean cancelled = new AtomicBoolean(false);

        @Override
        public void run() {
            try {
                while (!Thread.currentThread().isInterrupted() && !cancelled.get()) {
                    Operation event = queue.take();
                    TransactionEntity transaction = event.event();
                    log.debug("consumed event {}/{} from the queue", event.atmOperationType(), transaction);
                        TransactionEntity pendingTransaction = transactionRepository.save(TransactionEntity.builder()
                                .id(transaction.getId())
                                .cardNumber(transaction.getCardNumber())
                                .securityCode(transaction.getSecurityCode())
                                .amount(transaction.getAmount())
                                .transactionTime(LocalDateTime.now())
                                .state(State.PENDING).build());

                        transactionTaskExecutor.submit(() -> {
                                    boolean isWithdrawal = WITHDRAWAL == event.atmOperationType();
                                    log.debug("{} in progress: {}", isWithdrawal ? WITHDRAWAL : CANCEL, pendingTransaction);
                                    if (isWithdrawal) {
                                        processWithdrawal(pendingTransaction);
                                    } else {
                                        processCancelWithdrawal(pendingTransaction);
                                    }
                                }
                        );
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("event processor has been interrupted");
            }
        }

        private void processWithdrawal(TransactionEntity transaction) {
            externalSystemManager.withdrawalReservation(transaction.getCardNumber(), transaction.getSecurityCode(), transaction.getAmount());
            TransactionEntity completedTransaction = transactionRepository.save(TransactionEntity.builder()
                    .id(transaction.getId())
                    .cardNumber(transaction.getCardNumber())
                    .securityCode(transaction.getSecurityCode())
                    .amount(transaction.getAmount())
                    .transactionTime(LocalDateTime.now())
                    .state(State.DONE).build());
            log.debug("withdrawal transaction {} was completed successful.", completedTransaction);
        }

        private void processCancelWithdrawal(TransactionEntity transaction) {
            externalSystemManager.rollbackWithdrawal(transaction.getCardNumber(), transaction.getSecurityCode(), transaction.getAmount());
            TransactionEntity canceledTransaction = transactionRepository.save(TransactionEntity.builder()
                    .id(transaction.getId())
                    .cardNumber(transaction.getCardNumber())
                    .securityCode(transaction.getSecurityCode())
                    .amount(transaction.getAmount())
                    .transactionTime(LocalDateTime.now())
                    .state(State.CANCELED).build());
            log.debug("cancel withdrawal transaction {} was completed successful.", canceledTransaction);
        }
    }
}
