// src/main/java/com/neobank/controller/AuthController.java

package com.neobank.controller;

import com.neobank.dto.ApiResponseDTO;
import com.neobank.dto.auth.*;
import com.neobank.service.AuthService;
import com.neobank.service.CaptchaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AuthController {

    private final AuthService    authService;
    private final CaptchaService captchaService;

    // ─────────────────────────────────────────────
    //  GET /api/auth/captcha
    //  Generate new captcha
    // ─────────────────────────────────────────────

    @GetMapping("/captcha")
    public ResponseEntity<ApiResponseDTO<CaptchaResponseDTO>> getCaptcha() {
        CaptchaResponseDTO captcha = captchaService.generate();
        return ResponseEntity.ok(ApiResponseDTO.success("Captcha generated", captcha));
    }

    // ─────────────────────────────────────────────
    //  GET /api/auth/check-username?username=john
    //  Real-time username availability check
    // ─────────────────────────────────────────────

    @GetMapping("/check-username")
    public ResponseEntity<ApiResponseDTO<UsernameCheckDTO>> checkUsername(
            @RequestParam String username
    ) {
        UsernameCheckDTO result = authService.checkUsername(username);
        return ResponseEntity.ok(ApiResponseDTO.success(result.getMessage(), result));
    }

    // ─────────────────────────────────────────────
    //  POST /api/auth/send-registration-otp
    //  Body: { email, captchaToken, captchaAnswer }
    // ─────────────────────────────────────────────

    @PostMapping("/send-registration-otp")
    public ResponseEntity<ApiResponseDTO<Void>> sendRegistrationOtp(
            @RequestBody SendRegistrationOtpRequestDTO req
    ) {
        authService.sendRegistrationOtp(req.getEmail(), req.getCaptchaToken(), req.getCaptchaAnswer());
        return ResponseEntity.ok(ApiResponseDTO.success(
            "OTP sent to " + req.getEmail() + ". Valid for 10 minutes.", null
        ));
    }

    // ─────────────────────────────────────────────
    //  POST /api/auth/register
    //  Body: { email, username, password, confirmPassword, otp }
    // ─────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<ApiResponseDTO<RegisterResponseDTO>> register(
            @RequestBody RegisterRequestDTO req
    ) {
        RegisterResponseDTO result = authService.register(req);
        return ResponseEntity.ok(ApiResponseDTO.success("Registration successful!", result));
    }

    // ─────────────────────────────────────────────
    //  POST /api/auth/login
    //  Body: { usernameOrEmail, password }
    // ─────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<ApiResponseDTO<LoginResponseDTO>> login(
            @RequestBody LoginRequestDTO req
    ) {
        LoginResponseDTO result = authService.login(req);
        return ResponseEntity.ok(ApiResponseDTO.success("Login successful!", result));
    }

    // ─────────────────────────────────────────────
    //  POST /api/auth/resend-otp
    //  Body: { email, purpose }
    // ─────────────────────────────────────────────

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponseDTO<Void>> resendOtp(
            @RequestBody ResendOtpRequestDTO req
    ) {
        authService.resendOtp(req.getEmail(), req.getPurpose());
        return ResponseEntity.ok(ApiResponseDTO.success(
            "New OTP sent to " + req.getEmail(), null
        ));
    }

    // ─────────────────────────────────────────────
    //  POST /api/auth/forgot-username
    //  Body: { email, captchaToken, captchaAnswer }
    // ─────────────────────────────────────────────

    @PostMapping("/forgot-username")
    public ResponseEntity<ApiResponseDTO<Void>> forgotUsername(
            @RequestBody ForgotUsernameRequestDTO req
    ) {
        authService.forgotUsername(req);
        return ResponseEntity.ok(ApiResponseDTO.success(
            "OTP sent to your email to recover your username.", null
        ));
    }

    // ─────────────────────────────────────────────
    //  POST /api/auth/forgot-username/verify
    //  Body: { email, otp }
    // ─────────────────────────────────────────────

    @PostMapping("/forgot-username/verify")
    public ResponseEntity<ApiResponseDTO<Void>> verifyForgotUsernameOtp(
            @RequestBody OtpRequestDTO req
    ) {
        authService.verifyForgotUsernameOtp(req);
        return ResponseEntity.ok(ApiResponseDTO.success(
            "Username has been sent to your email.", null
        ));
    }

    // ─────────────────────────────────────────────
    //  POST /api/auth/forgot-password
    //  Body: { email, captchaToken, captchaAnswer }
    // ─────────────────────────────────────────────
    
    @PostMapping("/change-password")
    public ResponseEntity<ApiResponseDTO<Void>> changePassword(
            @RequestBody ChangePasswordRequestDTO req,
            Authentication auth) {
        authService.changePassword(req, auth.getName());
        return ResponseEntity.ok(ApiResponseDTO.success("Password changed successfully", null));
    }
    
    
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponseDTO<Void>> forgotPassword(
            @RequestBody ForgotPasswordRequestDTO req
    ) {
        authService.forgotPassword(req);
        return ResponseEntity.ok(ApiResponseDTO.success(
            "OTP sent to your email to reset your password.", null
        ));
    }

    // ─────────────────────────────────────────────
    //  POST /api/auth/reset-password
    //  Body: { email, otp, newPassword, confirmPassword }
    // ─────────────────────────────────────────────

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponseDTO<Void>> resetPassword(
            @RequestBody ResetPasswordRequestDTO req
    ) {
        authService.resetPassword(req);
        return ResponseEntity.ok(ApiResponseDTO.success(
            "Password reset successfully. You can now login.", null
        ));
    }
}