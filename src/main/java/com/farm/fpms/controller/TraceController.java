package com.farm.fpms.controller;

import com.farm.fpms.common.StatusLabel;
import com.farm.fpms.service.TraceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import javax.servlet.http.HttpServletRequest;

@Controller
public class TraceController {

    private final TraceService traceService;

    public TraceController(TraceService traceService) {
        this.traceService = traceService;
    }

    @GetMapping("/trace/{code}")
    public String trace(@PathVariable String code, HttpServletRequest request, Model model) {
        String traceUrl = traceService.traceUrl(request, code);
        model.addAttribute("trace", traceService.publicTrace(code));
        model.addAttribute("code", code);
        model.addAttribute("traceUrl", traceUrl);
        model.addAttribute("qrDataUri", traceService.generateQrDataUri(request, code));
        model.addAttribute("label", new StatusLabel());
        return "trace";
    }
}
