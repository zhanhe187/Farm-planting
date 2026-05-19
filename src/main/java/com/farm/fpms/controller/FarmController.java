package com.farm.fpms.controller;

import com.farm.fpms.entity.BatchStatus;
import com.farm.fpms.common.BusinessException;
import com.farm.fpms.common.SessionUser;
import com.farm.fpms.service.BatchService;
import com.farm.fpms.service.CropCatalogService;
import com.farm.fpms.service.DashboardService;
import com.farm.fpms.service.HarvestService;
import com.farm.fpms.service.OperationService;
import com.farm.fpms.service.StockService;
import com.farm.fpms.entity.MaterialCategory;
import com.farm.fpms.entity.SafetyCheckResult;
import com.farm.fpms.service.AiGateway;
import com.farm.fpms.entity.AiProvider;
import com.farm.fpms.entity.AiProviderForm;
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
import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
public class FarmController {

    private final JdbcTemplate jdbcTemplate;
    private final DashboardService dashboardService;
    private final BatchService batchService;
    private final CropCatalogService cropCatalogService;
    private final OperationService operationService;
    private final StockService stockService;
    private final HarvestService harvestService;
    private final AiProviderService aiProviderService;
    private final AiGateway aiGateway;

    public FarmController(JdbcTemplate jdbcTemplate, DashboardService dashboardService, BatchService batchService,
                          CropCatalogService cropCatalogService,
                          OperationService operationService, HarvestService harvestService,
                          StockService stockService, AiProviderService aiProviderService, AiGateway aiGateway) {
        this.jdbcTemplate = jdbcTemplate;
        this.dashboardService = dashboardService;
        this.batchService = batchService;
        this.cropCatalogService = cropCatalogService;
        this.operationService = operationService;
        this.stockService = stockService;
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
        model.addAttribute("plots", jdbcTemplate.queryForList("select id, name, area_mu from farm_plot order by id"));
        model.addAttribute("crops", cropCatalogService.listActiveCrops());
        model.addAttribute("cropCatalog", cropCatalogService.listAllCrops());
        return "batches";
    }

    @PostMapping("/crops")
    public String createCrop(@RequestParam String name,
                             @RequestParam(required = false) String variety,
                             @RequestParam int minGrowthDays,
                             @RequestParam(defaultValue = "0") BigDecimal salePricePerKg,
                             @RequestParam(required = false) String imageUrl,
                             @RequestParam(defaultValue = "/batches") String redirectTo,
                             RedirectAttributes redirect) {
        try {
            long id = cropCatalogService.createCrop(name, variety, minGrowthDays, salePricePerKg, imageUrl);
            redirect.addFlashAttribute("notice", "可种植农作物已添加，编号：" + id);
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:" + safeCatalogRedirect(redirectTo);
    }

    @PostMapping("/crops/{id}")
    public String updateCrop(@PathVariable long id,
                             @RequestParam String name,
                             @RequestParam(required = false) String variety,
                             @RequestParam int minGrowthDays,
                             @RequestParam(defaultValue = "0") BigDecimal salePricePerKg,
                             @RequestParam(required = false) String imageUrl,
                             @RequestParam(defaultValue = "/batches") String redirectTo,
                             RedirectAttributes redirect) {
        try {
            cropCatalogService.updateCrop(id, name, variety, minGrowthDays, salePricePerKg, imageUrl);
            redirect.addFlashAttribute("notice", "农作物目录已更新，新的参考价会用于后续选择和展示");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:" + safeCatalogRedirect(redirectTo);
    }

    @PostMapping("/crops/{id}/delete")
    public String deleteCrop(@PathVariable long id,
                             @RequestParam(defaultValue = "/batches") String redirectTo,
                             RedirectAttributes redirect) {
        cropCatalogService.deleteCrop(id);
        redirect.addFlashAttribute("notice", "农作物已停用，不会再出现在新建批次或种子绑定选项中");
        return "redirect:" + safeCatalogRedirect(redirectTo);
    }

    @PostMapping("/batches")
    public String createBatch(@RequestParam String batchNo,
                              @RequestParam long plotId,
                              @RequestParam long cropId,
                              @RequestParam double plannedAreaMu,
                              @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate sowDate,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expectedHarvestDate,
                              HttpSession session, RedirectAttributes redirect) {
        try {
            long id = batchService.createBatch(batchNo, plotId, cropId, plannedAreaMu, sowDate, expectedHarvestDate,
                    currentUserId(session));
            redirect.addFlashAttribute("notice", "新批次已创建，编号：" + id);
        } catch (RuntimeException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/batches";
    }

    @PostMapping("/batches/{id}/advance")
    public String advanceBatch(@PathVariable long id, @RequestParam String target,
                               HttpSession session, RedirectAttributes redirect) {
        try {
            batchService.transit(id, BatchStatus.fromLabel(target), operatorName(session), "状态审批");
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
                "select m.*, c.name crop_name, i.quantity stock_quantity from farm_material m " +
                        "left join farm_crop c on c.id = m.crop_id left join stock_inventory i on i.material_id = m.id order by m.id"));
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
                "select m.id, m.name, m.category, m.unit, m.safe_interval_days, m.unit_price, m.crop_id, c.name crop_name, " +
                        "i.quantity, i.safety_stock, i.version, i.quantity * m.unit_price inventory_value " +
                        "from stock_inventory i join farm_material m on m.id = i.material_id " +
                        "left join farm_crop c on c.id = m.crop_id order by m.id"));
        model.addAttribute("crops", cropCatalogService.listActiveCrops());
        model.addAttribute("cropCatalog", cropCatalogService.listAllCrops());
        model.addAttribute("categories", MaterialCategory.values());
        model.addAttribute("inOrders", jdbcTemplate.queryForList(
                "select si.*, m.name material_name, m.unit from stock_in_order si join farm_material m on m.id = si.material_id order by si.id desc"));
        model.addAttribute("outOrders", jdbcTemplate.queryForList(
                "select so.*, m.name material_name from stock_out_order so join farm_material m on m.id = so.material_id order by so.id desc"));
        return "stock";
    }

