package com.financetracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "saving_goals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavingGoal implements OwnedRecord {

    @Column(length = 80)
    private String ownerId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal targetAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    private LocalDate targetDate;

    private LocalDate startDate;

    @Column(precision = 19, scale = 2)
    private BigDecimal openingAmount;

    @Version
    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("0")
    private Long version;

    private String priority = "MEDIUM";

    private boolean completed = false;
}
