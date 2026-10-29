package com.financetracker.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "net_worth_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NetWorthItem implements OwnedRecord {

    @Column(length = 80)
    private String ownerId;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal value;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ItemType type;

    private String notes;

    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum ItemType {
        ASSET, LIABILITY
    }
}
