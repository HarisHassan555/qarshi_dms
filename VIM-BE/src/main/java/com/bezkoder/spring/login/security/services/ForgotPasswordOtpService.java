package com.bezkoder.spring.login.security.services;

import com.bezkoder.spring.login.admin.bll.services.IUserService;
import com.bezkoder.spring.login.admin.bll.servicesimpl.EmailService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import com.bezkoder.spring.login.backgroundjob.BackgroundJobService;
import com.bezkoder.spring.login.backgroundjob.dto.ForgotPasswordOtpEmailJobPayload;
import com.bezkoder.spring.login.repository.UserRepository;
import com.bezkoder.spring.login.util.UtilRandomKey;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ForgotPasswordOtpService {

    private static final long OTP_TTL_MS = 10 * 60 * 1000L;
    private static final long VERIFIED_TTL_MS = 15 * 60 * 1000L;

    private final UserRepository userRepository;
    private final EmailService emailService;
    private final IUserService userService;
    private final BackgroundJobService backgroundJobService;
    private final Map<String, OtpEntry> otpByEmail = new ConcurrentHashMap<>();

    public ForgotPasswordOtpService(
            UserRepository userRepository,
            EmailService emailService,
            IUserService userService,
            BackgroundJobService backgroundJobService
    ) {
        this.userRepository = userRepository;
        this.emailService = emailService;
        this.userService = userService;
        this.backgroundJobService = backgroundJobService;
    }

    public Map<String, Object> requestOtp(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (email.isEmpty()) {
            return failure("Email is required.");
        }

        List<CfgTblUser> matches = userRepository.findActiveUsersByEmailAddress(email);
        if (matches == null || matches.isEmpty()) {
            return failure("No account is registered with this email address.");
        }

        CfgTblUser user = matches.stream()
                .filter(candidate -> Boolean.TRUE.equals(candidate.getBlIsActive()) && Boolean.TRUE.equals(candidate.getBlnStatus()))
                .findFirst()
                .orElse(matches.get(0));

        String otp = UtilRandomKey.getRandomOrderNumber();
        long now = System.currentTimeMillis();
        otpByEmail.put(email, new OtpEntry(otp, user.getSerUserId(), now + OTP_TTL_MS, false));

        String displayName = user.getTxtUserName() != null ? user.getTxtUserName() : user.getEffectiveLoginName();
        ForgotPasswordOtpEmailJobPayload mailPayload = new ForgotPasswordOtpEmailJobPayload();
        mailPayload.setRecipientEmail(email);
        mailPayload.setDisplayName(displayName != null ? displayName : "User");
        mailPayload.setOtp(otp);
        if (backgroundJobService.isEnabled()) {
            backgroundJobService.enqueueForgotPasswordOtpEmail(mailPayload);
        } else {
            String body = "Hello " + mailPayload.getDisplayName() + ",\n\n"
                    + "Your password reset OTP is: " + otp + "\n"
                    + "This code expires in 10 minutes.\n\n"
                    + "If you did not request this, you can ignore this email.\n";
            emailService.sendEmail(Collections.singletonList(email), "Password reset OTP", body);
        }

        Map<String, Object> ok = new HashMap<>(success("OTP has been sent to your email address."));
        ok.put("maskedEmail", maskEmail(email));
        return ok;
    }

    public Map<String, Object> verifyOtp(String rawEmail, String rawOtp) {
        String email = normalizeEmail(rawEmail);
        String otp = String.valueOf(rawOtp != null ? rawOtp : "").trim();
        if (email.isEmpty() || otp.isEmpty()) {
            return failure("Email and OTP are required.");
        }

        OtpEntry entry = otpByEmail.get(email);
        if (entry == null || entry.expiresAt < System.currentTimeMillis()) {
            otpByEmail.remove(email);
            return failure("OTP has expired. Please request a new code.");
        }
        if (!entry.otp.equals(otp)) {
            return failure("Invalid OTP. Please try again.");
        }

        entry.verified = true;
        entry.expiresAt = System.currentTimeMillis() + VERIFIED_TTL_MS;
        otpByEmail.put(email, entry);
        return success("OTP verified. You can set a new password.");
    }

    public Map<String, Object> resetPassword(String rawEmail, String rawOtp, String newPassword) {
        String email = normalizeEmail(rawEmail);
        String otp = String.valueOf(rawOtp != null ? rawOtp : "").trim();
        String password = String.valueOf(newPassword != null ? newPassword : "").trim();

        if (email.isEmpty() || otp.isEmpty() || password.isEmpty()) {
            return failure("Email, OTP, and new password are required.");
        }
        if (password.length() < 4) {
            return failure("Password must be at least 4 characters.");
        }

        OtpEntry entry = otpByEmail.get(email);
        if (entry == null || entry.expiresAt < System.currentTimeMillis()) {
            otpByEmail.remove(email);
            return failure("OTP has expired. Please start again.");
        }
        if (!entry.verified || !entry.otp.equals(otp)) {
            return failure("OTP is not verified. Please verify the code first.");
        }

        String updateResult = userService.userPasswordUpdate(entry.userId, password, null);
        if (!"Success".equalsIgnoreCase(String.valueOf(updateResult))) {
            return failure("Could not update password. Please try again.");
        }

        otpByEmail.remove(email);
        return success("Password updated successfully. You can sign in with your new password.");
    }

    private static String normalizeEmail(String value) {
        return String.valueOf(value != null ? value : "").trim().toLowerCase(Locale.ROOT);
    }

    private static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return email;
        }
        return email.charAt(0) + "***" + email.substring(at);
    }

    private static Map<String, Object> success(String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "Success");
        payload.put("message", message);
        return payload;
    }

    private static Map<String, Object> failure(String message) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("status", "Failure");
        payload.put("message", message);
        return payload;
    }

    private static final class OtpEntry {
        private final String otp;
        private final int userId;
        private long expiresAt;
        private boolean verified;

        private OtpEntry(String otp, int userId, long expiresAt, boolean verified) {
            this.otp = otp;
            this.userId = userId;
            this.expiresAt = expiresAt;
            this.verified = verified;
        }
    }
}