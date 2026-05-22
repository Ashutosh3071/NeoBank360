// src/main/java/com/neobank/service/CaptchaService.java

package com.neobank.service;

import com.neobank.dto.auth.CaptchaResponseDTO;
import com.neobank.exception.NeoBankException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CaptchaService {

    // In-memory store: token → answer
    // Replace with Redis for production
    private final Map<String, String> captchaStore = new ConcurrentHashMap<>();

    private static final String CHARS = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

    // ── Generate new captcha ──
    public CaptchaResponseDTO generate() {
        String code  = generateCode(6);
        String token = UUID.randomUUID().toString();
        captchaStore.put(token, code);

        // Auto-expire after 10 minutes (simple cleanup)
        new Thread(() -> {
            try { Thread.sleep(10 * 60 * 1000); } catch (InterruptedException ignored) {}
            captchaStore.remove(token);
        }).start();

        log.info("Captcha generated — token: {}", token);

        return CaptchaResponseDTO.builder()
                .token(token)
                .captchaText(code)   // Send as text; swap for base64 image if needed
                .build();
    }

    // ── Validate captcha ──
    public void validate(String token, String answer) {
        String expected = captchaStore.get(token);

        if (expected == null) {
            throw new NeoBankException(
                HttpStatus.BAD_REQUEST,
                "CAPTCHA_EXPIRED",
                "Captcha has expired. Please refresh and try again."
            );
        }

        if (!expected.equalsIgnoreCase(answer)) {
            captchaStore.remove(token); // invalidate after wrong attempt
            throw new NeoBankException(
                HttpStatus.BAD_REQUEST,
                "CAPTCHA_INVALID",
                "Incorrect captcha. Please try again."
            );
        }

        captchaStore.remove(token); // one-time use
    }

    private String generateCode(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(CHARS.charAt((int)(Math.random() * CHARS.length())));
        }
        return sb.toString();
    }
}