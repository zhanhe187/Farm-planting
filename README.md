# FPMS 智慧农场种植管理系统

这是根据 `D:\javacode\农场种植管理系统-规划文档.md` 编写的 Java Web 项目。

## 技术栈

- Java 8 target / 本机 JDK 17 可运行
- Spring Boot 2.7.18
- Thymeleaf + 本地 CSS（田间指挥台风格）
- SQL Server 运行库
- SQL Server 初始化脚本位于 `src/main/resources/db/init-daihaojie-xm.sql`
- Maven 构建

## 数据库初始化

本项目运行数据全部保存在 SQL Server 数据库 `daihaojie_xm` 中。

连接信息：

- JDBC URL：`jdbc:sqlserver://localhost:1433;databaseName=daihaojie_xm;encrypt=true;trustServerCertificate=true`
- 用户名：`sa`
- 密码：`123456`

首次运行前执行：

```powershell
cd D:\javacode\fpms-mvp
sqlcmd -S localhost -U sa -P 123456 -C -b -f 65001 -i src\main\resources\db\init-daihaojie-xm.sql
```

## 启动应用

```powershell
cd D:\javacode\fpms-mvp
mvn spring-boot:run
```

访问：

- 后台：`http://localhost:8080`

## 默认账号

本地初始化账号见 [落地部署与运行说明](D:/javacode/fpms-mvp/docs/落地部署与运行说明.md)。初始化密码为 `123456`。

| 账号 | 角色 |
| --- | --- |
| `admin` | SUPER_ADMIN |
| `owner` | FARM_OWNER |
| `agri` | AGRI_TECH |
| `warehouse` | WAREHOUSE |
| `worker` | FIELD_WORKER |
| `customer` | CUSTOMER |

## 已实现的规划文档核心点

- C1 批次生命周期状态机
- C2 农事作业事件流，SQL Server 触发器脚本禁止 UPDATE/DELETE
- C3 农资库存与作业联动扣减
- C4 农药安全间隔期预检与采收强校验
- C5 追溯码与免登录公开页
- C7 地块 Canvas 布局图
- C8 轻量驾驶舱图表
- C10 AI 农事助手入口和 SSE-ready 接口
- C11 AI 端点配置与调用日志
- C12 手机 bind_token 绑定和拍照识作物

## 重要脚本

- `src/main/resources/db/init-daihaojie-xm.sql`：SQL Server 建库、建表、触发器、视图与初始化数据
- `src/main/resources/schema.sql`：历史本地建表脚本，不作为当前运行库入口
- `src/main/resources/data.sql`：历史初始化数据来源，不作为当前运行库入口
- `src/main/resources/db/db-sqlserver.sql`：SQL Server 核心表结构
- `src/main/resources/db/db-triggers.sql`：不可篡改事件触发器
- `src/main/resources/db/db-views.sql`：Indexed View 示例
- `src/main/resources/db/db-ai.sql`：AI 端点和调用日志表

## 测试

```powershell
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2026.1\apache-maven-3.9.10\bin\mvn.cmd' test
```

测试覆盖：状态机、间隔期、库存扣减、Prompt 脱敏、一次性 bind_token。

## 落地说明

部署、初始化账号、SQL Server 切换和生产增强建议见 [docs/落地部署与运行说明.md](D:/javacode/fpms-mvp/docs/落地部署与运行说明.md)。
