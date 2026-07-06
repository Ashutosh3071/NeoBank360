package com.neobank.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.neobank.dto.RedeemRequest;
import com.neobank.dto.RewardResponse;
import com.neobank.service.RewardService;

@RestController
@RequestMapping("/rewards")
public class RewardController {

    @Autowired
    private RewardService rewardService;

    @GetMapping
    public ResponseEntity<RewardResponse> getRewardBalance(Authentication authentication) {
        String email = authentication.getName();
        return ResponseEntity.ok(rewardService.getRewardBalanceByEmail(email));
    }

    @PostMapping("/redeem")
    public ResponseEntity<?> redeemPoints(Authentication authentication, @RequestBody RedeemRequest request) {
        String email = authentication.getName();
        rewardService.redeemReward(email, request);
        return ResponseEntity.ok().body(java.util.Map.of("message", "Points redeemed successfully"));
    }

}
