package com.financetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class SavingMovement implements OwnedRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String ownerId;
    @Column(nullable = false)
    private Long goalId;
    @Column(nullable = false)
    private java.time.LocalDate date;
    @Column(nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal amount;
    private String note;
}