    @PostMapping("/stock/materials")
    public String createMaterial(@RequestParam String name,
                                 @RequestParam String category,
                                 @RequestParam String unit,
                                 @RequestParam(defaultValue = "0") int safeIntervalDays,
                                 @RequestParam(defaultValue = "0") BigDecimal unitPrice,
                                 @RequestParam(required = false) Long cropId,
                                 @RequestParam(defaultValue = "0") double initialQuantity,
                                 @RequestParam(defaultValue = "0") double safetyStock,
                                 RedirectAttributes redirect) {
        try {
            long id = stockService.createMaterial(name, category, unit, safeIntervalDays, unitPrice, cropId,
                    initialQuantity, safetyStock);
            redirect.addFlashAttribute("notice", "农资已添加并建立库存台账，编号：" + id);
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/stock";
    }

    @PostMapping("/stock/replenish")
    public String replenishStock(@RequestParam long materialId,
                                 @RequestParam double quantity,
                                 @RequestParam BigDecimal unitPrice,
                                 RedirectAttributes redirect) {
        try {
            stockService.replenishMaterial(materialId, quantity, unitPrice);
            redirect.addFlashAttribute("notice", "库存已补充");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/stock";
    }

    @GetMapping("/harvest")
    public String harvest(Model model, @RequestParam(required = false) Long batchId) {
        model.addAttribute("batches", dashboardService.batches());
        model.addAttribute("harvestableBatches", jdbcTemplate.queryForList(
                "select b.*, p.name plot_name, c.name crop_name, c.variety crop_variety from plant_batch b " +
                        "join farm_plot p on p.id = b.plot_id join farm_crop c on c.id = b.crop_id " +
                        "where b.status in (N'待采收', N'采收中') order by b.id desc"));
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

    @PostMapping("/providers/{id}")
    public String updateProvider(@PathVariable long id, AiProviderForm form, RedirectAttributes redirect) {
        try {
            aiProviderService.updateProvider(id, form);
            redirect.addFlashAttribute("notice", "AI 端点已更新。");
        } catch (BusinessException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        return "redirect:/providers";
    }

    @PostMapping("/providers/{id}/delete")
    public String deleteProvider(@PathVariable long id, RedirectAttributes redirect) {
        aiProviderService.deleteProvider(id);
        redirect.addFlashAttribute("notice", "AI 端点已删除。");
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

    private Long currentUserId(HttpSession session) {
        Object user = session.getAttribute(SessionUser.SESSION_KEY);
        if (user instanceof SessionUser) {
            return ((SessionUser) user).getId();
        }
        return null;
    }

    private String safeCatalogRedirect(String redirectTo) {
        if ("/stock".equals(redirectTo)) {
            return "/stock";
        }
        return "/batches";
    }
}
