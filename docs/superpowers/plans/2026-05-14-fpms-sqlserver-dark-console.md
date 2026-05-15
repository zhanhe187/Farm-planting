# FPMS SQL Server Dark Console Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make SQL Server `daihaojie_xm` the only runtime database and upgrade the Thymeleaf frontend to a dark command-center style.

**Architecture:** Keep the current Spring Boot 2.7 + Thymeleaf + JdbcTemplate architecture. Add one repeatable SQL Server setup script for database creation, schema creation, and seed data. Adjust only the small backend SQL compatibility points needed for SQL Server, then retheme existing templates through the shared stylesheet and fragments.

**Tech Stack:** Java 8 target, Spring Boot 2.7.18, JdbcTemplate, Thymeleaf, SQL Server, mssql-jdbc, JUnit 5, AssertJ.

---

## File Map

- Modify `src/main/resources/application.yml`: default datasource becomes SQL Server and automatic `schema.sql` / `data.sql` init is disabled.
- Create `src/main/resources/db/init-daihaojie-xm.sql`: repeatable SQL Server database, table, trigger, view, and seed data setup.
- Modify `src/main/resources/db/db-sqlserver.sql`: align standalone schema with complete runtime table set if needed.
- Modify `src/main/java/com/farm/fpms/service/OperationService.java`: use `GeneratedKeyHolder` instead of `select max(id)`.
- Modify `src/main/resources/static/css/app.css`: implement dark command-center theme and responsive polish.
- Modify `src/main/resources/static/js/dashboard.js`: tune canvas and ECharts colors for dark theme.
- Modify `src/main/resources/templates/fragments.html`: make brand and topbar fit the new style.
- Modify `README.md` and `docs/落地部署与运行说明.md`: document SQL Server-only runtime and initialization command.
- Create tests under `src/test/java/com/farm/fpms/config` and `src/test/java/com/farm/fpms/service` to pin the database config/script and generated-key behavior.

---

### Task 1: Pin SQL Server Runtime Configuration

**Files:**
- Test: `src/test/java/com/farm/fpms/config/SqlServerConfigurationTest.java`
- Modify: `src/main/resources/application.yml`

- [ ] **Step 1: Write the failing configuration test**

```java
package com.farm.fpms.config;

import org.junit.jupiter.api.Test;

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
            byte[] bytes = new byte[input.available()];
            int read = input.read(bytes);
            assertThat(read).isEqualTo(bytes.length);
            return new String(bytes, StandardCharsets.UTF_8);
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=SqlServerConfigurationTest test`

Expected: FAIL because `application.yml` still contains H2 and `spring.sql.init.mode: always`.

- [ ] **Step 3: Update application.yml**

Set:

```yaml
spring:
  datasource:
    url: jdbc:sqlserver://localhost:1433;databaseName=daihaojie_xm;encrypt=true;trustServerCertificate=true
    username: sa
    password: 123456
    driver-class-name: com.microsoft.sqlserver.jdbc.SQLServerDriver
  sql:
    init:
      mode: never
      encoding: UTF-8
```

Remove the `spring.h2.console` block.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=SqlServerConfigurationTest test`

Expected: PASS.

---

### Task 2: Add Repeatable SQL Server Initialization Script

**Files:**
- Test: `src/test/java/com/farm/fpms/config/SqlServerInitScriptTest.java`
- Create: `src/main/resources/db/init-daihaojie-xm.sql`

- [ ] **Step 1: Write the failing script coverage test**

```java
package com.farm.fpms.config;

