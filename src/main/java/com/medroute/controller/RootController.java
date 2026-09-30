package com.medroute.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import javax.servlet.http.HttpSession;

@Controller
public class RootController {

    @GetMapping({"/", "/dashboard", "/home"})
    public String rootRedirect(HttpSession session) {
        if (session != null && Boolean.TRUE.equals(session.getAttribute("AUTHENTICATED"))) {
            return "redirect:/demand/dashboard";
        }
        return "redirect:/auth/login";
    }
}
