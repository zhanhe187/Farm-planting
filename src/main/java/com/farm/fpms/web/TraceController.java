package com.farm.fpms.web;

import com.farm.fpms.service.TraceService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class TraceController {

    private final TraceService traceService;

    public TraceController(TraceService traceService) {
        this.traceService = traceService;
    }

    @GetMapping("/trace/{code}")
    public String trace(@PathVariable String code, Model model) {
        model.addAttribute("trace", traceService.publicTrace(code));
        model.addAttribute("code", code);
        model.addAttribute("qrDataUri", traceService.generateQrDataUri(code));
        return "trace";
    }
}
