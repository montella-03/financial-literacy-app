package com.financetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "plan_month"}))
@Getter
@Setter
@NoArgsConstructor
public class MonthlySnapshot implements OwnedRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String ownerId;
    @Column(name = "plan_month", nullable = false)
    private String month;
    @Column(nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal debtBalance;
    @Column(nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal savingsBalance;
    private java.time.LocalDate capturedOn;
}
