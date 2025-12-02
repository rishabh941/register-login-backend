package com.example.demo.model;

public class User {
    private String userId;
    private String email;
    private String username;
    private String passwordHash;
    private String role = "USER";  // New field - default USER

    private String firstName;
    private String lastName;
    private String phone;
    private String dateOfBirth;

    private String riskAppetite;
    private String experience;
    private String investmentGoal;

    private String createdAt;
    private String updatedAt;

    // OTP password reset (simple flow)
    private String otp;          // 6-digit code
    private String otpExpiry;    // ISO-8601 instant string

    // ---- Getters & Setters (NO Lombok) ----
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }

    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getDateOfBirth() { return dateOfBirth; }
    public void setDateOfBirth(String dateOfBirth) { this.dateOfBirth = dateOfBirth; }

    public String getRiskAppetite() { return riskAppetite; }
    public void setRiskAppetite(String riskAppetite) { this.riskAppetite = riskAppetite; }

    public String getExperience() { return experience; }
    public void setExperience(String experience) { this.experience = experience; }

    public String getInvestmentGoal() { return investmentGoal; }
    public void setInvestmentGoal(String investmentGoal) { this.investmentGoal = investmentGoal; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }

    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }

    public String getOtpExpiry() { return otpExpiry; }
    public void setOtpExpiry(String otpExpiry) { this.otpExpiry = otpExpiry; }
}