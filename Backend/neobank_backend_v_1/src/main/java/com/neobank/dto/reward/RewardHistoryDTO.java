package com.neobank.dto.reward;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RewardHistoryDTO {
    private Long    id;
    private Integer points;
    private String  description;
    private String  actionType;   // EARNED / SPENT
    private Integer balanceAfter;
    private String  createdAt;
}