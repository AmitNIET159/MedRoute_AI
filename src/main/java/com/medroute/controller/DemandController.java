package com.medroute.controller;

import com.medroute.model.DemandAnalysis;
import com.medroute.model.Medicine;
import com.medroute.service.DemandService;
import com.medroute.dao.MedicineDAO;
import com.medroute.dao.FacilityDAO;
import com.medroute.model.Facility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/demand")
public class DemandController {

    private final DemandService demandService;
    private final MedicineDAO medicineDAO;
    private final FacilityDAO facilityDAO;

    @Autowired
    public DemandController(DemandService demandService, MedicineDAO medicineDAO, FacilityDAO facilityDAO) {
        this.demandService = demandService;
        this.medicineDAO = medicineDAO;
        this.facilityDAO = facilityDAO;
    }

    private Long getAuthorizedFacilityId(HttpSession session) {
        String role = (String) session.getAttribute("ROLE");
        if ("ADMIN".equals(role)) {
            // Admin default view: might need facility selection in future
            // For now, return the first active facility dynamically instead of hardcoding 1L
            List<Facility> facilities = facilityDAO.findActiveBounded(1);
            if (facilities != null && !facilities.isEmpty()) {
                return facilities.get(0).getId();
            }
            return 1L; 
        }
        return (Long) session.getAttribute("FACILITY_ID");
    }

    @GetMapping("/dashboard")
    public String getDashboard(HttpSession session, Model model) {
        Long facilityId = getAuthorizedFacilityId(session);
        if (facilityId == null) {
            return "redirect:/auth/login";
        }

        List<Medicine> allMedicines = medicineDAO.findAll(0, 1000, null, null);
        List<DemandAnalysis> analyses = new ArrayList<>();
        
        int countCritical = 0, countHigh = 0, countModerate = 0, countLow = 0, countNoData = 0;

        for (Medicine m : allMedicines) {
            try {
                DemandAnalysis analysis = demandService.analyzeMedicine(facilityId, m.getId());
                analyses.add(analysis);
                
                if ("NO_DATA".equals(analysis.getMetrics().getDataStatus())) {
                    countNoData++;
                } else {
                    switch (analysis.getRiskScore().getRiskLevel()) {
                        case "CRITICAL": countCritical++; break;
                        case "HIGH": countHigh++; break;
                        case "MODERATE": countModerate++; break;
                        case "LOW": countLow++; break;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                // Ignore missing medicine/facility in analysis
            }
        }
        
        analyses.sort((a, b) -> Integer.compare(b.getRiskScore().getFinalScore(), a.getRiskScore().getFinalScore()));

        model.addAttribute("analyses", analyses);
        model.addAttribute("countTotal", allMedicines.size());
        model.addAttribute("countCritical", countCritical);
        model.addAttribute("countHigh", countHigh);
        model.addAttribute("countModerate", countModerate);
        model.addAttribute("countLow", countLow);
        model.addAttribute("countNoData", countNoData);
        
        // JSON string for charts
        Map<String, Integer> riskData = new HashMap<>();
        riskData.put("CRITICAL", countCritical);
        riskData.put("HIGH", countHigh);
        riskData.put("MODERATE", countModerate);
        riskData.put("LOW", countLow);
        
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            model.addAttribute("riskDataJson", mapper.writeValueAsString(riskData));
        } catch (Exception e) {
            model.addAttribute("riskDataJson", "{}");
        }

        model.addAttribute("activePage", "demand-dashboard");
        model.addAttribute("contentPage", "/WEB-INF/views/demand/dashboard.jsp");
        model.addAttribute("includeCharts", true);
        return "layouts/base";
    }

    @GetMapping("/analysis/{medicineId}")
    @ResponseBody
    public ResponseEntity<DemandAnalysis> getAnalysis(@PathVariable Long medicineId, HttpSession session) {
        Long facilityId = getAuthorizedFacilityId(session);
        if (facilityId == null) {
            return ResponseEntity.status(401).build();
        }
        
        try {
            DemandAnalysis analysis = demandService.analyzeMedicine(facilityId, medicineId);
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
}
