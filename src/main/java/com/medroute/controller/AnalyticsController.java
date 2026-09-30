package com.medroute.controller;

import com.medroute.service.AnalyticsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;
import java.util.Map;

@Controller
public class AnalyticsController {

    private final AnalyticsService analyticsService;

    public AnalyticsController(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    private Long getFacilityId(HttpSession session) {
        String role = (String) session.getAttribute("ROLE");
        if ("ADMIN".equals(role)) {
            return null; // Global view for admin
        }
        return (Long) session.getAttribute("FACILITY_ID");
    }

    @GetMapping("/analytics")
    public String getDashboard(Model model, HttpSession session) {
        if (session.getAttribute("USER_ID") == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("activePage", "analytics");
        model.addAttribute("contentPage", "/WEB-INF/views/analytics/dashboard.jsp");
        model.addAttribute("includeCharts", true);
        return "layouts/base";
    }

    @GetMapping("/api/analytics/inventory-trends")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getInventoryTrends(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session) {
        Long facilityId = getFacilityId(session);
        return ResponseEntity.ok(analyticsService.getInventoryTrends(facilityId, startDate, endDate));
    }

    @GetMapping("/api/analytics/transfer-metrics")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getTransferMetrics(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session) {
        Long facilityId = getFacilityId(session);
        return ResponseEntity.ok(analyticsService.getTransferMetrics(facilityId, startDate, endDate));
    }

    @GetMapping("/api/analytics/facility-performance")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getFacilityPerformance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session) {
        String role = (String) session.getAttribute("ROLE");
        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(analyticsService.getFacilityPerformance(startDate, endDate));
    }

    @GetMapping("/api/analytics/demand-patterns")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getDemandPatterns(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            HttpSession session) {
        Long facilityId = getFacilityId(session);
        return ResponseEntity.ok(analyticsService.getDemandPatterns(facilityId, startDate, endDate));
    }

    @GetMapping("/api/analytics/risk-heatmap")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getRiskHeatmap(HttpSession session) {
        Long facilityId = getFacilityId(session);
        return ResponseEntity.ok(analyticsService.getRiskDistribution(facilityId));
    }
}
