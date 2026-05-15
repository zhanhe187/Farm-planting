package com.farm.fpms.service;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final JdbcTemplate jdbcTemplate;

    public DashboardService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> overview() {
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("plotCount", jdbcTemplate.queryForObject("select count(*) from farm_plot", Integer.class));
        data.put("activeBatchCount", jdbcTemplate.queryForObject(
                "select count(*) from plant_batch where status not in ('COMPLETED','ABANDONED')", Integer.class));
        data.put("readyHarvestCount", jdbcTemplate.queryForObject(
                "select count(*) from plant_batch where status = 'READY_HARVEST'", Integer.class));
        data.put("lowStockCount", jdbcTemplate.queryForObject(
                "select count(*) from stock_inventory where quantity <= safety_stock", Integer.class));
        data.put("plots", jdbcTemplate.queryForList(
                "select p.*, b.batch_no, b.status batch_status, c.name crop_name, b.sow_date, b.expected_harvest_date " +
                        "from farm_plot p left join plant_batch b on b.plot_id = p.id and b.status not in ('COMPLETED','ABANDONED') " +
                        "left join farm_crop c on c.id = b.crop_id order by p.id"));
        data.put("recentOperations", jdbcTemplate.queryForList(
                "select o.*, b.batch_no, c.name crop_name from plant_operation o " +
                        "join plant_batch b on b.id = o.batch_id join farm_crop c on c.id = b.crop_id " +
                        "order by o.operation_date desc, o.id desc"));
        data.put("statusRows", jdbcTemplate.queryForList(
                "select status, count(*) status_value from plant_batch group by status order by status"));
        data.put("stockRows", jdbcTemplate.queryForList(
                "select m.name, i.quantity, i.safety_stock from stock_inventory i join farm_material m on m.id = i.material_id order by i.id"));
        data.put("totalRevenue", jdbcTemplate.queryForObject(
                "select isnull(sum(total_amount), 0) from sale_order", BigDecimal.class));
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        data.put("monthRevenue", jdbcTemplate.queryForObject(
                "select isnull(sum(total_amount), 0) from sale_order where sale_date >= ?",
                BigDecimal.class, java.sql.Date.valueOf(monthStart)));
        data.put("orderCount", jdbcTemplate.queryForObject("select count(*) from sale_order", Integer.class));
        return data;
    }

    public List<Map<String, Object>> batches() {
        return jdbcTemplate.queryForList(
                "select b.*, p.name plot_name, c.name crop_name, c.variety crop_variety from plant_batch b " +
                        "join farm_plot p on p.id = b.plot_id join farm_crop c on c.id = b.crop_id order by b.id desc");
    }
}
