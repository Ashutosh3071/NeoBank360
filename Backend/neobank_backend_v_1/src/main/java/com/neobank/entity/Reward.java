// src/main/java/com/neobank/entity/Reward.java

package com.neobank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rewards")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class Reward {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "points_balance", nullable = false)
    @Builder.Default
    private Integer pointsBalance = 0;

    @UpdateTimestamp
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;

    @OneToMany(mappedBy = "reward", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RewardHistory> history = new ArrayList<>();

    // ── Helper ──
    public void addPoints(int points) {
        this.pointsBalance += points;
    }

    public void deductPoints(int points) {
        if (this.pointsBalance < points) {
            throw new IllegalStateException(
                "Insufficient points. Balance: " + this.pointsBalance
            );
        }
        this.pointsBalance -= points;
    }
}