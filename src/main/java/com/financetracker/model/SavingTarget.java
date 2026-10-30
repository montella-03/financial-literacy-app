package com.financetracker.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.math.BigDecimal;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"owner_id", "plan_month"}))
@Getter
@Setter
@NoArgsConstructor
public class SavingTarget implements OwnedRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 80)
    private String ownerId;
    @Column(name = "plan_month", nullable = false, length = 7)
    private String month;
    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;
    @Version
    private Long version;
}
