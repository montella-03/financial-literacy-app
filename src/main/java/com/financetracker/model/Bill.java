package com.financetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Bill implements OwnedRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Version
    @Column(nullable = false)
    @org.hibernate.annotations.ColumnDefault("0")
    private Long version;
    @Column(nullable = false, length = 80)
    private String ownerId;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal amount;
    @Column(nullable = false)
    private java.time.LocalDate dueDate;
    private String category;
    private Long debtId;
    private boolean monthly;
    private int dueDay;
    private java.time.LocalDate paidOn;
}
