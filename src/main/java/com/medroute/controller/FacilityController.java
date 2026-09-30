package com.medroute.controller;

import com.medroute.model.Facility;
import com.medroute.model.User;
import com.medroute.service.AuditService;
import com.medroute.service.FacilityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/facilities")
public class FacilityController {

    @Autowired
    private FacilityService facilityService;

    @Autowired
    private AuditService auditService;

    private User getUser(HttpSession session) {
        if (session == null) return null;
        User user = (User) session.getAttribute("user");
        if (user != null) return user;
        Long userId = (Long) session.getAttribute("USER_ID");
        if (userId == null) userId = (Long) session.getAttribute("userId");
        if (userId == null) return null;
        String roleStr = (String) session.getAttribute("ROLE");
        if (roleStr == null) roleStr = (String) session.getAttribute("userRole");
        Long facilityId = (Long) session.getAttribute("FACILITY_ID");
        if (facilityId == null) facilityId = (Long) session.getAttribute("facilityId");
        user = new User();
        user.setId(userId);
        if (roleStr != null) {
            try {
                user.setRole(com.medroute.model.Role.valueOf(roleStr));
            } catch (Exception ignored) {}
        }
        user.setFacilityId(facilityId);
        session.setAttribute("user", user);
        return user;
    }

    @GetMapping
    public String listFacilities(@RequestParam(defaultValue = "1") int page, Model model) {
        int limit = 20;
        int offset = (page - 1) * limit;
        List<Facility> facilities = facilityService.getAllFacilities(offset, limit);
        int total = facilityService.countAllFacilities();
        
        model.addAttribute("facilities", facilities);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", (int) Math.ceil((double) total / limit));
        model.addAttribute("activePage", "facilities");
        model.addAttribute("contentPage", "/WEB-INF/views/facility/list.jsp");
        
        return "layouts/base";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("facility", new Facility());
        model.addAttribute("activePage", "facilities");
        model.addAttribute("contentPage", "/WEB-INF/views/facility/form.jsp");
        return "layouts/base";
    }

    @PostMapping("/new")
    public String createFacility(@ModelAttribute Facility facility, HttpSession session, HttpServletRequest request) {
        User currentUser = getUser(session);
        facilityService.createFacility(facility, currentUser);
        if (currentUser != null) {
            auditService.logAction(currentUser.getId(), "CREATE", "FACILITY", null, request);
        }
        return "redirect:/facilities";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Optional<Facility> facility = facilityService.getFacilityById(id);
        if (facility.isPresent()) {
            model.addAttribute("facility", facility.get());
            model.addAttribute("activePage", "facilities");
            model.addAttribute("contentPage", "/WEB-INF/views/facility/form.jsp");
            return "layouts/base";
        }
        return "redirect:/facilities";
    }

    @PostMapping("/edit/{id}")
    public String updateFacility(@PathVariable Long id, @ModelAttribute Facility facility, HttpSession session, HttpServletRequest request) {
        User currentUser = getUser(session);
        facility.setId(id);
        facilityService.updateFacility(facility, currentUser);
        if (currentUser != null) {
            auditService.logAction(currentUser.getId(), "UPDATE", "FACILITY", String.valueOf(id), request);
        }
        return "redirect:/facilities";
    }

    @GetMapping("/{id}")
    public String viewFacility(@PathVariable Long id, Model model) {
        Optional<Facility> facility = facilityService.getFacilityById(id);
        if (facility.isPresent()) {
            model.addAttribute("facility", facility.get());
            model.addAttribute("activePage", "facilities");
            model.addAttribute("contentPage", "/WEB-INF/views/facility/detail.jsp");
            return "layouts/base";
        }
        return "redirect:/facilities";
    }
}
