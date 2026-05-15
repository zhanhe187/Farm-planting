package com.farm.fpms.web;

import java.util.HashMap;
import java.util.Map;

public class StatusLabel {

    private static final Map<String, String> BATCH_STATUS = new HashMap<>();
    private static final Map<String, String> OPERATION_TYPE = new HashMap<>();
    private static final Map<String, String> ROLE_LABEL = new HashMap<>();

    static {
        BATCH_STATUS.put("PLANNED", "已规划");
        BATCH_STATUS.put("SOWED", "已播种");
        BATCH_STATUS.put("GROWING", "生长中");
        BATCH_STATUS.put("READY_HARVEST", "待采收");
        BATCH_STATUS.put("HARVESTING", "采收中");
        BATCH_STATUS.put("COMPLETED", "已完成");
        BATCH_STATUS.put("ABANDONED", "已废弃");

        OPERATION_TYPE.put("SOWING", "播种");
        OPERATION_TYPE.put("FERTILIZE", "施肥");
        OPERATION_TYPE.put("PESTICIDE", "施药");
        OPERATION_TYPE.put("IRRIGATION", "灌溉");
        OPERATION_TYPE.put("PRUNING", "修剪");
        OPERATION_TYPE.put("HARVEST", "采收");
        OPERATION_TYPE.put("CORRECTION", "红冲修正");
        OPERATION_TYPE.put("OTHER", "其他");

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
