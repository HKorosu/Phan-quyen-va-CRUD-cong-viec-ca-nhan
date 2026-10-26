package com.fptaptech.taskmanager.web;

import com.fptaptech.taskmanager.admin.AdminDashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final AdminDashboardService dashboard;

    public AdminController(AdminDashboardService dashboard) {
        this.dashboard = dashboard;
    }

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(defaultValue = "0") int page, Model model) {
        model.addAttribute("dashboard", dashboard.dashboard(page));
        return "admin/dashboard";
    }
}