import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class SqlServerInitScriptTest {

    @Test
    void initScriptCreatesDatabaseSchemaAndSeedData() throws Exception {
        String sql = resourceText("/db/init-daihaojie-xm.sql");

        assertThat(sql).contains("create database daihaojie_xm");
        assertThat(sql).contains("use daihaojie_xm");
        assertThat(sql).contains("create table dbo.sys_user");
        assertThat(sql).contains("create table dbo.ai_recognize_log");
        assertThat(sql).contains("insert into dbo.sys_user");
        assertThat(sql).contains("owner', '123456'");
        assertThat(sql).contains("BATCH-20260501-TOMATO");
        assertThat(sql).contains("trg_operation_block_update");
        assertThat(sql).contains("v_batch_yield_summary");
    }

    private String resourceText(String path) throws Exception {
        try (InputStream input = getClass().getResourceAsStream(path)) {
            assertThat(input).isNotNull();
            byte[] bytes = new byte[input.available()];
            int read = input.read(bytes);
            assertThat(read).isEqualTo(bytes.length);
            return new String(bytes, StandardCharsets.UTF_8).toLowerCase();
        }
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=SqlServerInitScriptTest test`

Expected: FAIL because the script does not exist.

- [ ] **Step 3: Create `init-daihaojie-xm.sql`**

The script must:

```sql
if db_id(N'daihaojie_xm') is null
begin
    create database daihaojie_xm;
end;
go
use daihaojie_xm;
go
```

Then drop tables in dependency order, create all runtime tables under `dbo`, insert seed data from `data.sql`, create the immutable operation trigger, create `v_batch_yield_summary`, and create its unique clustered index.

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=SqlServerInitScriptTest test`

Expected: PASS.

---

### Task 3: Use Generated Keys For Operation Inserts

**Files:**
- Test: `src/test/java/com/farm/fpms/service/OperationServiceTest.java`
- Modify: `src/main/java/com/farm/fpms/service/OperationService.java`

- [ ] **Step 1: Write the failing test**

```java
package com.farm.fpms.service;

import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OperationServiceTest {

    @Test
    void createOperationReturnsGeneratedKeyWithoutMaxIdQuery() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        StockService stockService = mock(StockService.class);
        AtomicBoolean usedKeyHolder = new AtomicBoolean(false);

        when(jdbcTemplate.update(any(), any(GeneratedKeyHolder.class))).thenAnswer(invocation -> {
            GeneratedKeyHolder keyHolder = invocation.getArgument(1);
            keyHolder.getKeyList().add(java.util.Collections.singletonMap("id", 42L));
            usedKeyHolder.set(true);
            return 1;
        });

        OperationService service = new OperationService(jdbcTemplate, stockService);

        long id = service.createOperation(1L, "FERTILIZE", LocalDate.of(2026, 5, 14),
                "worker", "note", null, null);

        assertThat(id).isEqualTo(42L);
        assertThat(usedKeyHolder).isTrue();
        verify(jdbcTemplate, never()).queryForObject(eq("select max(id) from plant_operation"), eq(Long.class));
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `mvn -Dtest=OperationServiceTest test`

Expected: FAIL because `OperationService` does not call `JdbcTemplate.update(PreparedStatementCreator, KeyHolder)`.

- [ ] **Step 3: Update OperationService**

Use:

```java
GeneratedKeyHolder keyHolder = new GeneratedKeyHolder();
jdbcTemplate.update(connection -> {
    PreparedStatement ps = connection.prepareStatement(
            "insert into plant_operation(batch_id, type, operation_date, worker_name, note) values(?, ?, ?, ?, ?)",
            Statement.RETURN_GENERATED_KEYS);
    ps.setLong(1, batchId);
    ps.setString(2, type);
    ps.setDate(3, java.sql.Date.valueOf(operationDate));
    ps.setString(4, workerName);
    ps.setString(5, note);
    return ps;
}, keyHolder);
Long operationId = keyHolder.getKey() == null ? 0L : keyHolder.getKey().longValue();
```

- [ ] **Step 4: Run test to verify it passes**

Run: `mvn -Dtest=OperationServiceTest test`

Expected: PASS.

---

### Task 4: Apply Dark Command-Center Frontend Theme

**Files:**
- Modify: `src/main/resources/static/css/app.css`
- Modify: `src/main/resources/static/js/dashboard.js`
- Modify: `src/main/resources/templates/fragments.html`

- [ ] **Step 1: Update shared CSS tokens**

Define dark tokens:

```css
:root {
    --field: #8fd06a;
    --leaf: #4f9a63;
    --moss: #8aa596;
    --soil: #d6a044;
    --amber: #f0bd52;
    --paper: #121a16;
    --surface: #1b271f;
    --surface-2: #223429;
    --line: #355240;
    --ink: #f3f7ec;
    --muted: #a7b6aa;
    --danger: #ff806f;
}
```

- [ ] **Step 2: Restyle layout and cards**

Set body background to a dark radial/linear treatment, make `.sidebar`, `.card`, tables, inputs, buttons, `#plotCanvas`, `.chart`, and `.ai-float` use dark surfaces with readable contrast.

- [ ] **Step 3: Tune dashboard JS colors**

Use dark canvas fill colors and ECharts axis/label colors matching the CSS tokens.

- [ ] **Step 4: Quick CSS scan**

Run: `rg -n "#f6f1e4|#fffdf7|#243b2a|#f0ead8" src/main/resources/static/css/app.css src/main/resources/static/js/dashboard.js`

Expected: no stale light theme colors.

---

### Task 5: Update Documentation

**Files:**
- Modify: `README.md`
- Modify: `docs/落地部署与运行说明.md`
- Modify: `docs/项目完成进度报告.md`

- [ ] **Step 1: Replace H2 startup instructions**

Document SQL Server connection and the initialization command:

```powershell
sqlcmd -S localhost -U sa -P 123456 -C -b -f 65001 -i src\main\resources\db\init-daihaojie-xm.sql
```

- [ ] **Step 2: State runtime database clearly**

Add that all project data is stored in SQL Server database `daihaojie_xm`.

- [ ] **Step 3: Run doc scan**

Run: `rg -n "H2 控制台|jdbc:h2|默认使用 H2|新建 `fpms`" README.md docs/落地部署与运行说明.md`

Expected: no obsolete H2-as-runtime guidance remains.

---

### Task 6: Full Verification

**Files:**
- No new files.

- [ ] **Step 1: Run all tests**

Run: `mvn test`

Expected: BUILD SUCCESS.

- [ ] **Step 2: Check SQL Server command availability**

Run: `sqlcmd -?`

Expected: sqlcmd prints help. If not installed, record that SQL execution could not be run locally.

- [ ] **Step 3: Try SQL Server initialization when available**

Run:

```powershell
sqlcmd -S localhost -U sa -P 123456 -C -b -f 65001 -i src\main\resources\db\init-daihaojie-xm.sql
```

Expected: command completes without SQL errors.

- [ ] **Step 4: Start application when SQL Server is available**

Run:

```powershell
mvn spring-boot:run
```

Expected: application starts and connects to SQL Server `daihaojie_xm`.

## Self Review

- Spec coverage: database switch, SQL Server seed data, backend compatibility, dark frontend, and documentation are all covered.
- Placeholder scan: no TODO/TBD placeholders are used as work instructions.
- Type consistency: test and implementation names match current project packages and classes.
