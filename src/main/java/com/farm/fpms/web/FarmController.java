package com.farm.fpms.web;

import com.farm.fpms.domain.BatchStatus;
import com.farm.fpms.domain.BusinessException;
import com.farm.fpms.service.BatchService;
import com.farm.fpms.service.DashboardService;
import com.farm.fpms.service.HarvestService;
import com.farm.fpms.service.OperationService;
import com.farm.fpms.service.SafetyCheckResult;
import com.farm.fpms.service.AiGateway;
import com.farm.fpms.service.AiProvider;
import com.farm.fpms.service.AiProviderForm;
import com.farm.fpms.service.AiProviderService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import javax.servlet.http.HttpSession;
import java.time.LocalDate;

@Controller
public class FarmController {

    private final JdbcTemplate jdbcTemplate;
    private final DashboardService dashboardService;
    private final BatchService batchService;
    private final OperationService operationService;
    private final HarvestService harvestService;
    private final AiProviderService aiProviderService;
    private final AiGateway aiGateway;

    public FarmController(JdbcTemplate jdbcTemplate, DashboardService dashboardService, BatchService batchService,
                          OperationService operationService, HarvestService harvestService,
                          AiProviderService aiProviderService, AiGateway aiGateway) {
        this.jdbcTemplate = jdbcTemplate;
        this.dashboardService = dashboardService;
        this.batchService = batchService;
        this.operationService = operationService;
        this.harvestService = harvestService;
        this.aiProviderService = aiProviderService;
        this.aiGateway = aiGateway;
    }

    @GetMapping("/plots")
    public String plots(Model model) {
        model.addAttribute("plots", jdbcTemplate.queryForList(
                "select p.*, u.display_name owner_name from farm_plot p left join sys_user u on u.id = p.owner_id order by p.id"));
        return "plots";
    }

    @GetMapping("/batches")
    public String batches(Model model) {
        model.addAttribute("batches", dashboardService.batches());
        model.addAttribute("statuses", BatchStatus.values());
        return "batches";
    }

    @PostMapping("/batches/{id}/advance")
    public String advanceBatch(@PathVariable long id, @RequestParam String target,
                               HttpSession session, RedirectAttributes redirect) {
        try {
            batchService.transit(id, BatchStatus.valueOf(target), operatorName(session), "状态审批");
            redirect.addFlashAttribute("notice", "批次状态已推进到 " + target);
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/batches";
    }

    @GetMapping("/operations")
    public String operations(Model model) {
        model.addAttribute("operations", jdbcTemplate.queryForList(
                "select o.*, b.batch_no, c.name crop_name from plant_operation o " +
                        "join plant_batch b on b.id = o.batch_id join farm_crop c on c.id = b.crop_id order by o.id desc"));
        model.addAttribute("batches", dashboardService.batches());
        model.addAttribute("materials", jdbcTemplate.queryForList(
                "select m.*, i.quantity stock_quantity from farm_material m left join stock_inventory i on i.material_id = m.id order by m.id"));
        return "operations";
    }

    @PostMapping("/operations")
    public String createOperation(@RequestParam long batchId,
                                  @RequestParam String type,
                                  @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate operationDate,
                                  @RequestParam String workerName,
                                  @RequestParam(required = false) String note,
                                  @RequestParam(required = false) Long materialId,
                                  @RequestParam(required = false) Double quantity,
                                  RedirectAttributes redirect) {
        try {
            operationService.createOperation(batchId, type, operationDate, workerName, note, materialId, quantity);
            redirect.addFlashAttribute("notice", "农事作业已记录，若选择了农资则已同步扣减库存");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/operations";
    }

    @GetMapping("/stock")
    public String stock(Model model) {
        model.addAttribute("rows", jdbcTemplate.queryForList(
                "select m.name, m.category, m.unit, m.safe_interval_days, i.quantity, i.safety_stock, i.version " +
                        "from stock_inventory i join farm_material m on m.id = i.material_id order by m.id"));
        model.addAttribute("outOrders", jdbcTemplate.queryForList(
                "select so.*, m.name material_name from stock_out_order so join farm_material m on m.id = so.material_id order by so.id desc"));
        return "stock";
    }

    @GetMapping("/harvest")
    public String harvest(Model model, @RequestParam(required = false) Long batchId) {
        model.addAttribute("batches", dashboardService.batches());
        if (batchId != null) {
            SafetyCheckResult result = harvestService.precheck(batchId, LocalDate.now());
            model.addAttribute("precheck", result);
            model.addAttribute("selectedBatchId", batchId);
        }
        model.addAttribute("records", jdbcTemplate.queryForList(
                "select h.*, b.batch_no, c.name crop_name from harvest_record h " +
                        "join plant_batch b on b.id = h.batch_id join farm_crop c on c.id = b.crop_id order by h.id desc"));
        return "harvest";
    }

    @PostMapping("/harvest")
    public String doHarvest(@RequestParam long batchId,
                            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate harvestDate,
                            @RequestParam double quantityKg,
                            @RequestParam String grade,
                            HttpSession session,
                            RedirectAttributes redirect) {
        try {
            String traceCode = harvestService.harvest(batchId, harvestDate, quantityKg, grade, operatorName(session));
            redirect.addFlashAttribute("notice", "采收完成，追溯码：" + traceCode);
            return "redirect:/trace/" + traceCode;
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
            return "redirect:/harvest?batchId=" + batchId;
        }
    }

    @GetMapping("/providers")
    public String providers(Model model) {
        model.addAttribute("providers", aiProviderService.listProviderRows());
        model.addAttribute("logs", aiProviderService.listLogRows());
        model.addAttribute("form", new AiProviderForm());
        return "providers";
    }

    @PostMapping("/providers")
    public String createProvider(AiProviderForm form, RedirectAttributes redirect) {
        try {
            aiProviderService.createProvider(form);
            redirect.addFlashAttribute("notice", "AI 端点已保存，可以在 AI 对话或手机拍照识别中使用。");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/providers";
    }

    @PostMapping("/providers/{id}/test")
    public String testProvider(@PathVariable long id, RedirectAttributes redirect) {
        AiProvider provider = aiProviderService.requireProviderById(id);
        long start = System.nanoTime();
        try {
            aiGateway.chat(provider, "你是 FPMS AI 连通性测试助手。", "请只回复 pong。");
            aiProviderService.logCall(provider.getId(), provider.getScene(), "ping 连通性测试", true,
                    java.time.Duration.ofNanos(System.nanoTime() - start), null);
            redirect.addFlashAttribute("notice", "端点连通性测试通过，已写入调用日志。");
        } catch (RuntimeException ex) {
            aiProviderService.logCall(provider.getId(), provider.getScene(), "ping 连通性测试", false,
                    java.time.Duration.ofNanos(System.nanoTime() - start), ex.getClass().getSimpleName());
            redirect.addFlashAttribute("error", "端点测试失败：" + ex.getMessage());
        }
        return "redirect:/providers";
    }

    private String operatorName(HttpSession session) {
        Object user = session.getAttribute(SessionUser.SESSION_KEY);
        if (user instanceof SessionUser) {
            return ((SessionUser) user).getDisplayName();
        }
        return "系统用户";
    }
}
