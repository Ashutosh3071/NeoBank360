// src/main/java/com/neobank/entity/Budget.java

package com.neobank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "budgets",
    uniqueConstraints = {
        @UniqueConstraint(
            name  = "uq_budget",
            columnNames = {"user_id", "category", "budget_month"}
        )
    },
    indexes = {
        @Index(name = "idx_budget_user",  columnList = "user_id"),
        @Index(name = "idx_budget_month", columnList = "budget_month"),
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Budget {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 20)
    private BudgetCategory category;

    // Stored as first day of month e.g. 2026-04-01
    @Column(name = "budget_month", nullable = false)
    private LocalDate budgetMonth;

    @Column(name = "limit_amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal limitAmount;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Enum ──
    public enum BudgetCategory {
        GROCERIES, UTILITIES, RENT, ENTERTAINMENT, TRANSFER, OTHER
    }
//     public enum BudgetCategory {
//     GROCERIES,
//     UTILITIES,
//     RENT,
//     FUEL,
//     ENTERTAINMENT,
//     TRANSFER,
//     MEDICAL,
//     EDUCATION,
//     INCOME,
//     OTHER
// }
}