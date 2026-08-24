package com.director_appraisal.auth_user_service.service;

import com.director_appraisal.auth_user_service.model.PasswordResetToken;
import com.director_appraisal.auth_user_service.model.User;
import com.director_appraisal.auth_user_service.repository.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class UserService implements UserDetailsService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository resetTokenRepository;
    private final UserAdministrativePostRepository userAdministrativePostRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String frontendUrl;

    public UserService(UserRepository userRepository, 
                       PasswordResetTokenRepository resetTokenRepository,
                       UserAdministrativePostRepository userAdministrativePostRepository,
                       @Lazy PasswordEncoder passwordEncoder,
                       EmailService emailService,
                       @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.userRepository = userRepository;
        this.resetTokenRepository = resetTokenRepository;
        this.userAdministrativePostRepository = userAdministrativePostRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        if (username == null || username.isBlank()) {
            throw new UsernameNotFoundException("Email cannot be empty");
        }
        return userRepository.findByEmailIgnoreCase(username.trim())
                .filter(u -> !Boolean.TRUE.equals(u.getDeleted()))
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + username));
    }

    public Optional<User> findByEmail(String email) {
        if (email == null || email.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByEmailIgnoreCase(email.trim())
                .filter(u -> !Boolean.TRUE.equals(u.getDeleted()));
    }


    public List<User> findAllUsers() {
        return userRepository.findAll().stream()
                .filter(u -> !Boolean.TRUE.equals(u.getDeleted()))
                .collect(java.util.stream.Collectors.toList());
    }

    public List<User> findByUniversityId(Long universityId) {
        if (universityId == null) {
            return List.of();
        }
        return userRepository.findByUniversityId(universityId).stream()
                .filter(u -> !Boolean.TRUE.equals(u.getDeleted()))
                .toList();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    @Transactional
    public User createUser(User user) {
        String trimmedEmail = user.getEmail() != null ? user.getEmail().trim().toLowerCase() : "";
        user.setEmail(trimmedEmail);

        Optional<User> existingOpt = userRepository.findByEmail(trimmedEmail);
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            if (Boolean.TRUE.equals(existing.getDeleted())) {
                if (existing.getId() != null) {
                    userAdministrativePostRepository.deleteByUserId(existing.getId());
                }
                resetTokenRepository.deleteByEmail(trimmedEmail);
                userRepository.delete(existing);
                userRepository.flush();
            } else {
                throw new IllegalArgumentException("User with email " + user.getEmail() + " already exists.");
            }
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        return userRepository.save(user);
    }

    @Transactional
    public User createOrUpdateLeadership(User user, String rawPassword) {
        String trimmedEmail = user.getEmail() != null ? user.getEmail().trim().toLowerCase() : "";
        user.setEmail(trimmedEmail);

        Optional<User> existingOpt = userRepository.findByEmailIgnoreCase(trimmedEmail);
        if (existingOpt.isPresent()) {
            User existing = existingOpt.get();
            existing.setName(user.getName());
            existing.setRole(user.getRole());
            existing.setDesignation(user.getDesignation());
            existing.setSchool("Root");
            existing.setAccountType("reviewer");
            existing.setCategory("all");
            existing.setStatus("active");
            existing.setDeleted(false);
            if (user.getUniversityId() != null) {
                existing.setUniversityId(user.getUniversityId());
            }
            if (user.getUniversityCode() != null && !user.getUniversityCode().isBlank()) {
                existing.setUniversityCode(user.getUniversityCode());
            }
            if (rawPassword != null && !rawPassword.isBlank()) {
                existing.setPassword(passwordEncoder.encode(rawPassword));
            }
            return userRepository.save(existing);
        } else {
            if (rawPassword != null && !rawPassword.isBlank()) {
                user.setPassword(passwordEncoder.encode(rawPassword));
            }
            user.setSchool("Root");
            user.setAccountType("reviewer");
            user.setCategory("all");
            user.setStatus("active");
            user.setDeleted(false);
            return userRepository.save(user);
        }
    }

    @Transactional
    public void deleteUser(User user) {
        if (user == null) {
            return;
        }

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            resetTokenRepository.deleteByEmail(user.getEmail().trim().toLowerCase());
        }

        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        user.setStatus("deleted");
        userRepository.save(user);
        userRepository.flush();
    }

    @Transactional
    public User updateUser(User user, String rawPassword) {
        if (rawPassword != null && !rawPassword.isBlank()) {
            user.setPassword(passwordEncoder.encode(rawPassword));
        }
        return userRepository.save(user);
    }

    public boolean checkPassword(String rawPassword, String encodedPassword) {
        return passwordEncoder.matches(rawPassword, encodedPassword);
    }

    @Transactional
    public String createPasswordResetToken(String email) {
        String trimmedEmail = email.trim().toLowerCase();
        User user = userRepository.findByEmail(trimmedEmail)
                .orElseThrow(() -> new IllegalArgumentException("User not found with email: " + trimmedEmail));

        resetTokenRepository.deleteByEmail(trimmedEmail);

        String rawToken = UUID.randomUUID().toString().replace("-", "");
        String tokenHash = hashSha256(rawToken);
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(1);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .email(trimmedEmail)
                .tokenHash(tokenHash)
                .expiresAt(expiresAt)
                .used(false)
                .createdAt(LocalDateTime.now())
                .build();

        resetTokenRepository.save(resetToken);

        String resetLink = frontendUrl + "/reset-password?token=" + rawToken;
        String emailText = "Dear User,\n\n"
                + "You requested a password reset for your Faculty Appraisal Account.\n"
                + "Please click the link below to reset your password. This link is valid for 1 hour:\n\n"
                + resetLink + "\n\n"
                + "If you did not request this, please ignore this email.\n\n"
                + "Regards,\n"
                + "Faculty Appraisal Team";

        try {
            emailService.sendEmail(trimmedEmail, "Password Reset Request", emailText);
            System.out.println("Password reset email successfully sent to: " + trimmedEmail);
        } catch (Exception e) {
            System.err.println("Failed to send password reset email to: " + trimmedEmail + ". Error: " + e.getMessage());
            System.out.println("=================================================");
            System.out.println("PASSWORD RESET REQUEST INITIATED (EMAIL FAILED)");
            System.out.println("Email: " + trimmedEmail);
            System.out.println("Raw Token: " + rawToken);
            System.out.println("=================================================");
        }

        return rawToken;
    }

    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        String tokenHash = hashSha256(rawToken);

        PasswordResetToken resetToken = resetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new IllegalArgumentException("Invalid, expired, or already used reset token."));

        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Invalid, expired, or already used reset token.");
        }

        User user = userRepository.findByEmail(resetToken.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("User associated with this token not found."));

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        resetTokenRepository.save(resetToken);
    }

    private String hashSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error hashing token", e);
        }
    }
}
