# FPMS 智慧农场种植管理系统

这是一个基于 Spring Boot + Thymeleaf + SQL Server 的智慧农场种植管理系统，覆盖种植、农资、采收、溯源、销售和客户自助购买闭环。

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

手机扫码请让手机和电脑处于同一局域网。PC 端进入 `/mobile` 后，二维码会自动使用电脑的局域网 IP；也可以通过环境变量 `FPMS_MOBILE_BASE_URL` 手动指定，例如 `http://192.168.5.93:8080/m/bind`。

## 默认账号

初始化密码均为 `123456`。

| 账号 | 角色 |
| --- | --- |
| `admin` | SUPER_ADMIN |
| `owner` | FARM_OWNER |
| `agri` | AGRI_TECH |
| `warehouse` | WAREHOUSE |
| `worker` | FIELD_WORKER |
| `customer` | CUSTOMER |

权限按角色在菜单和后端路由同时拦截：只有 `SUPER_ADMIN` 可管理 AI 端点；`WAREHOUSE` 只能进入库存模块，并可在库存页新增种子绑定作物；公开注册账号默认为 `CUSTOMER / OWN_CUSTOMER`，可在“购买农作物”中自助购买在售作物，并查看本人客户名匹配的销售流水。

## 核心功能

- C1 批次生命周期状态机
- C2 农事作业事件流，SQL Server 触发器脚本禁止 UPDATE/DELETE
- C3 农资库存与作业联动扣减
- C4 农药安全间隔期预检与采收强校验
- C5 追溯码与免登录公开页
- C6 角色菜单与后端路由权限拦截
- C7 地块 Canvas 布局图
- C8 轻量驾驶舱图表
- C9 客户自助购买在售农作物并生成销售流水
- C10 AI 农事助手入口和 SSE-ready 接口
- C11 AI 端点配置、编辑、删除、测试与调用日志
- C12 手机 bind_token SQL Server 持久化绑定和拍照识作物

## AI 端点配置

管理员登录后进入 `AI 端点管理`：

- DeepSeek 适合作为 CHAT 文本对话端点：Base URL 填 `https://api.deepseek.com`，模型可填 `deepseek-chat`，场景选择 `CHAT`。
- 手机拍照识别需要单独配置支持图片输入的 OpenAI 兼容 VISION 端点，场景选择 `VISION`。
- API Key 只在服务端加密保存，页面仅显示脱敏值；编辑端点时 API Key 留空会保留原值。
- 默认初始化只提供禁用示例，不再启用本地 mock 端点。

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

测试覆盖：状态机、间隔期、库存扣减、客户购买流水、权限拦截、Prompt 脱敏、一次性 bind_token。

## 使用说明

系统运行逻辑、角色权限和业务操作说明见 [doc/系统使用说明文档.md](D:/javacode/fpms-mvp/doc/系统使用说明文档.md)。
