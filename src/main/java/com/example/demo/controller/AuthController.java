package com.example.demo.controller;

import com.example.demo.dto.*;
import com.example.demo.model.User;
import com.example.demo.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final BCryptPasswordEncoder passwordEncoder;

    // Cookie security behavior configurable via env (default true in prod)
    @Value("${app.cookie.secure:true}")
    private boolean cookieSecure;

    @Value("${app.cookie.domain:#{null}}")
    private String cookieDomain;

    public AuthController(UserService userService, BCryptPasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    @PostMapping("/register")
    public ResponseEntity<RegisterResponse> register(@Valid @RequestBody RegisterRequest request) {
        RegisterResponse response = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
        try {
            // ✅ FIXED: userService.login() NOW RETURNS JwtResponse
            JwtResponse jwtResponse = userService.login(loginRequest);
            
            // Create HttpOnly cookie (server-managed session)
            Cookie jwtCookie = new Cookie("jwtToken", jwtResponse.getToken());
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(cookieSecure); // true in production; false allowed in dev
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(86400); // 24 hours
            if (cookieDomain != null && !cookieDomain.isBlank()) {
                jwtCookie.setDomain(cookieDomain);
            }
            response.addCookie(jwtCookie);

            // Also add explicit Set-Cookie header (robustness across servlet impls)
            String sameSite = cookieSecure ? "None" : "Lax";
            String domainPart = (cookieDomain != null && !cookieDomain.isBlank()) ? "; Domain=" + cookieDomain : "";
            response.setHeader("Set-Cookie",
                    String.format("jwtToken=%s; HttpOnly; Path=/; Max-Age=86400; Secure=%s; SameSite=%s%s",
                            jwtResponse.getToken(),  // ✅ Use from JwtResponse
                            cookieSecure ? "true" : "false",
                            sameSite,
                            domainPart
                    )
            );

            // ✅ NOW RETURNS userId IN RESPONSE!
            return ResponseEntity.ok(jwtResponse);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new MessageResponse(e.getMessage()));
        }
    }

    /**
     * Logout endpoint - clears the HttpOnly cookie by sending a deletion Set-Cookie.
     * Frontend MUST call with credentials: 'include' to allow browser to send cookie and accept deletion header.
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(HttpServletResponse response) {
        // Build deletion cookie (same name, path, domain; Max-Age=0)
        Cookie cookie = new Cookie("jwtToken", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(cookieSecure);
        cookie.setPath("/");
        cookie.setMaxAge(0); // delete immediately
        if (cookieDomain != null && !cookieDomain.isBlank()) {
            cookie.setDomain(cookieDomain);
        }
        response.addCookie(cookie);

        // Explicit header as well
        String sameSite = cookieSecure ? "None" : "Lax";
        String domainPart = (cookieDomain != null && !cookieDomain.isBlank()) ? "; Domain=" + cookieDomain : "";
        response.setHeader("Set-Cookie",
                String.format("jwtToken=; HttpOnly; Path=/; Max-Age=0; Secure=%s; SameSite=%s%s",
                        cookieSecure ? "true" : "false",
                        sameSite,
                        domainPart
                )
        );

        return ResponseEntity.ok(new MessageResponse("Logged out"));
    }

    // ---------- OTP PASSWORD RESET ----------

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@RequestBody java.util.Map<String, String> body) {
        try {
            String email = body.get("email");
            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(new MessageResponse("Email required"));
            }
            String msg = userService.forgotPassword(email);
            return ResponseEntity.ok(new MessageResponse(msg));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@RequestBody java.util.Map<String, String> body) {
        try {
            String email = body.get("email");
            String otp = body.get("otp");
            String newPassword = body.get("newPassword");
            if (email == null || otp == null || newPassword == null || email.isBlank() || otp.isBlank() || newPassword.isBlank()) {
                return ResponseEntity.badRequest().body(new MessageResponse("email, otp, newPassword are required"));
            }
            String msg = userService.resetPassword(email, otp, newPassword);
            return ResponseEntity.ok(new MessageResponse(msg));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new MessageResponse(e.getMessage()));
        }
    }
}