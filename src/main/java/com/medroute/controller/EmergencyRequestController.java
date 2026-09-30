package com.medroute.controller;

import com.medroute.dao.MedicineDAO;
import com.medroute.model.EmergencyRequest;
import com.medroute.model.Medicine;
import com.medroute.model.RequestStatus;
import com.medroute.model.Urgency;
import com.medroute.model.User;
import com.medroute.service.EmergencyRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

@Controller
@RequestMapping("/requests")
public class EmergencyRequestController {

    private static final Logger logger = LoggerFactory.getLogger(EmergencyRequestController.class);
    private static final int PAGE_SIZE = 20;

    @Autowired
    private EmergencyRequestService emergencyRequestService;

    @Autowired
    private MedicineDAO medicineDAO;

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
     * GET /requests — List emergency requests.
     * ADMIN sees all; facility users see only their facility's requests.
     */
    @GetMapping
    public String listRequests(@RequestParam(value = "status", required = false) String statusFilter,
                               @RequestParam(value = "page", defaultValue = "1") int page,
                               HttpSession session, Model model) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/auth/login";

        int offset = Math.max(0, (page - 1) * PAGE_SIZE);
        String role = user.getRole().name();
        boolean isAdmin = "ADMIN".equals(role);

        List<EmergencyRequest> requests;
        int totalCount;

        if (isAdmin) {
            requests = emergencyRequestService.findAll(offset, PAGE_SIZE, statusFilter);
            totalCount = emergencyRequestService.countAll(statusFilter);
        } else {
            Long facilityId = user.getFacilityId();
            if (facilityId == null) {
                model.addAttribute("error", "No facility assigned to your account.");
                model.addAttribute("requests", List.of());
                model.addAttribute("activePage", "requests");
                return "requests/list";
            }
            requests = emergencyRequestService.findByFacility(facilityId, offset, PAGE_SIZE, statusFilter);
            totalCount = emergencyRequestService.countByFacility(facilityId, statusFilter);
        }

        int totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);

        model.addAttribute("requests", requests);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("statusFilter", statusFilter);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("activePage", "requests");
        model.addAttribute("contentPage", "/WEB-INF/views/requests/list.jsp");

        return "layouts/base";
    }

    /**
     * GET /requests/new — Show request creation form.
     */
    @GetMapping("/new")
    public String showCreateForm(HttpSession session, Model model) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/auth/login";

        if (user.getFacilityId() == null) {
            model.addAttribute("error", "No facility assigned. Cannot create requests.");
            model.addAttribute("activePage", "requests");
            model.addAttribute("contentPage", "/WEB-INF/views/requests/list.jsp");
            return "layouts/base";
        }

        List<Medicine> medicines = medicineDAO.findAll(0, 200, null, null);
        model.addAttribute("medicines", medicines);
        model.addAttribute("urgencyValues", Urgency.values());
        model.addAttribute("activePage", "requests");
        model.addAttribute("contentPage", "/WEB-INF/views/requests/form.jsp");

        return "layouts/base";
    }

    /**
     * POST /requests — Create new emergency request.
     * Facility is derived from authenticated session.
     */
    @PostMapping
    public String createRequest(@RequestParam("medicineId") Long medicineId,
                                @RequestParam("quantityNeeded") int quantityNeeded,
                                @RequestParam("urgency") String urgencyStr,
                                @RequestParam("requiredByDate") String requiredByDateStr,
                                @RequestParam(value = "reason", required = false) String reason,
                                HttpSession session,
                                HttpServletRequest httpReq,
                                RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/auth/login";

        try {
            Urgency urgency = Urgency.valueOf(urgencyStr);
            LocalDate requiredByDate;
            try {
                requiredByDate = LocalDate.parse(requiredByDateStr);
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Invalid date format");
            }

            EmergencyRequest request = new EmergencyRequest();
            request.setMedicineId(medicineId);
            request.setQuantityNeeded(quantityNeeded);
            request.setUrgency(urgency);
            request.setRequiredByDate(requiredByDate);
            request.setReason(reason);

            EmergencyRequest created = emergencyRequestService.createRequest(
                    request, user.getId(), user.getFacilityId(), httpReq);

            redirectAttributes.addFlashAttribute("success",
                    "Emergency request #" + created.getId() + " created successfully.");
            return "redirect:/requests/" + created.getId();

        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/requests/new";
        }
    }

    /**
     * GET /requests/{id} — View request detail.
     */
    @GetMapping("/{id}")
    public String viewRequest(@PathVariable("id") Long id,
                              HttpSession session, Model model) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/auth/login";

        EmergencyRequest request = emergencyRequestService.findById(id)
                .orElse(null);

        if (request == null) {
            model.addAttribute("error", "Request not found.");
            model.addAttribute("activePage", "requests");
            model.addAttribute("contentPage", "/WEB-INF/views/requests/list.jsp");
            return "layouts/base";
        }

        // Authorization: admin or own facility
        boolean isAdmin = "ADMIN".equals(user.getRole().name());
        if (!isAdmin && !request.getRequestingFacilityId().equals(user.getFacilityId())) {
            model.addAttribute("error", "Not authorized to view this request.");
            model.addAttribute("activePage", "requests");
            model.addAttribute("contentPage", "/WEB-INF/views/requests/list.jsp");
            return "layouts/base";
        }

        boolean canCancel = (request.getStatus() == RequestStatus.OPEN ||
                             request.getStatus() == RequestStatus.PARTIALLY_FULFILLED);
        boolean canMatch = (request.getStatus() == RequestStatus.OPEN ||
                            request.getStatus() == RequestStatus.PARTIALLY_FULFILLED);

        model.addAttribute("request", request);
        model.addAttribute("canCancel", canCancel);
        model.addAttribute("canMatch", canMatch);
        model.addAttribute("isAdmin", isAdmin);
        model.addAttribute("activePage", "requests");
        model.addAttribute("contentPage", "/WEB-INF/views/requests/detail.jsp");

        return "layouts/base";
    }

    /**
     * POST /requests/{id}/cancel — Cancel a request.
     */
    @PostMapping("/{id}/cancel")
    public String cancelRequest(@PathVariable("id") Long id,
                                HttpSession session,
                                HttpServletRequest httpReq,
                                RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/auth/login";

        try {
            boolean isAdmin = "ADMIN".equals(user.getRole().name());
            emergencyRequestService.cancelRequest(id, user.getId(), user.getFacilityId(),
                    isAdmin, httpReq);
            redirectAttributes.addFlashAttribute("success", "Request #" + id + " cancelled.");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", "Not authorized to cancel this request.");
        } catch (IllegalStateException | IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/requests/" + id;
    }
}
