// src/main/java/com/neobank/entity/DevMail.java

package com.neobank.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Stores intercepted emails in dev mode (IS_MAIL_SERVICE_SUPPORTED = false).
 * Polled by the frontend every 3 seconds to show popup notifications.
 */
@Entity
@Table(name = "dev_mails")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class DevMail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "recipient", nullable = false, length = 255)
    private String recipient;

    @Column(name = "subject", nullable = false, length = 255)
    private String subject;

    @Column(name = "body", nullable = false, columnDefinition = "TEXT")
    private String body;

    @Column(name = "otp", length = 8)
    private String otp;                    // extracted OTP if present, else null

    @Column(name = "seen", nullable = false)
    @Builder.Default
    private boolean seen = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}