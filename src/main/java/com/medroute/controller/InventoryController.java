package com.medroute.controller;

import com.medroute.model.InventoryBatch;
import com.medroute.model.InventoryTransaction;
import com.medroute.model.Role;
import com.medroute.service.InventoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/inventory")
public class InventoryController {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private com.medroute.dao.FacilityDAO facilityDAO;

    private Long getFacilityIdFromSession(HttpSession session, Long requestedFacilityId) {
        String roleStr = (String) session.getAttribute("ROLE");
        Long sessionFacilityId = (Long) session.getAttribute("FACILITY_ID");
        
        if (Role.ADMIN.name().equals(roleStr)) {
            if (requestedFacilityId != null) return requestedFacilityId;
            if (sessionFacilityId != null) return sessionFacilityId;
            // ADMIN has no facility_id — resolve dynamically
            java.util.List<com.medroute.model.Facility> facilities = facilityDAO.findActiveBounded(1);
            if (facilities != null && !facilities.isEmpty()) {
                return facilities.get(0).getId();
            }
            return null;
        }
        return sessionFacilityId;
    }

    private Long getUserId(HttpSession session) {
        return (Long) session.getAttribute("USER_ID");
    }

    private boolean isAuthenticated(HttpSession session) {
        return session != null && Boolean.TRUE.equals(session.getAttribute("AUTHENTICATED"));
    }

    @GetMapping({"", "/"})
    public org.springframework.web.servlet.ModelAndView getInventoryPage(HttpSession session) {
        if (!isAuthenticated(session)) {
            return new org.springframework.web.servlet.ModelAndView("redirect:/auth/login");
        }
        
        org.springframework.web.servlet.ModelAndView mav = new org.springframework.web.servlet.ModelAndView("layouts/base");
        mav.addObject("activePage", "inventory");
        mav.addObject("contentPage", "/WEB-INF/views/inventory/list.jsp");
        mav.addObject("includeCharts", false);
        
        Long facilityId = getFacilityIdFromSession(session, null);
        if (facilityId != null) {
            List<InventoryBatch> batches = inventoryService.findByFacilityId(facilityId, 0, 50, null, null, null, null);
            mav.addObject("inventoryList", batches);
        }
        return mav;
    }

    @GetMapping("/dashboard")
    public ResponseEntity<?> getDashboardStats(HttpSession session, @RequestParam(required = false) Long facilityId) {
        if (!isAuthenticated(session)) return ResponseEntity.status(401).body("Unauthorized");
        
        Long targetFacilityId = getFacilityIdFromSession(session, facilityId);
        if (targetFacilityId == null) {
            return ResponseEntity.badRequest().body("Facility ID required for ADMIN");
        }
        
        Map<String, Object> stats = inventoryService.getDashboardStats(targetFacilityId);
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/list")
    public ResponseEntity<?> listInventory(HttpSession session,
                                           @RequestParam(required = false) Long facilityId,
                                           @RequestParam(defaultValue = "0") int offset,
                                           @RequestParam(defaultValue = "50") int limit,
                                           @RequestParam(required = false) String search,
                                           @RequestParam(required = false) Long categoryId,
                                           @RequestParam(required = false) String status,
                                           @RequestParam(required = false) String expiryRange) {
        if (!isAuthenticated(session)) return ResponseEntity.status(401).body("Unauthorized");

        Long targetFacilityId = getFacilityIdFromSession(session, facilityId);
        if (targetFacilityId == null) {
             return ResponseEntity.badRequest().body("Facility ID required");
        }

        List<InventoryBatch> batches = inventoryService.findByFacilityId(targetFacilityId, offset, limit, search, categoryId, status, expiryRange);
        int total = inventoryService.countByFacilityId(targetFacilityId, search, categoryId, status, expiryRange);

        Map<String, Object> response = new HashMap<>();
        response.put("data", batches);
        response.put("total", total);
        response.put("offset", offset);
        response.put("limit", limit);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBatchDetails(@PathVariable Long id, HttpSession session) {
        if (!isAuthenticated(session)) return ResponseEntity.status(401).body("Unauthorized");

        InventoryBatch batch = inventoryService.findById(id);
        if (batch == null) return ResponseEntity.notFound().build();

        String role = (String) session.getAttribute("ROLE");
        Long sessionFacilityId = (Long) session.getAttribute("FACILITY_ID");
        if (!Role.ADMIN.name().equals(role) && !batch.getFacilityId().equals(sessionFacilityId)) {
            return ResponseEntity.status(403).body("Forbidden");
        }

        List<InventoryTransaction> transactions = inventoryService.getTransactionsForBatch(id);

        Map<String, Object> response = new HashMap<>();
        response.put("batch", batch);
        response.put("transactions", transactions);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/stock-in")
    public ResponseEntity<?> stockIn(@RequestBody InventoryBatch batch, HttpSession session, HttpServletRequest request) {
        if (!isAuthenticated(session)) return ResponseEntity.status(401).body("Unauthorized");

        Long targetFacilityId = getFacilityIdFromSession(session, batch.getFacilityId());
        batch.setFacilityId(targetFacilityId);
        
        try {
            inventoryService.stockIn(batch, getUserId(session), request);
            return ResponseEntity.ok("Stock-in successful");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to stock in");
        }
    }

    @PostMapping("/consume")
    public ResponseEntity<?> consumeStock(@RequestParam Long medicineId,
                                          @RequestParam int quantity,
                                          @RequestParam String department,
                                          @RequestParam(required = false) Long facilityId,
                                          HttpSession session, HttpServletRequest request) {
        if (!isAuthenticated(session)) return ResponseEntity.status(401).body("Unauthorized");

        Long targetFacilityId = getFacilityIdFromSession(session, facilityId);
        if (targetFacilityId == null) {
            return ResponseEntity.badRequest().body("Facility ID required");
        }

        try {
            inventoryService.consumeStock(targetFacilityId, medicineId, quantity, department, getUserId(session), request);
            return ResponseEntity.ok("Consumption recorded successfully");
        } catch (IllegalArgumentException | IllegalStateException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to consume stock");
        }
    }

    @PostMapping("/adjust")
    public ResponseEntity<?> adjustStock(@RequestParam Long batchId,
                                         @RequestParam int newQuantity,
                                         @RequestParam(required = false) String notes,
                                         @RequestParam(required = false) Long facilityId,
                                         HttpSession session, HttpServletRequest request) {
        if (!isAuthenticated(session)) return ResponseEntity.status(401).body("Unauthorized");

        Long targetFacilityId = getFacilityIdFromSession(session, facilityId);
        
        try {
            inventoryService.adjustStock(batchId, targetFacilityId, newQuantity, getUserId(session), notes, request);
            return ResponseEntity.ok("Adjustment successful");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Failed to adjust stock");
        }
    }

    @GetMapping("/expiry")
    public ResponseEntity<?> getExpiringStock(HttpSession session,
                                              @RequestParam(required = false) Long facilityId) {
        if (!isAuthenticated(session)) return ResponseEntity.status(401).body("Unauthorized");
        Long targetFacilityId = getFacilityIdFromSession(session, facilityId);
        if (targetFacilityId == null) {
            return ResponseEntity.badRequest().body("Facility ID required");
        }
        return ResponseEntity.ok(inventoryService.findByFacilityId(targetFacilityId, 0, 1000, null, null, null, "EXPIRING_SOON"));
    }
}
