package com.financetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "plan_month", "category"}))
@Getter
@Setter
@NoArgsConstructor
public class Budget implements OwnedRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String ownerId;
    @Column(nullable = false)
    private String category;
    @Column(name = "plan_month", nullable = false)
    private String month;
    @Column(nullable = false, precision = 19, scale = 2)
    private java.math.BigDecimal limitAmount;
}
