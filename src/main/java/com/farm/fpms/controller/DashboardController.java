package com.farm.fpms.controller;

import com.farm.fpms.service.DashboardService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.Map;

@Controller
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        Map<String, Object> overview = dashboardService.overview();
        for (Map.Entry<String, Object> entry : overview.entrySet()) {
            model.addAttribute(entry.getKey(), entry.getValue());
        }
        return "dashboard";
    }
}
