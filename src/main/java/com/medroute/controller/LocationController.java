package com.medroute.controller;

import com.medroute.model.FacilityLocationDTO;
import com.medroute.model.GeocodingResult;
import com.medroute.model.TransferRequest;
import com.medroute.model.User;
import com.medroute.model.WeatherSnapshot;
import com.medroute.service.LocationService;
import com.medroute.service.TransferService;
import com.medroute.service.WeatherService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class LocationController {

    @Autowired
    private LocationService locationService;
    
    @Autowired
    private WeatherService weatherService;

    @Autowired
    private TransferService transferService;

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

    @GetMapping("/location/map")
    public String showMap(HttpSession session, Model model) {
        User user = getUser(session);
        if (user == null) {
            return "redirect:/auth/login";
        }
        
        model.addAttribute("activePage", "map");
        model.addAttribute("contentPage", "/WEB-INF/views/location/map.jsp");
        model.addAttribute("includeMap", true);
        return "layouts/base";
    }

    @PostMapping("/api/location/geocode")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> geocodeFacilityAddress(
            @RequestParam("facilityId") Long facilityId,
            @RequestParam("address") String address,
            HttpSession session) {
        
        User user = getUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Authorization: Admin can geocode any, Facility users only their own
        if (!"ADMIN".equals(user.getRole().name()) && !facilityId.equals(user.getFacilityId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Map<String, Object> response = new HashMap<>();
        
        Optional<GeocodingResult> resultOpt = locationService.geocodeAddress(address);
        if (resultOpt.isPresent()) {
            GeocodingResult result = resultOpt.get();
            boolean updated = locationService.updateFacilityCoordinates(facilityId, result.getLatitude(), result.getLongitude());
            if (updated) {
                response.put("success", true);
                response.put("latitude", result.getLatitude());
                response.put("longitude", result.getLongitude());
                response.put("message", "Address geocoded and coordinates saved successfully.");
                return ResponseEntity.ok(response);
            }
        }
        
        response.put("success", false);
        response.put("message", "Location unavailable or geocoding failed.");
        return ResponseEntity.badRequest().body(response);
    }

    @GetMapping("/api/location/nearby")
    @ResponseBody
    public ResponseEntity<List<FacilityLocationDTO>> getNearbyFacilities(
            @RequestParam("facilityId") Long facilityId,
            @RequestParam(value = "radiusKm", defaultValue = "25.0") double radiusKm,
            HttpSession session) {

        User user = getUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        List<FacilityLocationDTO> nearby = locationService.findNearbyFacilities(facilityId, radiusKm);
        return ResponseEntity.ok(nearby);
    }

    @GetMapping("/api/location/facilities")
    @ResponseBody
    public ResponseEntity<List<FacilityLocationDTO>> getAllBoundedFacilities(HttpSession session) {
        User user = getUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(locationService.findAllBoundedFacilities(200));
    }

    @GetMapping("/api/location/weather")
    @ResponseBody
    public ResponseEntity<WeatherSnapshot> getWeatherContext(
            @RequestParam("lat") Double lat,
            @RequestParam("lng") Double lng,
            HttpSession session) {
        
        User user = getUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        Optional<WeatherSnapshot> optWeather = weatherService.getWeatherContext(lat, lng);
        return optWeather.map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build());
    }

    @GetMapping("/api/location/routes")
    @ResponseBody
    public ResponseEntity<List<TransferRequest>> getActiveTransferRoutes(HttpSession session) {
        User user = getUser(session);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(transferService.findAll(0, 50, null));
    }
}
