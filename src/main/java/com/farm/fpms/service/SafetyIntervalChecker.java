package com.farm.fpms.service;

import com.farm.fpms.entity.MaterialCategory;
import com.farm.fpms.entity.OperationMaterialSnapshot;
import com.farm.fpms.entity.OperationSnapshot;
import com.farm.fpms.entity.OperationType;
import com.farm.fpms.entity.SafetyCheckResult;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class SafetyIntervalChecker {

    public SafetyCheckResult check(List<OperationSnapshot> operations, LocalDate harvestDate) {
        LocalDate forbidUntil = null;
        for (OperationSnapshot operation : operations) {
            for (OperationMaterialSnapshot material : operation.getMaterials()) {
                if (material.getCategory() == MaterialCategory.PESTICIDE) {
                    LocalDate candidate = operation.getOperationDate().plusDays(material.getSafeIntervalDays());
                    if (forbidUntil == null || candidate.isAfter(forbidUntil)) {
                        forbidUntil = candidate;
                    }
                }
            }
        }
        if (forbidUntil == null || !harvestDate.isBefore(forbidUntil)) {
            return new SafetyCheckResult(true, forbidUntil, "安全间隔期已满足，可以采收");
        }
        return new SafetyCheckResult(false, forbidUntil, "未满足农药安全间隔期，最早采收日期：" + forbidUntil);
    }

    public SafetyCheckResult checkBatch(JdbcTemplate jdbcTemplate, long batchId, LocalDate harvestDate) {
        return check(loadBatchOperations(jdbcTemplate, batchId), harvestDate);
    }

    public List<OperationSnapshot> loadBatchOperations(JdbcTemplate jdbcTemplate, long batchId) {
        String sql = "select o.id operation_id, o.type, o.operation_date, m.id material_id, m.category, " +
                "coalesce(m.safe_interval_days, 0) safe_interval_days " +
                "from plant_operation o " +
                "left join plant_operation_material pom on pom.operation_id = o.id " +
                "left join farm_material m on m.id = pom.material_id " +
                "where o.batch_id = ? order by o.operation_date asc, o.id asc";
        Map<Long, OperationSnapshotBuilder> builders = new LinkedHashMap<Long, OperationSnapshotBuilder>();
        jdbcTemplate.query(sql, rs -> {
            Long operationId = rs.getLong("operation_id");
            OperationSnapshotBuilder builder = builders.get(operationId);
            if (builder == null) {
                Date opDate = rs.getDate("operation_date");
                builder = new OperationSnapshotBuilder(operationId,
                        OperationType.fromLabel(rs.getString("type")),
                        opDate == null ? LocalDate.now() : opDate.toLocalDate());
                builders.put(operationId, builder);
            }
            Long materialId = rs.getLong("material_id");
            if (!rs.wasNull()) {
                builder.materials.add(new OperationMaterialSnapshot(materialId,
                        MaterialCategory.fromLabel(rs.getString("category")),
                        rs.getInt("safe_interval_days")));
            }
        }, batchId);
        List<OperationSnapshot> result = new ArrayList<OperationSnapshot>();
        for (OperationSnapshotBuilder builder : builders.values()) {
            result.add(new OperationSnapshot(builder.operationId, builder.type, builder.date, builder.materials));
        }
        return result;
    }

    private static class OperationSnapshotBuilder {
        private final Long operationId;
        private final OperationType type;
        private final LocalDate date;
        private final List<OperationMaterialSnapshot> materials = new ArrayList<OperationMaterialSnapshot>();

        private OperationSnapshotBuilder(Long operationId, OperationType type, LocalDate date) {
            this.operationId = operationId;
            this.type = type;
            this.date = date;
        }
    }
}
