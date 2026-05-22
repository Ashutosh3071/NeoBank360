// RewardHistoryRepository.java
package com.neobank.repository;

import com.neobank.entity.RewardHistory;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RewardHistoryRepository extends JpaRepository<RewardHistory, Long> {
    Page<RewardHistory> findByRewardIdOrderByCreatedAtDesc(Long rewardId, Pageable pageable);
}