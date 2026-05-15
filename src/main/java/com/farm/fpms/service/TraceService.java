package com.farm.fpms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class TraceService {

    private final JdbcTemplate jdbcTemplate;
    private final QrCodeService qrCodeService;

    @Value("${fpms.trace.base-url:http://localhost:8080/trace/}")
    private String traceBaseUrl;

    public TraceService(JdbcTemplate jdbcTemplate, QrCodeService qrCodeService) {
        this.jdbcTemplate = jdbcTemplate;
        this.qrCodeService = qrCodeService;
    }

    public Map<String, Object> publicTrace(String code) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select h.*, b.batch_no, b.id batch_id, p.name plot_name, p.soil_type, c.name crop_name, c.variety, c.image_url " +
                        "from harvest_record h join plant_batch b on b.id = h.batch_id " +
                        "join farm_plot p on p.id = b.plot_id join farm_crop c on c.id = b.crop_id where h.trace_code = ?",
                code);
        if (rows.isEmpty()) {
            return null;
        }
        Map<String, Object> trace = new HashMap<String, Object>(rows.get(0));

        Set<String> publicFields = loadPublicFields();
        List<Map<String, Object>> timeline = jdbcTemplate.queryForList(
                "select type, operation_date, worker_name, note from plant_operation where batch_id = ? order by operation_date asc",
                trace.get("batch_id"));
        if (!publicFields.contains("materialBrand")) {
            for (Map<String, Object> op : timeline) {
                op.put("note", "***");
            }
        }
        trace.put("timeline", timeline);
        return trace;
    }

    public String generateQrDataUri(String traceCode) {
        return qrCodeService.toDataUri(traceBaseUrl + traceCode);
    }

    private Set<String> loadPublicFields() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select field_key from trace_public_field where public_enabled = 1");
        Set<String> fields = new HashSet<String>();
        for (Map<String, Object> row : rows) {
            Object key = row.get("field_key") != null ? row.get("field_key") : row.get("FIELD_KEY");
            if (key != null) fields.add(key.toString());
        }
        return fields;
    }
}
