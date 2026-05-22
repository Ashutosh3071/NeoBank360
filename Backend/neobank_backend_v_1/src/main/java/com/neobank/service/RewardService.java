// src/main/java/com/neobank/service/RewardService.java

package com.neobank.service;

import com.neobank.dto.reward.*;
import com.neobank.entity.*;
import com.neobank.exception.NeoBankException;
import com.neobank.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class RewardService {

    private final RewardRepository        rewardRepository;
    private final RewardHistoryRepository rewardHistoryRepository;
    private final UserRepository          userRepository;

    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("dd MMM yyyy, hh:mm a");

    // ─────────────────────────────────────────────
    //  GET BALANCE
    //  BR-05: cross-user check via JWT
    // ─────────────────────────────────────────────

    @Transactional
    public RewardDTO getBalance(Long userId, String username) {
        User user = findUser(username);
        if (!user.getId().equals(userId)) {
            throw new NeoBankException(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                    "Access denied to this reward data.");
        }
        Reward reward = findOrCreate(user);
        return buildRewardDTO(reward);
    }

    // ─────────────────────────────────────────────
    //  GET MY BALANCE (by username directly)
    // ─────────────────────────────────────────────

    @Transactional
    public RewardDTO getMyBalance(String username) {
        User user = findUser(username);
        Reward reward = findOrCreate(user);
        return buildRewardDTO(reward);
    }

    // ─────────────────────────────────────────────
    //  GET HISTORY
    // ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<RewardHistoryDTO> getHistory(String username, int page, int size) {
        User user = findUser(username);
        Reward reward = rewardRepository.findByUserId(user.getId())
                .orElse(null);
        if (reward == null) return List.of();

        return rewardHistoryRepository
                .findByRewardIdOrderByCreatedAtDesc(
                        reward.getId(), PageRequest.of(page, size))
                .stream().map(this::buildHistoryDTO).toList();
    }

    // ─────────────────────────────────────────────
    //  EARN POINTS
    //  Called internally by BillService, TransactionService etc.
    // ─────────────────────────────────────────────

    @Transactional
    public void earnPoints(Long userId, int points, String description) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));

        Reward reward = findOrCreate(user);
        reward.addPoints(points);
        rewardRepository.save(reward);

        RewardHistory history = RewardHistory.builder()
                .reward(reward)
                .points(points)
                .description(description)
                .actionType(RewardHistory.ActionType.EARNED)
                .balanceAfter(reward.getPointsBalance())
                .build();
        rewardHistoryRepository.save(history);

        log.info("Reward +{} for userId:{} → balance: {}",
                points, userId, reward.getPointsBalance());
    }

    // ─────────────────────────────────────────────
    //  SPEND POINTS
    //  BR-03: balance must not go negative
    // ─────────────────────────────────────────────

    @Transactional
    public RewardDTO spendPoints(Long userId, int points,
                                  String description, String username) {
        User user = findUser(username);
        if (!user.getId().equals(userId)) {
            throw new NeoBankException(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                    "Access denied.");
        }

        Reward reward = findOrCreate(user);

        if (reward.getPointsBalance() < points) {
            throw new NeoBankException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "INSUFFICIENT_POINTS",
                    "Insufficient reward points. Balance: "
                    + reward.getPointsBalance() + " / Required: " + points);
        }

        reward.deductPoints(points);
        rewardRepository.save(reward);

        RewardHistory history = RewardHistory.builder()
                .reward(reward)
                .points(-points)
                .description(description)
                .actionType(RewardHistory.ActionType.SPENT)
                .balanceAfter(reward.getPointsBalance())
                .build();
        rewardHistoryRepository.save(history);

        return buildRewardDTO(reward);
    }

    // ─────────────────────────────────────────────
    //  AUTO-EARN on transaction (called from TransactionService)
    // ─────────────────────────────────────────────

    @Transactional
    public void awardTransactionPoints(Long userId, java.math.BigDecimal amount) {
        // 1 point per ₹100 transacted
        int points = amount.divide(
                java.math.BigDecimal.valueOf(100),
                0, java.math.RoundingMode.DOWN
        ).intValue();

        if (points > 0) {
            earnPoints(userId, points,
                    "Transaction reward: ₹" + amount.toPlainString());
        }
    }

    // ─────────────────────────────────────────────
    //  PRIVATE
    // ─────────────────────────────────────────────

    private Reward findOrCreate(User user) {
        return rewardRepository.findByUserId(user.getId())
                .orElseGet(() -> {
                    Reward r = Reward.builder()
                            .user(user)
                            .pointsBalance(0)
                            .build();
                    return rewardRepository.save(r);
                });
    }

    private User findUser(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NeoBankException(
                        HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User not found."));
    }

    private RewardDTO buildRewardDTO(Reward r) {
        return RewardDTO.builder()
                .id(r.getId())
                .pointsBalance(r.getPointsBalance())
                .lastUpdated(r.getLastUpdated() != null
                        ? r.getLastUpdated().format(FMT) : null)
                .build();
    }

    private RewardHistoryDTO buildHistoryDTO(RewardHistory h) {
        return RewardHistoryDTO.builder()
                .id(h.getId())
                .points(h.getPoints())
                .description(h.getDescription())
                .actionType(h.getActionType().name())
                .balanceAfter(h.getBalanceAfter())
                .createdAt(h.getCreatedAt() != null
                        ? h.getCreatedAt().format(FMT) : null)
                .build();
    }
}