package com.medroute.service;

import com.medroute.dao.UserDAO;
import com.medroute.model.OTPPurpose;
import com.medroute.model.Role;
import com.medroute.model.User;
import com.medroute.util.PasswordUtil;
import com.medroute.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private OTPService otpService;

    @Autowired
    private AuditService auditService;

    public static class RegisterResult {
        private boolean success;
        private String message;
        private Long userId;

        public RegisterResult(boolean success, String message, Long userId) {
            this.success = success;
            this.message = message;
            this.userId = userId;
        }

        public boolean isSuccess() { return success; }
        public String getMessage() { return message; }
        public Long getUserId() { return userId; }
    }

    public static class LoginResult {
        public enum Status { SUCCESS, INVALID_CREDENTIALS, NOT_VERIFIED, ACCOUNT_DISABLED }
        private Status status;
        private User user;
        private String message;

        public LoginResult(Status status, User user, String message) {
            this.status = status;
            this.user = user;
            this.message = message;
        }

        public Status getStatus() { return status; }
        public User getUser() { return user; }
        public String getMessage() { return message; }
    }

    public RegisterResult register(String email, String password, String fullName,
                                   String phone, String roleStr, Long facilityId,
                                   HttpServletRequest request) {
        
        email = ValidationUtil.sanitize(email);
        fullName = ValidationUtil.sanitize(fullName);
        phone = ValidationUtil.sanitize(phone);

        if (!ValidationUtil.isValidEmail(email)) return new RegisterResult(false, "Invalid email format", null);
        if (!ValidationUtil.isValidPhone(phone)) return new RegisterResult(false, "Invalid phone format", null);
        if (!ValidationUtil.isValidName(fullName)) return new RegisterResult(false, "Invalid full name", null);
        if (!ValidationUtil.isValidRole(roleStr)) return new RegisterResult(false, "Invalid role", null);

        if (userDAO.existsByEmail(email)) {
            return new RegisterResult(false, "Email is already registered", null);
        }

        String policyViolation = PasswordUtil.getPolicyViolation(password);
        if (policyViolation != null) {
            return new RegisterResult(false, policyViolation, null);
        }

        String hash = PasswordUtil.hash(password);

        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(hash);
        user.setFullName(fullName);
        user.setPhone(phone);
        user.setRole(Role.valueOf(roleStr.toUpperCase()));
        user.setFacilityId(facilityId);
        user.setActive(true);
        user.setVerified(false);

        Long userId = userDAO.create(user);
        
        boolean otpSent = otpService.generateAndSend(email, OTPPurpose.REGISTRATION);
        
        auditService.logAction(userId, "USER_REGISTERED", "USER", String.valueOf(userId), request);

        if (!otpSent) {
            return new RegisterResult(true, "Registration successful, but OTP rate limited. Please request a new OTP later.", userId);
        }
        
        return new RegisterResult(true, "Registration successful. Please verify your email.", userId);
    }

    public LoginResult login(String email, String password, HttpServletRequest request) {
        if (email == null) {
            return new LoginResult(LoginResult.Status.INVALID_CREDENTIALS, null, "Invalid credentials");
        }
        
        Optional<User> userOpt = userDAO.findByEmail(email.trim());
        if (userOpt.isEmpty()) {
            return new LoginResult(LoginResult.Status.INVALID_CREDENTIALS, null, "Invalid credentials");
        }

        User user = userOpt.get();

        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            auditService.logAction(user.getId(), "LOGIN_FAILED", "USER", String.valueOf(user.getId()), request);
            return new LoginResult(LoginResult.Status.INVALID_CREDENTIALS, null, "Invalid credentials");
        }

        if (!user.isVerified()) {
            return new LoginResult(LoginResult.Status.NOT_VERIFIED, user, "Account not verified");
        }

        if (!user.isActive()) {
            return new LoginResult(LoginResult.Status.ACCOUNT_DISABLED, user, "Account is disabled");
        }

        auditService.logAction(user.getId(), "LOGIN_SUCCESS", "USER", String.valueOf(user.getId()), request);
        return new LoginResult(LoginResult.Status.SUCCESS, user, "Login successful");
    }
}
