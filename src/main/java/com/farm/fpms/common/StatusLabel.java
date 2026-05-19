package com.farm.fpms.common;

import java.util.HashMap;
import java.util.Map;

public class StatusLabel {

    private static final Map<String, String> BATCH_STATUS = new HashMap<>();
    private static final Map<String, String> OPERATION_TYPE = new HashMap<>();
    private static final Map<String, String> ROLE_LABEL = new HashMap<>();

    static {
        // English → Chinese (backward compat)
        BATCH_STATUS.put("PLANNED", "已计划");
        BATCH_STATUS.put("SOWED", "已播种");
        BATCH_STATUS.put("GROWING", "生长期");
        BATCH_STATUS.put("READY_HARVEST", "待采收");
        BATCH_STATUS.put("HARVESTING", "采收中");
        BATCH_STATUS.put("COMPLETED", "已完结");
        BATCH_STATUS.put("ABANDONED", "已废弃");
        // Chinese pass-through (after DB migration)
        BATCH_STATUS.put("已计划", "已计划");
        BATCH_STATUS.put("已播种", "已播种");
        BATCH_STATUS.put("生长期", "生长期");
        BATCH_STATUS.put("待采收", "待采收");
        BATCH_STATUS.put("采收中", "采收中");
        BATCH_STATUS.put("已完结", "已完结");
        BATCH_STATUS.put("已废弃", "已废弃");

        // English → Chinese (backward compat)
        OPERATION_TYPE.put("SOWING", "播种");
        OPERATION_TYPE.put("FERTILIZE", "施肥");
        OPERATION_TYPE.put("PESTICIDE", "施药");
        OPERATION_TYPE.put("IRRIGATION", "灌溉");
        OPERATION_TYPE.put("WEEDING", "除草");
        OPERATION_TYPE.put("PEST_CHECK", "病虫害巡检");
        OPERATION_TYPE.put("HARVEST", "采收");
        OPERATION_TYPE.put("CORRECTION", "红冲修正");
        // Chinese pass-through (after DB migration)
        OPERATION_TYPE.put("播种", "播种");
        OPERATION_TYPE.put("施肥", "施肥");
        OPERATION_TYPE.put("施药", "施药");
        OPERATION_TYPE.put("灌溉", "灌溉");
        OPERATION_TYPE.put("除草", "除草");
        OPERATION_TYPE.put("病虫害巡检", "病虫害巡检");
        OPERATION_TYPE.put("采收", "采收");
        OPERATION_TYPE.put("红冲修正", "红冲修正");

        ROLE_LABEL.put("SUPER_ADMIN", "超级管理员");
        ROLE_LABEL.put("FARM_OWNER", "农场主");
        ROLE_LABEL.put("AGRI_TECH", "农技员");
        ROLE_LABEL.put("WAREHOUSE", "仓库管理员");
        ROLE_LABEL.put("FIELD_WORKER", "田间工人");
        ROLE_LABEL.put("CUSTOMER", "客户");
    }

    public String batchStatus(String code) {
        if (code == null) return "";
        return BATCH_STATUS.getOrDefault(code, code);
    }

    public String operationType(String code) {
        if (code == null) return "";
        return OPERATION_TYPE.getOrDefault(code, code);
    }

    public String role(String code) {
        if (code == null) return "";
        return ROLE_LABEL.getOrDefault(code, code);
    }
}
