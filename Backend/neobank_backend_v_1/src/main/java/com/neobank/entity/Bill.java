// src/main/java/com/neobank/entity/Bill.java

package com.neobank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "bills",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uq_bill",
            columnNames = {"user_id", "biller_name", "due_year", "due_month"}
        )
    },
    indexes = {
        @Index(name = "idx_bill_user",   columnList = "user_id"),
        @Index(name = "idx_bill_status", columnList = "status"),
        @Index(name = "idx_bill_due",    columnList = "due_date"),
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Bill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "biller_name", nullable = false, length = 255)
    private String billerName;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    // Virtual columns for uniqueness — stored separately for constraint
    @Column(name = "due_year",  insertable = false, updatable = false)
    private Integer dueYear;

    @Column(name = "due_month", insertable = false, updatable = false)
    private Integer dueMonth;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private BillStatus status = BillStatus.PENDING;

    @Column(name = "description", length = 500)
    private String description;

    // Reminder: set true if due within 3 days (computed, not stored)
    @Transient
    private Boolean remindMe;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // ── Enums ──
    public enum BillStatus {
        PENDING, PAID, OVERDUE
    }

    // ── Helper ──
    public boolean isOverdue() {
        return status == BillStatus.PENDING
            && LocalDate.now().isAfter(dueDate);
    }

    public boolean isDueSoon(int days) {
        LocalDate today = LocalDate.now();
        return status == BillStatus.PENDING
            && !dueDate.isBefore(today)
            && !dueDate.isAfter(today.plusDays(days));
    }
}