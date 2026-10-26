package com.fptaptech.taskmanager.web;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {
    @GetMapping("/")
    public String home(Authentication authentication, Model model) {
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getPrincipal())) {
            model.addAttribute("roles", authentication.getAuthorities());
            model.addAttribute("authenticated", true);
        } else {
            model.addAttribute("authenticated", false);
        }
        return "home";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
