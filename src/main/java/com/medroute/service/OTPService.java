package com.medroute.service;

import com.medroute.dao.OTPDAO;
import com.medroute.model.OTPPurpose;
import com.medroute.util.OTPUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class OTPService {

    private static final Logger logger = LoggerFactory.getLogger(OTPService.class);

    @Autowired
    private OTPDAO otpDAO;

    @Autowired
    private JavaMailSender mailSender;

    private static final int OTP_EXPIRY_MINUTES = 5;
    private static final int MAX_ATTEMPTS = 5;
    private static final int MAX_REQUESTS_PER_WINDOW = 3;
    private static final int RATE_LIMIT_WINDOW_MINUTES = 15;

    public enum VerificationResult {
        SUCCESS, INVALID, EXPIRED, MAX_ATTEMPTS, NO_PENDING
    }

    public boolean generateAndSend(String email, OTPPurpose purpose) {
        int recentRequests = otpDAO.countRecentRequests(email, RATE_LIMIT_WINDOW_MINUTES);
        if (recentRequests >= MAX_REQUESTS_PER_WINDOW) {
            logger.warn("OTP rate limit exceeded for email: {}", email);
            return false;
        }

        String otp = OTPUtil.generate();
        String hash = OTPUtil.hash(otp);

        otpDAO.invalidatePrevious(email, purpose.name());

        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(OTP_EXPIRY_MINUTES);
        otpDAO.create(email, hash, purpose.name(), expiresAt);

        sendOtpEmail(email, otp, purpose);

        logger.info("OTP sent to {} for {}", email, purpose.name());
        return true;
    }

    public VerificationResult verify(String email, String inputOtp, OTPPurpose purpose) {
        Optional<OTPDAO.OTPRecord> recordOpt = otpDAO.findLatestValid(email, purpose.name());

        if (recordOpt.isEmpty()) {
            return VerificationResult.NO_PENDING;
        }

        OTPDAO.OTPRecord record = recordOpt.get();

        if (record.expiresAt().isBefore(LocalDateTime.now())) {
            return VerificationResult.EXPIRED;
        }

        if (record.attempts() >= MAX_ATTEMPTS) {
            return VerificationResult.MAX_ATTEMPTS;
        }

        otpDAO.incrementAttempts(record.id());

        if (OTPUtil.verify(inputOtp, record.otpHash())) {
            otpDAO.markUsed(record.id());
            return VerificationResult.SUCCESS;
        }

        return VerificationResult.INVALID;
    }

    private void sendOtpEmail(String to, String otp, OTPPurpose purpose) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(to);
            helper.setSubject("MedRoute AI - Your Verification Code");

            String htmlBody = "<h3>MedRoute AI</h3>" +
                    "<p>Your verification code for " + purpose.name().toLowerCase() + " is:</p>" +
                    "<h2>" + otp + "</h2>" +
                    "<p>This code expires in 5 minutes.</p>" +
                    "<p>Do not share this code with anyone.</p>";

            helper.setText(htmlBody, true);
            mailSender.send(message);
        } catch (Exception e) {
            logger.error("Failed to send OTP email to {}", to, e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
}
