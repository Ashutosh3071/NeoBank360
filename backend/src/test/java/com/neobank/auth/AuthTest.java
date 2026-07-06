package com.neobank.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private BCryptPasswordEncoder passwordEncoder;

	@Test
	void checkHash() {
		String johnHash = "$2a$12$7WMyBffTjnku6LsY7.U9NuOejeL32TaGxmp3m1iN3X4tZBZeJnCrS";
		String user2Hash = "$2a$12$EwwUa4Pd1djmOhvuASZuvu5FwUnXhEAhoW5SqrJpnWt5vy9211G12";
		String testuser2Hash = "$2a$12$yDcDii7wbK7WI8HPVvvbhu/BSZrSazQiJ4295D827lZOYt1BblyAi";
		String pstestHash = "$2a$12$uKszeYYnEzU9/4xT8Xu2ceJl344/ImMg71QYc3hDBS6YH2nFBQx4q";
		String[] candidates = {
			"Password@123",
			"NeoBank@123",
			"NeoBank@1234Secure!",
			"Password@1234",
			"Password@123!",
			"Password123!",
			"Password@12345",
			"Password123",
			"password123",
			"john@test.com",
			"user2@test.com",
			"admin@test.com",
			"Admin@123",
			"Admin@1234",
			"Customer@123",
			"Customer@1234",
			"User@123",
			"User@1234",
			"NeoBank360@123",
			"NeoBank@360",
			"NeoBank360",
			"Password@123456",
			"Password@12345678"
		};
		for (String c : candidates) {
			if (passwordEncoder.matches(c, johnHash)) {
				System.out.println("JOHN HASH MATCH FOR: " + c);
			}
			if (passwordEncoder.matches(c, user2Hash)) {
				System.out.println("USER2 HASH MATCH FOR: " + c);
			}
			if (passwordEncoder.matches(c, testuser2Hash)) {
				System.out.println("TESTUSER2 HASH MATCH FOR: " + c);
			}
			if (passwordEncoder.matches(c, pstestHash)) {
				System.out.println("PSTEST HASH MATCH FOR: " + c);
			}
		}
	}

	private final ObjectMapper objectMapper = new ObjectMapper();

	private String uniqueEmail() {
		return "user_" + UUID.randomUUID() + "@neobank.in";
	}

	private String uniqueAadhaar() {
		long randomNum = (long) (Math.random() * 900000000000L) + 100000000000L;
		return String.valueOf(randomNum);
	}

	private String uniquePan() {
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < 5; i++) {
			sb.append((char) ('A' + (int) (Math.random() * 26)));
		}
		for (int i = 0; i < 4; i++) {
			sb.append((int) (Math.random() * 10));
		}
		sb.append((char) ('A' + (int) (Math.random() * 26)));
		return sb.toString();
	}

	private Map<String, Object> validRegisterPayload() {
		Map<String, Object> payload = new HashMap<>();
		payload.put("fullName", "Ritik Shekhar Parida");
		payload.put("email", uniqueEmail());
		payload.put("password", "NeoBank@123");
		payload.put("aadhaarNumber", uniqueAadhaar());
		payload.put("panNumber", uniquePan());
		return payload;
	}

	private org.springframework.test.web.servlet.ResultActions performRegister(Map<String, Object> payload) throws Exception {
		return mockMvc.perform(post("/auth/register")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(payload)));
	}

	@Test
	@DisplayName("Should register successfully with valid details")
	void shouldRegisterSuccessfully() throws Exception {
		Map<String, Object> payload = validRegisterPayload();
		performRegister(payload).andExpect(status().isCreated());
	}

	@Test
	@DisplayName("Should reject duplicate email with 409 Conflict")
	void shouldRejectDuplicateEmail() throws Exception {
		Map<String, Object> payload1 = validRegisterPayload();

		// First registration should succeed
		performRegister(payload1).andExpect(status().isCreated());

		// Second registration with same email should fail (we must change Aadhaar and PAN to be unique so it fails on email)
		Map<String, Object> payload2 = validRegisterPayload();
		payload2.put("email", payload1.get("email"));

		performRegister(payload2).andExpect(status().isConflict());
	}

	@Test
	@DisplayName("Should reject weak password with 400 Bad Request")
	void shouldRejectWeakPassword() throws Exception {
		Map<String, Object> payload = validRegisterPayload();
		payload.put("password", "weak");

		performRegister(payload).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Should reject invalid email format with 400 Bad Request")
	void shouldRejectInvalidEmailFormat() throws Exception {
		Map<String, Object> payload = validRegisterPayload();
		payload.put("email", "invalid-email-format");

		performRegister(payload).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Should reject missing full name with 400 Bad Request")
	void shouldRejectMissingFullName() throws Exception {
		Map<String, Object> payload = validRegisterPayload();
		payload.put("fullName", "");

		performRegister(payload).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Should reject missing password with 400 Bad Request")
	void shouldRejectMissingPassword() throws Exception {
		Map<String, Object> payload = validRegisterPayload();
		payload.put("password", "");

		performRegister(payload).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Should reject password without uppercase letter")
	void shouldRejectPasswordWithoutUppercase() throws Exception {
		Map<String, Object> payload = validRegisterPayload();
		payload.put("password", "neobank@123");

		performRegister(payload).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Should reject password without lowercase letter")
	void shouldRejectPasswordWithoutLowercase() throws Exception {
		Map<String, Object> payload = validRegisterPayload();
		payload.put("password", "NEOBANK@123");

		performRegister(payload).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Should reject password without digit")
	void shouldRejectPasswordWithoutDigit() throws Exception {
		Map<String, Object> payload = validRegisterPayload();
		payload.put("password", "NeoBank@abc");

		performRegister(payload).andExpect(status().isBadRequest());
	}

	@Test
	@DisplayName("Should reject password without special character")
	void shouldRejectPasswordWithoutSpecialCharacter() throws Exception {
		Map<String, Object> payload = validRegisterPayload();
		payload.put("password", "NeoBank123");

		performRegister(payload).andExpect(status().isBadRequest());
	}
}