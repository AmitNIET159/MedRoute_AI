package com.medroute.controller;

import com.medroute.dao.FacilityDAO;
import com.medroute.dao.UserDAO;
import com.medroute.model.Facility;
import com.medroute.model.OTPPurpose;
import com.medroute.model.Role;
import com.medroute.model.User;
import com.medroute.service.AuditService;
import com.medroute.service.AuthService;
import com.medroute.service.OTPService;
import com.medroute.util.CsrfUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/auth")
public class AuthController {
    
    @Autowired private AuthService authService;
    @Autowired private OTPService otpService;
    @Autowired private AuditService auditService;
    @Autowired private FacilityDAO facilityDAO;
    @Autowired private UserDAO userDAO;
    
    private static final Logger logger = LoggerFactory.getLogger(AuthController.class);

    @GetMapping("/login")
    public String showLogin(Model model, HttpServletRequest request,
                           @RequestParam(required = false) String expired,
                           @RequestParam(required = false) String error,
                           @RequestParam(required = false) String logout,
                           @RequestParam(required = false) String verified) {
        HttpSession session = request.getSession(false);
        if (session != null && Boolean.TRUE.equals(session.getAttribute("AUTHENTICATED"))) {
            return "redirect:/demand/dashboard";
        }
        
        if (expired != null) model.addAttribute("message", "Session expired. Please log in again.");
        if (error != null) model.addAttribute("error", "Invalid credentials.");
        if (logout != null) model.addAttribute("message", "You have been logged out successfully.");
        if (verified != null) model.addAttribute("message", "Email verified successfully. You can now log in.");
        
        String csrfToken = CsrfUtil.generateAndStoreToken(request.getSession(true));
        model.addAttribute("csrfToken", csrfToken);
        
        return "auth/login";
    }

