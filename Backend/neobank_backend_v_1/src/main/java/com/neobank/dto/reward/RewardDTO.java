// RewardDTO.java
package com.neobank.dto.reward;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class RewardDTO {
    private Long    id;
    private Integer pointsBalance;
    private String  lastUpdated;
}

// RewardHistoryDTO.java
