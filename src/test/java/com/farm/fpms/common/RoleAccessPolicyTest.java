package com.farm.fpms.common;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleAccessPolicyTest {

    private final RoleAccessPolicy policy = new RoleAccessPolicy();

    @Test
    void onlySuperAdminCanManageAiProviders() {
        assertThat(policy.canAccess(user("SUPER_ADMIN"), "GET", "/providers")).isTrue();
        assertThat(policy.canAccess(user("FARM_OWNER"), "GET", "/providers")).isFalse();
        assertThat(policy.canAccess(user("AGRI_TECH"), "GET", "/providers")).isFalse();
    }

    @Test
    void warehouseCanOnlyOperateStockInBackOffice() {
        SessionUser warehouse = user("WAREHOUSE");

        assertThat(policy.canAccess(warehouse, "GET", "/stock")).isTrue();
        assertThat(policy.canAccess(warehouse, "POST", "/stock/replenish")).isTrue();
        assertThat(policy.canAccess(warehouse, "POST", "/crops")).isTrue();
        assertThat(policy.canAccess(warehouse, "POST", "/crops/1")).isFalse();
        assertThat(policy.canAccess(warehouse, "POST", "/crops/1/delete")).isFalse();
        assertThat(policy.canAccess(warehouse, "GET", "/batches")).isFalse();
        assertThat(policy.canAccess(warehouse, "GET", "/sales")).isFalse();
        assertThat(policy.defaultPath(warehouse)).isEqualTo("/stock");
    }

    @Test
    void fieldWorkerCanRecordOperationsAndUseMobileButCannotManageStockOrSales() {
        SessionUser worker = user("FIELD_WORKER");

        assertThat(policy.canAccess(worker, "GET", "/operations")).isTrue();
        assertThat(policy.canAccess(worker, "POST", "/operations")).isTrue();
        assertThat(policy.canAccess(worker, "GET", "/mobile")).isTrue();
        assertThat(policy.canAccess(worker, "GET", "/stock")).isFalse();
        assertThat(policy.canAccess(worker, "GET", "/sales")).isFalse();
        assertThat(policy.defaultPath(worker)).isEqualTo("/operations");
    }

    @Test
    void customerCanOnlyReadSalesAndCannotCreateSales() {
        SessionUser customer = user("CUSTOMER");

        assertThat(policy.canAccess(customer, "GET", "/market")).isTrue();
        assertThat(policy.canAccess(customer, "POST", "/market")).isTrue();
        assertThat(policy.canAccess(customer, "GET", "/sales")).isTrue();
        assertThat(policy.canAccess(customer, "POST", "/sales")).isFalse();
        assertThat(policy.canAccess(customer, "GET", "/dashboard")).isFalse();
        assertThat(policy.defaultPath(customer)).isEqualTo("/market");
    }

    private SessionUser user(String roleCode) {
        return new SessionUser(1L, roleCode.toLowerCase(), roleCode, roleCode, "ALL");
    }
}
