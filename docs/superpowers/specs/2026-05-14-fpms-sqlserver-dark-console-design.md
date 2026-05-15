# FPMS SQL Server And Dark Console Upgrade Design

## Goal

将 FPMS 从 H2 本地运行库切换为 SQL Server 实库运行，所有初始化数据和业务数据都保存在 `daihaojie_xm` 数据库中。同时将前端升级为深色指挥中心风格，使系统更适合项目验收和驾驶舱展示。

## Confirmed Requirements

- 默认数据库改为 SQL Server。
- SQL Server 连接信息：
  - Host: `localhost`
  - Port: `1433`
  - Database: `daihaojie_xm`
  - Username: `sa`
  - Password: `123456`
- 不再把 H2 作为运行数据源。
- 建表、初始化账号、地块、作物、批次、库存、农事、AI、移动绑定、溯源等数据全部落入 SQL Server。
- 前端采用深色指挥中心方向。

## Database Design

新增一个 SQL Server 一体化初始化脚本，负责：

1. 若不存在则创建 `daihaojie_xm` 数据库。
2. 切换到 `daihaojie_xm`。
3. 按依赖顺序删除旧表，保证脚本可重复执行。
4. 创建当前应用运行所需的全部表：
   - `sys_user`
   - `farm_plot`
   - `farm_crop`
   - `farm_material`
   - `stock_inventory`
   - `plant_batch`
   - `batch_status_log`
   - `plant_operation`
   - `plant_operation_material`
   - `stock_out_order`
   - `harvest_record`
   - `trace_public_field`
   - `ai_provider`
   - `ai_call_log`
   - `ai_bind_token`
   - `ai_recognize_log`
5. 插入现有 `data.sql` 中的初始化数据，并保持账号密码 `123456` 可登录。
6. 保留 SQL Server 触发器和视图脚本，但让主初始化脚本足够支撑应用直接运行。

应用配置改为默认连接 SQL Server，并关闭 Spring Boot 自动执行 H2 `schema.sql` / `data.sql` 的初始化行为，避免启动时重复建表或使用 H2 方言。

## Backend Compatibility Design

当前项目使用 `JdbcTemplate`，不引入大型持久层改造。只修正 SQL Server 下容易出问题的点：

- 新建作业后获取自增主键，避免使用 `select max(id)`。
- 使用 SQL Server 兼容的时间函数或 JDBC 参数。
- 保持查询结果字段映射兼容现有 Thymeleaf 模板。
- 保持现有登录、看板、库存扣减、采收校验、AI 日志和手机绑定流程不变。

## Frontend Design

采用“深色指挥中心”方向：

- 深色侧栏与主背景，突出系统级控制台感。
- KPI 卡片使用暗色表面、清晰边框、适度高亮，不使用过度装饰。
- 地块图、状态图、表格和表单保持足够对比度，确保后台长时间使用仍可读。
- 按钮、状态标签、提醒色使用绿色、琥珀色、红色等语义色，避免单一深绿色主题。
- 移动端和登录页也同步调整为同一视觉体系。

## Testing And Verification

验证路径：

- 运行 Maven 测试，确保核心服务规则不回退。
- 如本机 SQL Server 可访问，执行初始化脚本并启动应用。
- 使用 `owner / 123456` 登录。
- 验证 `/dashboard`、`/plots`、`/batches`、`/operations`、`/stock`、`/harvest`、`/providers`、`/mobile` 可访问。

如果本机 SQL Server 服务不可访问，则交付可直接执行的 SQL Server 初始化脚本和应用配置，并在结果中说明连接失败原因。

## Out Of Scope

- 不引入 Flyway。
- 不切换到 MyBatis-Plus Mapper。
- 不做完整 Sa-Token 或 Spring Security 改造。
- 不接入真实外部 AI Provider。
- 不做完整移动端 PWA。

## Self Review

- 数据库要求明确为 SQL Server 单一运行库，没有保留 H2 作为默认数据源。
- 初始化脚本覆盖当前应用全部运行表和种子数据。
- 后端改动集中在 SQL Server 兼容性，不扩大为持久层重构。
- 前端方向已按用户选择的 B 方案定稿。
