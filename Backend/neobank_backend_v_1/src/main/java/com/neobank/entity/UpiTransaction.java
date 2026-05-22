// src/main/java/com/neobank/entity/UpiTransaction.java

package com.neobank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "upi_transactions",
    indexes = {
        @Index(name = "idx_upitxn_ref",    columnList = "upi_reference"),
        @Index(name = "idx_upitxn_sender", columnList = "sender_vpa"),
        @Index(name = "idx_upitxn_recvr",  columnList = "receiver_vpa"),
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class UpiTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upi_reference", unique = true, nullable = false, length = 30)
    private String upiReference;

    @Column(name = "transaction_id", length = 30)
    private String transactionId;   // Links to Transaction table

    @Column(name = "sender_vpa",   nullable = false, length = 100)
    private String senderVpa;

    @Column(name = "receiver_vpa", nullable = false, length = 100)
    private String receiverVpa;

    @Column(name = "amount", nullable = false, precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private UpiTxnStatus status = UpiTxnStatus.SUCCESS;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_upi_id")
    private UpiId senderUpiId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_upi_id")
    private UpiId receiverUpiId;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum UpiTxnStatus {
        SUCCESS, FAILED, PENDING, REVERSED
    }
}