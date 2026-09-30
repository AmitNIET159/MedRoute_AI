package com.medroute.controller;

import com.medroute.model.TransferRequest;
import com.medroute.model.TransferStatus;
import com.medroute.model.User;
import com.medroute.service.TransferService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/transfers")
public class TransferController {

    private static final Logger logger = LoggerFactory.getLogger(TransferController.class);
    private static final int PAGE_SIZE = 10;

    private final TransferService transferService;
    private final com.medroute.dao.FacilityDAO facilityDAO;

    public TransferController(TransferService transferService, com.medroute.dao.FacilityDAO facilityDAO) {
        this.transferService = transferService;
        this.facilityDAO = facilityDAO;
    }

    private User getUserFromSession(HttpSession session) {
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId == null) userId = (Long) session.getAttribute("userId");
        if (userId == null) return null;
        String roleStr = (String) session.getAttribute("ROLE");
        if (roleStr == null) roleStr = (String) session.getAttribute("userRole");
        Long facilityId = (Long) session.getAttribute("FACILITY_ID");
        if (facilityId == null) facilityId = (Long) session.getAttribute("facilityId");
        // ADMIN with null facilityId — resolve dynamically
        if ("ADMIN".equals(roleStr) && facilityId == null) {
            java.util.List<com.medroute.model.Facility> facilities = facilityDAO.findActiveBounded(1);
            if (facilities != null && !facilities.isEmpty()) {
                facilityId = facilities.get(0).getId();
            }
        }
        User user = new User();
        user.setId(userId);
        if (roleStr != null) {
            try {
                user.setRole(com.medroute.model.Role.valueOf(roleStr));
            } catch (Exception ignored) {}
        }
        user.setFacilityId(facilityId);
        return user;
    }

    @GetMapping
    public String listTransfers(@RequestParam(defaultValue = "incoming") String tab,
                                @RequestParam(required = false) String status,
                                @RequestParam(defaultValue = "1") int page,
                                HttpSession session, Model model) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/auth/login";

        Long facilityId = user.getFacilityId();
        boolean isAdmin = user.getRole() != null && "ADMIN".equals(user.getRole().name());

        int offset = (page - 1) * PAGE_SIZE;
        String statusFilter = status;

        List<TransferRequest> transfers;
        int totalRecords;

        if (isAdmin && ("all".equals(tab) || facilityId == null)) {
            tab = "all";
            transfers = transferService.findAll(offset, PAGE_SIZE, statusFilter);
            totalRecords = transferService.countAll(statusFilter);
        } else if ("outgoing".equals(tab)) {
            transfers = transferService.findOutgoing(facilityId, offset, PAGE_SIZE, statusFilter);
            totalRecords = transferService.countOutgoing(facilityId, statusFilter);
        } else {
            // Default: incoming (transfers where I am the donor, i.e. from_facility_id = my facility)
            transfers = transferService.findIncoming(facilityId, offset, PAGE_SIZE, statusFilter);
            totalRecords = transferService.countIncoming(facilityId, statusFilter);
        }

        int totalPages = Math.max(1, (int) Math.ceil((double) totalRecords / PAGE_SIZE));

        model.addAttribute("transfers", transfers);
        model.addAttribute("currentTab", tab);
        model.addAttribute("currentStatus", statusFilter);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", totalPages);
        model.addAttribute("statuses", TransferStatus.values());
        model.addAttribute("activePage", "transfers");
        model.addAttribute("contentPage", "/WEB-INF/views/transfers/list.jsp");
        model.addAttribute("includeCharts", false);

        return "layouts/base";
    }

    @GetMapping("/{id}")
    public String transferDetail(@PathVariable Long id, HttpSession session, Model model) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/auth/login";

        Optional<TransferRequest> opt = transferService.findById(id);
        if (opt.isEmpty()) return "redirect:/transfers";

        TransferRequest transfer = opt.get();
        Long facilityId = user.getFacilityId();
        boolean isAdmin = user.getRole() != null && "ADMIN".equals(user.getRole().name());

        if (!isAdmin && (facilityId == null || (!facilityId.equals(transfer.getFromFacilityId())
                     && !facilityId.equals(transfer.getToFacilityId())))) {
            return "redirect:/transfers";
        }

        model.addAttribute("transfer", transfer);
        model.addAttribute("activePage", "transfers");
        model.addAttribute("contentPage", "/WEB-INF/views/transfers/detail.jsp");
        model.addAttribute("includeCharts", false);
        return "layouts/base";
    }

    @PostMapping("/create")
    public String createTransfer(@RequestParam Long emergencyRequestId,
                                 @RequestParam Long fromFacilityId,
                                 @RequestParam int quantity,
                                 @RequestParam BigDecimal matchScore,
                                 @RequestParam BigDecimal distanceKm,
                                 HttpSession session,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/auth/login";

        if (quantity <= 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Quantity must be greater than zero");
            return "redirect:/matching/request/" + emergencyRequestId;
        }

        try {
            TransferRequest created = transferService.createTransfer(
                    fromFacilityId, emergencyRequestId, quantity,
                    matchScore, distanceKm, user.getId(), user.getFacilityId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Transfer request created successfully");
            return "redirect:/transfers/" + created.getId();
        } catch (Exception e) {
            logger.error("Failed to create transfer: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/matching/request/" + emergencyRequestId;
        }
    }

    @PostMapping("/{id}/accept")
    public String acceptTransfer(@PathVariable Long id, HttpSession session,
                                 HttpServletRequest request, RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/auth/login";

        try {
            transferService.acceptTransfer(id, user.getId(), user.getFacilityId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Transfer accepted — stock reserved");
        } catch (Exception e) {
            logger.error("Failed to accept transfer {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/transfers/" + id;
    }

    @PostMapping("/{id}/reject")
    public String rejectTransfer(@PathVariable Long id, HttpSession session,
                                 HttpServletRequest request, RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/auth/login";

        try {
            transferService.rejectTransfer(id, user.getId(), user.getFacilityId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Transfer rejected");
        } catch (Exception e) {
            logger.error("Failed to reject transfer {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/transfers/" + id;
    }

    @PostMapping("/{id}/schedule")
    public String scheduleTransfer(@PathVariable Long id,
                                   @RequestParam(required = false) String notes,
                                   HttpSession session, HttpServletRequest request,
                                   RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/auth/login";

        try {
            transferService.scheduleTransfer(id, notes, user.getId(), user.getFacilityId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Transfer scheduled");
        } catch (Exception e) {
            logger.error("Failed to schedule transfer {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/transfers/" + id;
    }

    @PostMapping("/{id}/dispatch")
    public String dispatchTransfer(@PathVariable Long id, HttpSession session,
                                   HttpServletRequest request, RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/auth/login";

        try {
            transferService.dispatchTransfer(id, user.getId(), user.getFacilityId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Transfer dispatched — stock in transit");
        } catch (Exception e) {
            logger.error("Failed to dispatch transfer {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/transfers/" + id;
    }

    @PostMapping("/{id}/receive")
    public String receiveTransfer(@PathVariable Long id, HttpSession session,
                                  HttpServletRequest request, RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/auth/login";

        try {
            transferService.receiveTransfer(id, user.getId(), user.getFacilityId(), request);
            redirectAttributes.addFlashAttribute("successMessage", "Transfer received and completed");
        } catch (Exception e) {
            logger.error("Failed to receive transfer {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/transfers/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancelTransfer(@PathVariable Long id, HttpSession session,
                                 HttpServletRequest request, RedirectAttributes redirectAttributes) {
        User user = getUserFromSession(session);
        if (user == null) return "redirect:/auth/login";

        boolean isAdmin = user.getRole() != null && "ADMIN".equals(user.getRole().name());

        try {
            transferService.cancelTransfer(id, user.getId(), user.getFacilityId(), isAdmin, request);
            redirectAttributes.addFlashAttribute("successMessage", "Transfer cancelled");
        } catch (Exception e) {
            logger.error("Failed to cancel transfer {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/transfers/" + id;
    }
}
