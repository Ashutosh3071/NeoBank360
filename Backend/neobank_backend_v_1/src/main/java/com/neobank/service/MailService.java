// src/main/java/com/neobank/service/MailService.java

package com.neobank.service;

import com.neobank.entity.DevMail;
import com.neobank.repository.DevMailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService {

    private final JavaMailSender    mailSender;
    private final DevMailRepository devMailRepository;   // ← injected

    // ─────────────────────────────────────────────
    // TOGGLE — set true when mail server is ready
    // ─────────────────────────────────────────────
    private static final boolean IS_MAIL_SERVICE_SUPPORTED = false;

    // OTP: 4–8 consecutive digits
    private static final Pattern OTP_PATTERN = Pattern.compile("\\b(\\d{4,8})\\b");

    // ─────────────────────────────────────────────
    //  SEND — accepts SimpleMailMessage directly
    // ─────────────────────────────────────────────
    public void send(SimpleMailMessage message) {
        String to      = message.getTo() != null && message.getTo().length > 0
                         ? String.join(", ", message.getTo()) : "—";
        String subject = message.getSubject() != null ? message.getSubject() : "—";
        String body    = message.getText()    != null ? message.getText()    : "—";
        sendMail(to, subject, body);
    }

    // ─────────────────────────────────────────────
    // SEND MAIL — dispatcher
    // ─────────────────────────────────────────────
    public void sendMail(String to, String subject, String body) {
        if (IS_MAIL_SERVICE_SUPPORTED) {
            sendViaJavaMailSender(to, subject, body);
        } else {
            saveToDatabase(to, subject, body);          // ← replaces printToConsole
            printToConsole(to, subject, body);  
        }
    }

    // ─────────────────────────────────────────────
    // REAL MAIL
    // ─────────────────────────────────────────────
    private void sendViaJavaMailSender(String to, String subject, String body) {
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(to);
            msg.setSubject(subject);
            msg.setText(body);
            mailSender.send(msg);
            log.info("✅ Mail sent to: {}", to);
        } catch (Exception e) {
            log.error("❌ Mail failed to: {} | {}", to, e.getMessage(), e);
            throw new RuntimeException("Mail sending failed: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────
    // DEV MODE — persist to DB, frontend polls it
    // ─────────────────────────────────────────────
    private void saveToDatabase(String to, String subject, String body) {
        String otp = extractOtp(body);

        DevMail mail = DevMail.builder()
                .recipient(to)
                .subject(subject)
                .body(body)
                .otp(otp)
                .seen(false)
                .build();

        devMailRepository.save(mail);
        log.info("📧 [DEV] Mail saved to DB | to={} | otp={}", to, otp != null ? otp : "none");
    }

    // ─────────────────────────────────────────────
    // UTIL
    // ─────────────────────────────────────────────
    private String extractOtp(String text) {
        if (text == null) return null;
        Matcher m = OTP_PATTERN.matcher(text);
        return m.find() ? m.group(1) : null;
    }
    
	private void printToConsole(String to, String subject, String body) {
		log.info("""

				╔══════════════════════════════════════════════════════════╗
				               📧  MAIL SERVICE (CONSOLE MODE)             
				╠══════════════════════════════════════════════════════════╣
				║  TO      : {}
				║  SUBJECT : {}
				╠══════════════════════════════════════════════════════════╣
				  BODY    :
				{}
				
				╚══════════════════════════════════════════════════════════╝
				""", to, subject, body);
	}
}