    @PostMapping("/login")
    public String processLogin(@RequestParam String email, @RequestParam String password,
                               @RequestParam String csrfToken,
                               HttpServletRequest request, Model model) {
        
        if (!CsrfUtil.validateToken(request, csrfToken)) {
            model.addAttribute("error", "Invalid form submission. Please try again.");
            return "auth/login";
        }

        AuthService.LoginResult result = authService.login(email, password, request);

        if (result.getStatus() == AuthService.LoginResult.Status.SUCCESS) {
            User user = result.getUser();
            
            // Invalidate old session to prevent session fixation
            HttpSession oldSession = request.getSession(false);
            if (oldSession != null) {
                oldSession.invalidate();
            }
            
            HttpSession newSession = request.getSession(true);
            newSession.setAttribute("USER_ID", user.getId());
            newSession.setAttribute("userId", user.getId());
            newSession.setAttribute("ROLE", user.getRole().name());
            newSession.setAttribute("userRole", user.getRole().name());
            newSession.setAttribute("FACILITY_ID", user.getFacilityId());
            newSession.setAttribute("facilityId", user.getFacilityId());
            newSession.setAttribute("AUTHENTICATED", true);
            newSession.setAttribute("USER_NAME", user.getFullName());
            newSession.setAttribute("userName", user.getFullName());
            newSession.setAttribute("USER_EMAIL", user.getEmail());
            newSession.setAttribute("userEmail", user.getEmail());
            newSession.setAttribute("user", user);
            
            return "redirect:/demand/dashboard";
        } else if (result.getStatus() == AuthService.LoginResult.Status.NOT_VERIFIED) {
            HttpSession session = request.getSession(true);
            session.setAttribute("PENDING_VERIFICATION_EMAIL", email);
            session.setAttribute("PENDING_VERIFICATION_PURPOSE", OTPPurpose.REGISTRATION.name());
            return "redirect:/auth/verify-otp";
        } else {
            model.addAttribute("error", result.getMessage());
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String showRegister(Model model, HttpServletRequest request) {
        List<Facility> facilities = facilityDAO.findAllActive();
        model.addAttribute("facilities", facilities);
        
        List<String> roles = Arrays.stream(Role.values())
                .filter(r -> r != Role.ADMIN)
                .map(Enum::name)
                .collect(Collectors.toList());
        model.addAttribute("roles", roles);
        
        String csrfToken = CsrfUtil.generateAndStoreToken(request.getSession(true));
        model.addAttribute("csrfToken", csrfToken);
        
        return "auth/register";
    }

    @PostMapping("/register")
    public String processRegister(@RequestParam String email, @RequestParam String password,
                                   @RequestParam String confirmPassword,
                                   @RequestParam String fullName, @RequestParam String phone,
                                   @RequestParam String role, @RequestParam Long facilityId,
                                   @RequestParam String csrfToken,
                                   HttpServletRequest request, Model model) {
        
        if (!CsrfUtil.validateToken(request, csrfToken)) {
            return populateRegisterModelAndReturnError(model, request, "Invalid form submission.", email, fullName, phone, role, facilityId);
        }

        if (!password.equals(confirmPassword)) {
            return populateRegisterModelAndReturnError(model, request, "Passwords do not match.", email, fullName, phone, role, facilityId);
        }

        AuthService.RegisterResult result = authService.register(email, password, fullName, phone, role, facilityId, request);
        
        if (result.isSuccess()) {
            HttpSession session = request.getSession(true);
            session.setAttribute("PENDING_VERIFICATION_EMAIL", email);
            session.setAttribute("PENDING_VERIFICATION_PURPOSE", OTPPurpose.REGISTRATION.name());
            return "redirect:/auth/verify-otp";
        } else {
            return populateRegisterModelAndReturnError(model, request, result.getMessage(), email, fullName, phone, role, facilityId);
        }
    }
    
    private String populateRegisterModelAndReturnError(Model model, HttpServletRequest request, String error, 
                                                       String email, String fullName, String phone, String role, Long facilityId) {
        model.addAttribute("error", error);
        model.addAttribute("email", email);
        model.addAttribute("fullName", fullName);
        model.addAttribute("phone", phone);
        model.addAttribute("selectedRole", role);
        model.addAttribute("selectedFacilityId", facilityId);
        
        model.addAttribute("facilities", facilityDAO.findAllActive());
        model.addAttribute("roles", Arrays.stream(Role.values()).filter(r -> r != Role.ADMIN).map(Enum::name).collect(Collectors.toList()));
        model.addAttribute("csrfToken", CsrfUtil.generateAndStoreToken(request.getSession(true)));
        return "auth/register";
    }

    @GetMapping("/verify-otp")
    public String showOtpVerification(HttpSession session, Model model, HttpServletRequest request) {
        String email = (String) session.getAttribute("PENDING_VERIFICATION_EMAIL");
        if (email == null) {
            return "redirect:/auth/register";
        }
        
        String csrfToken = CsrfUtil.generateAndStoreToken(request.getSession(true));
        model.addAttribute("csrfToken", csrfToken);
        model.addAttribute("email", email);
        
        return "auth/otp";
    }

    @PostMapping("/verify-otp")
    public String verifyOtp(@RequestParam String otp, @RequestParam String csrfToken,
                            HttpSession session, HttpServletRequest request, Model model) {
        
        String email = (String) session.getAttribute("PENDING_VERIFICATION_EMAIL");
        String purposeStr = (String) session.getAttribute("PENDING_VERIFICATION_PURPOSE");
        
        if (email == null || purposeStr == null) {
            return "redirect:/auth/register";
        }
        
        if (!CsrfUtil.validateToken(request, csrfToken)) {
            model.addAttribute("error", "Invalid form submission.");
            return "auth/otp";
        }
        
        OTPPurpose purpose = OTPPurpose.valueOf(purposeStr);
        OTPService.VerificationResult result = otpService.verify(email, otp, purpose);
        
        if (result == OTPService.VerificationResult.SUCCESS) {
            if (purpose == OTPPurpose.REGISTRATION) {
                Optional<User> userOpt = userDAO.findByEmail(email);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    userDAO.updateVerified(user.getId(), true);
                    auditService.logAction(user.getId(), "EMAIL_VERIFIED", "USER", String.valueOf(user.getId()), request);
                }
                session.removeAttribute("PENDING_VERIFICATION_EMAIL");
                session.removeAttribute("PENDING_VERIFICATION_PURPOSE");
                return "redirect:/auth/login?verified=true";
            }
            // For other purposes like PASSWORD_RESET, handle appropriately
            return "redirect:/auth/login";
        } else if (result == OTPService.VerificationResult.INVALID) {
            model.addAttribute("error", "Invalid verification code.");
        } else if (result == OTPService.VerificationResult.EXPIRED) {
            model.addAttribute("error", "Code expired. Please request a new one.");
        } else if (result == OTPService.VerificationResult.MAX_ATTEMPTS) {
            model.addAttribute("error", "Too many failed attempts.");
        } else if (result == OTPService.VerificationResult.NO_PENDING) {
            return "redirect:/auth/register";
        }
        
        return "auth/otp";
    }

    @PostMapping("/resend-otp")
    @ResponseBody
    public Map<String, Object> resendOtp(HttpSession session) {
        Map<String, Object> response = new HashMap<>();
        String email = (String) session.getAttribute("PENDING_VERIFICATION_EMAIL");
        String purposeStr = (String) session.getAttribute("PENDING_VERIFICATION_PURPOSE");
        
        if (email == null || purposeStr == null) {
            response.put("success", false);
            response.put("message", "Session expired. Please restart the process.");
            return response;
        }
        
        boolean sent = otpService.generateAndSend(email, OTPPurpose.valueOf(purposeStr));
        if (sent) {
            response.put("success", true);
            response.put("message", "A new verification code has been sent to your email.");
        } else {
            response.put("success", false);
            response.put("message", "Please wait before requesting another code.");
        }
        return response;
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request, HttpSession session) {
        if (session != null) {
            Long userId = (Long) session.getAttribute("USER_ID");
            if (userId != null) {
                auditService.logAction(userId, "LOGOUT", "USER", String.valueOf(userId), request);
            }
            session.invalidate();
        }
        return "redirect:/auth/login?logout=true";
    }
}
