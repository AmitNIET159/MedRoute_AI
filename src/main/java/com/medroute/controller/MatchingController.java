package com.medroute.controller;

import com.medroute.model.EmergencyRequest;
import com.medroute.model.MatchCandidate;
import com.medroute.model.RequestStatus;
import com.medroute.model.User;
import com.medroute.service.EmergencyRequestService;
import com.medroute.service.MatchingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;

@Controller
@RequestMapping("/matching")
public class MatchingController {

    private static final Logger logger = LoggerFactory.getLogger(MatchingController.class);

    @Autowired
    private MatchingService matchingService;

    @Autowired
    private EmergencyRequestService emergencyRequestService;

    @Autowired
    private com.medroute.dao.FacilityDAO facilityDAO;

    private User getUserFromSession(HttpSession session) {
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId == null) return null;
        String roleStr = (String) session.getAttribute("ROLE");
        Long facilityId = (Long) session.getAttribute("FACILITY_ID");
        if ("ADMIN".equals(roleStr) && facilityId == null) {
            java.util.List<com.medroute.model.Facility> facilities = facilityDAO.findActiveBounded(1);
            if (facilities != null && !facilities.isEmpty()) {
                facilityId = facilities.get(0).getId();
            }
        }
        User user = new User();
        user.setId(userId);
        user.setRole(com.medroute.model.Role.valueOf(roleStr));
        user.setFacilityId(facilityId);
        return user;
    }

    /**
     * GET /matching/request/{requestId} — View matching candidates for an emergency request.
     * Only the request owner or ADMIN can view results.
     */
    @GetMapping("/request/{requestId}")
    public String viewMatchingResults(@PathVariable("requestId") Long requestId,
                                     @RequestParam(value = "radius", defaultValue = "50") double radius,
                                     HttpSession session, Model model) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/auth/login";

        // Load request
        EmergencyRequest request = emergencyRequestService.findById(requestId).orElse(null);
        if (request == null) {
            model.addAttribute("error", "Request not found.");
            model.addAttribute("activePage", "requests");
            model.addAttribute("contentPage", "/WEB-INF/views/requests/list.jsp");
            return "layouts/base";
        }

        // Authorization: admin or own facility
        boolean isAdmin = "ADMIN".equals(user.getRole().name());
        if (!isAdmin && !request.getRequestingFacilityId().equals(user.getFacilityId())) {
            model.addAttribute("error", "Not authorized to view matching for this request.");
            model.addAttribute("activePage", "requests");
            model.addAttribute("contentPage", "/WEB-INF/views/requests/list.jsp");
            return "layouts/base";
        }

        // Only match OPEN or PARTIALLY_FULFILLED requests
        boolean canMatch = (request.getStatus() == RequestStatus.OPEN ||
                            request.getStatus() == RequestStatus.PARTIALLY_FULFILLED);

        List<MatchCandidate> candidates = List.of();
        if (canMatch) {
            try {
                candidates = matchingService.findCandidates(requestId, radius);
            } catch (Exception e) {
                logger.error("Matching failed for request {}: {}", requestId, e.getMessage());
                model.addAttribute("error", "Matching engine encountered an error. Please try again.");
            }
        }

        model.addAttribute("request", request);
        model.addAttribute("candidates", candidates);
        model.addAttribute("canMatch", canMatch);
        model.addAttribute("searchRadius", radius);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("activePage", "requests");
        model.addAttribute("contentPage", "/WEB-INF/views/matching/results.jsp");

        return "layouts/base";
    }
}
