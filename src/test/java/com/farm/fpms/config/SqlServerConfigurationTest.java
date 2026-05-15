package com.farm.fpms.config;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SqlServerConfigurationTest {

    @Test
    void applicationUsesSqlServerDaihaojieXmByDefault() throws Exception {
        String yaml = resourceText("/application.yml");

        assertThat(yaml).contains("jdbc:sqlserver://localhost:1433;databaseName=daihaojie_xm");
        assertThat(yaml).contains("username: sa");
        assertThat(yaml).contains("password: 123456");
        assertThat(yaml).contains("driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver");
        assertThat(yaml).contains("mode: never");
        assertThat(yaml).doesNotContain("jdbc:h2:");
        assertThat(yaml).doesNotContain("h2:");
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
