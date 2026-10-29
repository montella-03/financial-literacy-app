package com.financetracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction implements OwnedRecord {

    @Column(length = 80)
    private String ownerId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TransactionType type;

    private String category;

    private String merchant;

    private String accountName;
    private String destinationAccount;
    @Column(unique = true, length = 160)
    private String importKey;

    @Column(length = 500)
    private String description;

    @Column(nullable = false)
    private LocalDateTime transactionDate;

    @Column(unique = true)
    private String sourceEmailId;

    @Column(length = 1000)
    private String rawEmailSnippet;

    @Column(nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public enum TransactionType {
        INCOME, EXPENSE, TRANSFER
    }
}
