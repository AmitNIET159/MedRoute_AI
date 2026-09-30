package com.medroute.controller;

import com.medroute.dao.AIDAO;
import com.medroute.dao.FacilityDAO;
import com.medroute.model.AIInsight;
import com.medroute.model.DemandAnalysis;
import com.medroute.model.Facility;
import com.medroute.service.AIService;
import com.medroute.service.DemandService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/ai")
public class AIController {

    private final AIService aiService;
    private final DemandService demandService;
    private final AIDAO aiDAO;
    private final FacilityDAO facilityDAO;

    @Autowired
    public AIController(AIService aiService, DemandService demandService, AIDAO aiDAO, FacilityDAO facilityDAO) {
        this.aiService = aiService;
        this.demandService = demandService;
        this.aiDAO = aiDAO;
        this.facilityDAO = facilityDAO;
    }

    private Long getAuthorizedFacilityId(HttpSession session) {
        String role = (String) session.getAttribute("ROLE");
        if ("ADMIN".equals(role)) {
            List<Facility> facilities = facilityDAO.findActiveBounded(1);
            if (facilities != null && !facilities.isEmpty()) {
                return facilities.get(0).getId();
            }
            return 1L; // Fallback for admin
        }
        return (Long) session.getAttribute("FACILITY_ID");
    }

    @GetMapping("/insights")
    public String getInsightsList(HttpSession session, Model model) {
        Long facilityId = getAuthorizedFacilityId(session);
        if (facilityId == null) return "redirect:/auth/login";

        List<AIInsight> insights = aiDAO.findByFacility(facilityId, 50);
        model.addAttribute("insights", insights);
        model.addAttribute("activePage", "ai-insights");
        model.addAttribute("contentPage", "/WEB-INF/views/ai/insights.jsp");
        return "layouts/base";
    }

    @PostMapping("/analyze")
    @ResponseBody
    public ResponseEntity<AIInsight> analyzeMedicine(@RequestBody Map<String, Object> payload, HttpSession session) {
        Long facilityId = getAuthorizedFacilityId(session);
        Long userId = (Long) session.getAttribute("USER_ID");
        
        if (facilityId == null || userId == null) {
            return ResponseEntity.status(401).build();
        }

        Object medicineIdObj = payload.get("medicineId");
        if (medicineIdObj == null) {
            return ResponseEntity.badRequest().build();
        }
        
        Long medicineId;
        try {
            medicineId = Long.valueOf(medicineIdObj.toString());
        } catch (NumberFormatException e) {
            return ResponseEntity.badRequest().build();
        }

        try {
            DemandAnalysis analysis = demandService.analyzeMedicine(facilityId, medicineId);
            AIInsight insight = aiService.getInsight(facilityId, medicineId, analysis, userId);
            return ResponseEntity.ok(insight);
        } catch (Exception e) {
            AIInsight err = new AIInsight();
            err.setTitle("Unable to generate insight. Please try again later.");
            err.setContent(e.getMessage());
            return ResponseEntity.status(500).body(err);
        }
    }
}
