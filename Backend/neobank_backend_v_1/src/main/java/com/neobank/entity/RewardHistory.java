// src/main/java/com/neobank/entity/RewardHistory.java

package com.neobank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "reward_history",
    indexes = {
        @Index(name = "idx_rh_reward",    columnList = "reward_id"),
        @Index(name = "idx_rh_created",   columnList = "created_at"),
    }
)
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class RewardHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reward_id", nullable = false)
    private Reward reward;

    @Column(name = "points", nullable = false)
    private Integer points;   // positive = earned, negative = spent

    @Column(name = "description", nullable = false, length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 20)
    private ActionType actionType;

    @Column(name = "balance_after", nullable = false)
    private Integer balanceAfter;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public enum ActionType {
        EARNED,   // points added
        SPENT,    // points deducted
        EXPIRED,  // future use
        ADJUSTED  // admin correction
    }
}