package com.interview.atm.entities;

import com.interview.atm.services.State;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

import static com.interview.atm.utils.Utils.obfuscateCreditCardNumber;
import static com.interview.atm.utils.Utils.prefix;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
@ToString
@Table(name = "transactions")
public class TransactionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "card_number")
    private long cardNumber;

    @Column(name = "security_code")
    private long securityCode;

    @Column
    private double amount;

    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private State state;

    @Column(name = "transaction_time")
    @CreationTimestamp
    private LocalDateTime transactionTime;

    @Column(name = "comment")
    private String comment;

    @ToString.Include(name = "cardNumber")
    private String getLastDigitsOfCardNumber() {
        return prefix + obfuscateCreditCardNumber(cardNumber);
    }


    @ToString.Include(name = "secretCode")
    private String replaceSecretCode() {
        return prefix;
    }
}
