package com.farm.fpms.controller;

import com.farm.fpms.common.RoleAccessPolicy;
import com.farm.fpms.common.SessionUser;
import com.farm.fpms.common.BusinessException;
import com.farm.fpms.service.SaleService;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class SaleController {

    private final SaleService saleService;
    private final JdbcTemplate jdbcTemplate;
    private final RoleAccessPolicy roleAccessPolicy = new RoleAccessPolicy();

    public SaleController(SaleService saleService, JdbcTemplate jdbcTemplate) {
        this.saleService = saleService;
        this.jdbcTemplate = jdbcTemplate;
    }

    @GetMapping("/sales")
    public String list(Model model, HttpSession session) {
        SessionUser user = currentUser(session);
        model.addAttribute("sales", saleService.listForUser(user));
        model.addAttribute("batches", jdbcTemplate.queryForList(
                "select b.id, b.batch_no, c.name crop_name, c.sale_price_per_kg from plant_batch b " +
                        "join farm_crop c on c.id = b.crop_id order by b.id desc"));
        model.addAttribute("canCreateSales", roleAccessPolicy.canCreateSales(user));
        return "sales";
    }

    @GetMapping("/market")
    public String market(Model model, HttpSession session) {
        SessionUser user = currentUser(session);
        model.addAttribute("crops", saleService.listMarketCrops());
        model.addAttribute("sales", saleService.listForUser(user));
        return "market";
    }

    @PostMapping("/market/orders")
    public String purchase(@RequestParam long cropId,
                           @RequestParam BigDecimal quantityKg,
                           HttpSession session,
                           RedirectAttributes redirect) {
        try {
            saleService.purchaseCrop(currentUser(session), cropId, quantityKg);
            redirect.addFlashAttribute("notice", "购买成功，系统已生成销售流水");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/market";
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
        try {
            saleService.create(batchId, customerName, productName, quantityKg, unitPrice,
                    LocalDate.parse(saleDate), note);
            redirect.addFlashAttribute("notice", "销售记录已保存");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/sales";
    }

    private SessionUser currentUser(HttpSession session) {
        Object user = session.getAttribute(SessionUser.SESSION_KEY);
        if (user instanceof SessionUser) {
            return (SessionUser) user;
        }
        return null;
    }
}
