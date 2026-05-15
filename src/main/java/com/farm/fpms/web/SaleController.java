package com.farm.fpms.web;

import com.farm.fpms.service.SaleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class SaleController {

    private final SaleService saleService;
    private final JdbcTemplate jdbcTemplate;

    public SaleController(SaleService saleService, JdbcTemplate jdbcTemplate) {
        this.saleService = saleService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/sales")
    public String list(Model model) {
        model.addAttribute("sales", saleService.listAll());
        model.addAttribute("batches", jdbcTemplate.queryForList(
                "select id, batch_no from plant_batch order by id desc"));
        return "sales";
    }

    @PostMapping("/sales")
    public String create(@RequestParam(required = false) Long batchId,
                         @RequestParam String customerName,
                         @RequestParam String productName,
                         @RequestParam BigDecimal quantityKg,
                         @RequestParam BigDecimal unitPrice,
                         @RequestParam String saleDate,
                         @RequestParam(required = false) String note,
                         RedirectAttributes redirect) {
        saleService.create(batchId, customerName, productName, quantityKg, unitPrice,
                LocalDate.parse(saleDate), note);
        redirect.addFlashAttribute("notice", "销售记录已保存");
        return "redirect:/sales";
    }
}
