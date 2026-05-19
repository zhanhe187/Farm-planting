package com.farm.fpms.config;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SqlServerInitScriptTest {

    @Test
    void initScriptCreatesDatabaseSchemaAndSeedData() throws Exception {
        String sql = resourceText("/db/init-daihaojie-xm.sql").toLowerCase();

        assertThat(sql).contains("create database daihaojie_xm");
        assertThat(sql).contains("use daihaojie_xm");
        assertThat(sql).contains("create table dbo.sys_user");
        assertThat(sql).contains("create table dbo.ai_recognize_log");
        assertThat(sql).contains("insert into dbo.sys_user");
        assertThat(sql).contains("owner', '123456'");
        assertThat(sql).contains("2026春-番茄-1号");
        assertThat(sql).contains("trg_operation_block_update");
        assertThat(sql).contains("v_batch_yield_summary");
        assertThat(sql).contains("深度求索对话示例");
        assertThat(sql).doesNotContain("mock-vision");
    }

    private String resourceText(String path) throws Exception {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            assertThat(input).isNotNull();
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            byte[] buffer = new byte[1024];
            int read;
            while ((read = input.read(buffer)) != -1) {
                output.write(buffer, 0, read);
            }
            return new String(output.toByteArray(), StandardCharsets.UTF_8);
        }
    }
}
