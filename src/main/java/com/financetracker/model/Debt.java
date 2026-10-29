package com.financetracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "debts")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Debt implements OwnedRecord {

    @Column(length = 80)
    private String ownerId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("0")
    private Long version;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @Column(precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Column(precision = 19, scale = 2)
    private BigDecimal minimumPayment;

    private String notes;
}
