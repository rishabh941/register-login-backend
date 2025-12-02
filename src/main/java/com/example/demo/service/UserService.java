package com.example.demo.service;

import com.example.demo.dto.JwtResponse;  // ✅ ADD THIS IMPORT
import com.example.demo.dto.LoginRequest;
import com.example.demo.dto.RegisterRequest;
import com.example.demo.dto.RegisterResponse;
import com.example.demo.model.User;
import com.example.demo.repository.UserRepository;
import com.example.demo.security.JwtUtils;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.security.SecureRandom;
import java.util.UUID;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtUtils jwtUtils;
    private final EmailService emailService;

    private static final DateTimeFormatter ISO_FORMATTER =
            DateTimeFormatter.ISO_INSTANT.withZone(ZoneOffset.UTC);

    public UserService(UserRepository userRepository, JwtUtils jwtUtils, EmailService emailService) {
        this.userRepository = userRepository;
        this.jwtUtils = jwtUtils;
        this.emailService = emailService;
    }

    // Add to UserService class
    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
    }

    public RegisterResponse register(RegisterRequest request) {

        // Duplicate checks
        if (userRepository.existsByEmail(request.getPersonalInfo().getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        if (userRepository.existsByUsername(request.getAccount().getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        String now = ISO_FORMATTER.format(Instant.now());

        User user = new User();
        user.setUserId(UUID.randomUUID().toString());

        // --- PERSONAL INFO ---
        user.setEmail(request.getPersonalInfo().getEmail());
        user.setFirstName(request.getPersonalInfo().getFirstName());
        user.setLastName(request.getPersonalInfo().getLastName());
        user.setPhone(request.getPersonalInfo().getPhone());
        user.setDateOfBirth(request.getPersonalInfo().getDateOfBirth());

        // --- ACCOUNT INFO ---
        user.setUsername(request.getAccount().getUsername());
        user.setPasswordHash(passwordEncoder.encode(request.getAccount().getPassword()));

        // --- INVESTMENT INFO ---
        user.setRiskAppetite(request.getInvestmentProfile().getRiskAppetite());
        user.setExperience(request.getInvestmentProfile().getExperience());
        user.setInvestmentGoal(request.getInvestmentProfile().getInvestmentGoal());

        // --- META INFO ---
        user.setCreatedAt(now);
        user.setUpdatedAt(now);

        userRepository.save(user);

        return new RegisterResponse(
                user.getUserId(),
                "User registered successfully"
        );
    }

    // ✅ FIXED: login() NOW RETURNS JwtResponse WITH userId
    public JwtResponse login(LoginRequest loginRequest) {
        User user = userRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPasswordHash())) {
            throw new RuntimeException("Invalid credentials");
        }

        String jwt = jwtUtils.generateJwtToken(user.getEmail());
        
        // ✅ RETURN COMPLETE JwtResponse WITH userId
        return new JwtResponse(jwt, user.getEmail(), user.getRole(), user.getUserId());
    }

    // ---------------- OTP PASSWORD RESET ----------------

    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        String otp = generateOtp();
        String expiry = ISO_FORMATTER.format(Instant.now().plusSeconds(10 * 60)); // 10 minutes
        user.setOtp(otp);
        user.setOtpExpiry(expiry);
        user.setUpdatedAt(ISO_FORMATTER.format(Instant.now()));
        userRepository.save(user);

        // Send email via SMTP
        emailService.sendOtp(email, otp);

        return "OTP sent to email!";
    }

    public String resetPassword(String email, String otp, String newPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (user.getOtp() == null || user.getOtp().isBlank()) {
            throw new RuntimeException("No OTP requested");
        }
        if (!user.getOtp().equals(otp)) {
            throw new RuntimeException("Invalid OTP");
        }
        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBlank()) {
            throw new RuntimeException("OTP expired");
        }
        Instant expiryInstant = Instant.parse(user.getOtpExpiry());
        if (Instant.now().isAfter(expiryInstant)) {
            throw new RuntimeException("OTP expired");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setOtp("");
        user.setOtpExpiry("");
        user.setUpdatedAt(ISO_FORMATTER.format(Instant.now()));
        userRepository.save(user);
        return "Password changed successfully!";
    }

    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int value = random.nextInt(900000) + 100000; // ensures 100000-999999
        return String.valueOf(value);
    }
}