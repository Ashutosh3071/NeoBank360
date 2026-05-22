// src/main/java/com/neobank/service/MailService.java

package com.neobank.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MailService2 {

	private final JavaMailSender mailSender;

	// ─────────────────────────────────────────────
	// TOGGLE — set true when mail server is ready
	// ─────────────────────────────────────────────

	private static final boolean IS_MAIL_SERVICE_SUPPORTED = false;

	// ─────────────────────────────────────────────
	//  SEND — accepts SimpleMailMessage directly
	//  Same signature as JavaMailSender.send()
	// ─────────────────────────────────────────────

	public void send(SimpleMailMessage message) {
		String to = message.getTo() != null && message.getTo().length > 0 ? String.join(", ", message.getTo()) : "—";
		String subject = message.getSubject() != null ? message.getSubject() : "—";
		String body = message.getText() != null ? message.getText() : "—";

		sendMail(to, subject, body);
	}

	// ─────────────────────────────────────────────
	// SEND MAIL — main method
	// ─────────────────────────────────────────────

	public void sendMail(String to, String subject, String body) {
		if (IS_MAIL_SERVICE_SUPPORTED) {
			sendViaJavaMailSender(to, subject, body);
		} else {
			printToConsole(to, subject, body);
		}
	}

	// ─────────────────────────────────────────────
	// REAL MAIL — JavaMailSender
	// ─────────────────────────────────────────────

	private void sendViaJavaMailSender(String to, String subject, String body) {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(to);
			message.setSubject(subject);
			message.setText(body);
			mailSender.send(message);
			log.info("✅ Mail sent successfully to: {}", to);
		} catch (Exception e) {
			log.error("❌ Failed to send mail to: {} | Error: {}", to, e.getMessage(), e);
			throw new RuntimeException("Mail sending failed: " + e.getMessage());
		}
	}

	// ─────────────────────────────────────────────
	// CONSOLE FALLBACK
	// ─────────────────────────────────────────────

	private void printToConsole(String to, String subject, String body) {
		log.info("""

				╔══════════════════════════════════════════════════════════╗
				║              📧  MAIL SERVICE (CONSOLE MODE)             ║
				╠══════════════════════════════════════════════════════════╣
				║  TO      : {}
				║  SUBJECT : {}
				╠══════════════════════════════════════════════════════════╣
				║  BODY    :
				{}
				╚══════════════════════════════════════════════════════════╝
				""", to, subject, body);
	}
}