// src/main/java/com/neobank/controller/DevMailController.java

package com.neobank.controller;

import com.neobank.dto.ApiResponseDTO;
import com.neobank.dto.DevMailDTO;
import com.neobank.entity.DevMail;
import com.neobank.repository.DevMailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * DEV ONLY — exposes intercepted mails for the frontend polling popup.
 * Excluded automatically in prod via @Profile("!prod").
 */
@RestController
@RequestMapping("/api/dev/mail")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Profile("!prod")                          // ← never active in production
public class DevMailController {

    private final DevMailRepository devMailRepository;

    // ── GET unseen mails (polled every 3s by frontend) ───────────────────────
    @GetMapping("/unseen")
    public ResponseEntity<ApiResponseDTO<List<DevMailDTO>>> getUnseen() {
        List<DevMailDTO> mails = devMailRepository
                .findBySeenFalseOrderByCreatedAtDesc()
                .stream()
                .map(this::toDTO)
                .toList();

        return ResponseEntity.ok(ApiResponseDTO.success("Unseen mails", mails));
    }

    // ── PATCH mark one as seen ────────────────────────────────────────────────
    @Transactional
    @PatchMapping("/{id}/seen")
    public ResponseEntity<ApiResponseDTO<Void>> markSeen(@PathVariable Long id) {
        devMailRepository.markAsSeen(id);
        return ResponseEntity.ok(ApiResponseDTO.success("Marked as seen", null));
    }

    // ── PATCH mark all as seen ────────────────────────────────────────────────
    @Transactional
    @PatchMapping("/seen-all")
    public ResponseEntity<ApiResponseDTO<Void>> markAllSeen() {
        devMailRepository.markAllAsSeen();
        return ResponseEntity.ok(ApiResponseDTO.success("All marked as seen", null));
    }

    // ── DELETE all dev mails (clear inbox) ───────────────────────────────────
    @DeleteMapping("/clear")
    public ResponseEntity<ApiResponseDTO<Void>> clearAll() {
        devMailRepository.deleteAll();
        return ResponseEntity.ok(ApiResponseDTO.success("Dev inbox cleared", null));
    }

    // ── Mapper ────────────────────────────────────────────────────────────────
    private DevMailDTO toDTO(DevMail m) {
        return DevMailDTO.builder()
                .id(m.getId())
                .recipient(m.getRecipient())
                .subject(m.getSubject())
                .body(m.getBody())
                .otp(m.getOtp())
                .seen(m.isSeen())
                .createdAt(m.getCreatedAt())
                .build();
    }
}