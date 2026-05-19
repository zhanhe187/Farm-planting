if db_id(N'daihaojie_xm') is null
begin
    create database daihaojie_xm;
end;
go

use daihaojie_xm;
go

if object_id(N'dbo.v_batch_yield_summary', N'V') is not null
    drop view dbo.v_batch_yield_summary;
go

if object_id(N'dbo.trg_operation_block_update', N'TR') is not null
    drop trigger dbo.trg_operation_block_update;
go

drop table if exists dbo.ai_recognize_log;
drop table if exists dbo.ai_bind_token;
drop table if exists dbo.ai_call_log;
drop table if exists dbo.ai_provider;
drop table if exists dbo.sale_order;
drop table if exists dbo.trace_public_field;
drop table if exists dbo.harvest_record;
drop table if exists dbo.stock_in_order;
drop table if exists dbo.stock_out_order;
drop table if exists dbo.plant_operation_material;
drop table if exists dbo.plant_operation;
drop table if exists dbo.batch_status_log;
drop table if exists dbo.plant_batch;
drop table if exists dbo.stock_inventory;
drop table if exists dbo.farm_material;
drop table if exists dbo.farm_crop;
drop table if exists dbo.farm_plot;
drop table if exists dbo.sys_user;
go

create table dbo.sys_user (
    id bigint identity(1,1) primary key,
    username varchar(50) not null unique,
    password varchar(120) not null,
    display_name nvarchar(80) not null,
    role_code varchar(40) not null,
    data_scope varchar(40) not null,
    enabled bit not null default 1
);

create table dbo.farm_plot (
    id bigint identity(1,1) primary key,
    name nvarchar(80) not null,
    area_mu decimal(12,2) not null,
    soil_type nvarchar(80),
    owner_id bigint,
    layout_x int not null,
    layout_y int not null,
    width int not null,
    height int not null,
    status varchar(40) not null
);

create table dbo.farm_crop (
    id bigint identity(1,1) primary key,
    name nvarchar(80) not null,
    variety nvarchar(80),
    min_growth_days int not null,
    sale_price_per_kg decimal(14,2) not null default 0,
    image_url nvarchar(300),
    enabled bit not null default 1
);

create table dbo.farm_material (
    id bigint identity(1,1) primary key,
    name nvarchar(100) not null,
    category varchar(40) not null,
    unit nvarchar(20) not null,
    safe_interval_days int not null default 0,
    unit_price decimal(14,2) not null default 0,
    crop_id bigint,
    enabled bit not null default 1
);

create table dbo.stock_inventory (
    id bigint identity(1,1) primary key,
    material_id bigint not null,
    quantity decimal(14,2) not null,
    safety_stock decimal(14,2) not null,
    version int not null default 0
);

create table dbo.plant_batch (
    id bigint identity(1,1) primary key,
    batch_no varchar(80) not null unique,
    plot_id bigint not null,
    crop_id bigint not null,
    status varchar(40) not null,
    planned_area_mu decimal(12,2) not null,
    sow_date date,
    expected_harvest_date date,
    actual_harvest_date date,
    trace_code varchar(80),
    owner_id bigint,
    created_at datetime2 not null default sysdatetime()
);

create table dbo.batch_status_log (
    id bigint identity(1,1) primary key,
    batch_id bigint not null,
    from_status varchar(40),
    to_status varchar(40) not null,
    operator_name nvarchar(80) not null,
    reason nvarchar(300),
    created_at datetime2 not null default sysdatetime()
);

create table dbo.plant_operation (
    id bigint identity(1,1) primary key,
    batch_id bigint not null,
    type varchar(40) not null,
    operation_date date not null,
    worker_name nvarchar(80) not null,
    note nvarchar(500),
    is_locked bit not null default 0,
    ref_operation_id bigint,
    created_at datetime2 not null default sysdatetime()
);

create table dbo.plant_operation_material (
    id bigint identity(1,1) primary key,
    operation_id bigint not null,
    material_id bigint not null,
    quantity decimal(14,2) not null
);

create table dbo.stock_out_order (
    id bigint identity(1,1) primary key,
    material_id bigint not null,
    quantity decimal(14,2) not null,
    type varchar(40) not null,
    operation_id bigint,
    created_at datetime2 not null default sysdatetime()
);

create table dbo.stock_in_order (
    id bigint identity(1,1) primary key,
    material_id bigint not null,
    quantity decimal(14,2) not null,
    unit_price decimal(14,2) not null,
    total_amount decimal(14,2) not null,
    created_at datetime2 not null default sysdatetime()
);

create table dbo.harvest_record (
    id bigint identity(1,1) primary key,
    batch_id bigint not null,
    harvest_date date not null,
    quantity_kg decimal(14,2) not null,
    quality_grade varchar(40) not null,
    trace_code varchar(80) not null,
    created_at datetime2 not null default sysdatetime()
);

create table dbo.sale_order (
    id bigint identity(1,1) primary key,
    batch_id bigint,
    customer_name nvarchar(120) not null,
    product_name nvarchar(120) not null,
    quantity_kg decimal(14,2) not null,
    unit_price decimal(14,2) not null,
    total_amount decimal(14,2) not null,
    sale_date date not null,
    note nvarchar(300),
    created_at datetime2 not null default sysdatetime()
);

create table dbo.trace_public_field (
    id bigint identity(1,1) primary key,
    field_key varchar(80) not null,
    public_enabled bit not null
);

create table dbo.ai_provider (
    id bigint identity(1,1) primary key,
    name nvarchar(120) not null,
    provider_type varchar(40) not null,
    base_url nvarchar(300) not null,
    encrypted_api_key varbinary(max) null,
    api_key_mask varchar(80) not null,
    default_model nvarchar(120) not null,
    scene varchar(40) not null,
    priority int not null,
    timeout_ms int not null default 60000,
    enabled bit not null default 1,
    created_at datetime2 not null default sysdatetime()
);

create table dbo.ai_call_log (
    id bigint identity(1,1) primary key,
    provider_id bigint null,
    scene varchar(40) not null,
    prompt_summary nvarchar(500),
    success bit not null,
    elapsed_ms int not null,
    error_code nvarchar(120),
    created_at datetime2 not null default sysdatetime()
);

create table dbo.ai_bind_token (
    id bigint identity(1,1) primary key,
    token varchar(80) not null unique,
    pc_session_id varchar(120) not null,
    expires_at datetime2 not null,
    used bit not null default 0,
    used_ip varchar(80),
    used_ua nvarchar(1000)
);

create table dbo.ai_recognize_log (
    id bigint identity(1,1) primary key,
    image_hash varchar(120),
    crop_name nvarchar(80),
    result_json nvarchar(max),
    created_at datetime2 not null default sysdatetime()
);
go

insert into dbo.sys_user(username, password, display_name, role_code, data_scope) values
('admin', '123456', N'系统管理员', 'SUPER_ADMIN', 'ALL'),
('owner', '123456', N'青禾农场主', 'FARM_OWNER', 'ALL'),
('agri', '123456', N'农技员小林', 'AGRI_TECH', 'OWN_PLOT'),
('warehouse', '123456', N'仓库管理员', 'WAREHOUSE', 'ALL'),
('worker', '123456', N'田间工人阿强', 'FIELD_WORKER', 'ASSIGNED_TASK'),
('customer', '123456', N'经销客户', 'CUSTOMER', 'OWN_CUSTOMER');

insert into dbo.farm_plot(name, area_mu, soil_type, owner_id, layout_x, layout_y, width, height, status) values
(N'A1 号温室', 8.50, N'壤土', 3, 42, 35, 180, 108, N'生长期'),
(N'B2 露天地', 15.20, N'沙壤土', 3, 255, 55, 245, 126, N'待采收'),
(N'C3 试验田', 6.80, N'黑土', 2, 95, 190, 160, 110, N'已计划'),
(N'D4 果蔬区', 12.00, N'黏壤土', 2, 295, 220, 205, 130, N'已完结'),
(N'E5 有机菜园', 5.50, N'壤土', 3, 400, 60, 190, 140, N'已废弃'),
(N'F6 冬暖大棚', 10.00, N'沙壤土', 2, 520, 220, 220, 155, N'已计划');

insert into dbo.farm_crop(name, variety, min_growth_days, sale_price_per_kg, image_url) values
(N'番茄', N'粉果 618', 80, 8.80, N'https://images.unsplash.com/photo-1592924357228-91a4daadcfea?auto=format&fit=crop&w=900&q=80'),
(N'黄瓜', N'津优 35', 55, 5.20, N'https://images.unsplash.com/photo-1449300079323-02e209d9d3a6?auto=format&fit=crop&w=900&q=80'),
(N'草莓', N'红颜', 95, 22.00, N'https://images.unsplash.com/photo-1464965911861-746a04b4bca6?auto=format&fit=crop&w=900&q=80'),
(N'白菜', N'胶州大白菜', 55, 3.20, null),
(N'生菜', N'意大利生菜', 40, 4.60, null),
(N'萝卜', N'潍县青萝卜', 75, 2.80, null),
(N'辣椒', N'线椒王', 90, 9.50, null);

insert into dbo.farm_material(name, category, unit, safe_interval_days, unit_price, crop_id) values
(N'番茄种子', N'种子', N'袋', 0, 18.50, 1),
(N'有机复合肥', N'化肥', N'kg', 0, 2.60, null),
(N'吡虫啉水分散粒剂', N'农药', N'g', 7, 0.18, null),
(N'代森锰锌可湿性粉剂', N'农药', N'g', 10, 0.12, null),
(N'黄瓜种子', N'种子', N'袋', 0, 16.80, 2),
(N'草莓种苗', N'种子', N'株', 0, 0.85, 3),
(N'白菜种子', N'种子', N'袋', 0, 12.00, 4),
(N'辣椒种子', N'种子', N'袋', 0, 22.00, 7),
(N'生物有机肥', N'化肥', N'kg', 0, 1.80, null),
(N'农用硫酸钾', N'化肥', N'kg', 0, 4.20, null),
(N'高效氯氟氰菊酯', N'农药', N'ml', 14, 0.06, null),
(N'阿维菌素乳油', N'农药', N'ml', 7, 0.08, null),
(N'黑色地膜', N'其他', N'卷', 0, 85.00, null);

insert into dbo.stock_inventory(material_id, quantity, safety_stock, version) values
(1, 38, 8, 0),
(2, 460, 100, 0),
(3, 740, 200, 0),
(4, 640, 120, 0),
(5, 17, 5, 0),
(6, 4500, 2000, 0),
(7, 3, 5, 0),
(8, 3, 5, 0),
(9, 230, 80, 0),
(10, 180, 60, 0),
(11, 800, 300, 0),
(12, 450, 200, 0),
(13, 2, 3, 0);

insert into dbo.stock_in_order(material_id, quantity, unit_price, total_amount) values
(1, 38, 18.50, 703.00),
(2, 460, 2.60, 1196.00),
(3, 740, 0.18, 133.20),
(4, 640, 0.12, 76.80),
(5, 17, 16.80, 285.60),
(6, 4500, 0.85, 3825.00),
(7, 3, 12.00, 36.00),
(8, 3, 22.00, 66.00),
(9, 230, 1.80, 414.00),
(10, 180, 4.20, 756.00),
(11, 800, 0.06, 48.00),
(12, 450, 0.08, 36.00),
(13, 2, 85.00, 170.00);

insert into dbo.plant_batch(batch_no, plot_id, crop_id, status, planned_area_mu, sow_date, expected_harvest_date, owner_id, trace_code) values
(N'2026春-番茄-1号', 1, 1, N'生长期', 8.50, '2026-03-01', '2026-05-28', 3, null),
(N'2026春-黄瓜-2号', 2, 2, N'待采收', 15.20, '2026-03-20', '2026-05-16', 3, null),
(N'2026秋-草莓-1号', 3, 3, N'已计划', 6.80, null, '2026-08-20', 2, null),
(N'2026春-白菜-1号', 4, 4, N'已完结', 12.00, '2026-03-05', '2026-04-30', 2, 'FPMS-BC-20260428-004'),
(N'2026春-辣椒-1号', 5, 7, N'已废弃', 5.50, '2026-03-15', '2026-06-20', 3, null),
(N'2026夏-黄瓜-1号', 6, 2, N'已计划', 10.00, null, '2026-07-25', 2, null);

insert into dbo.batch_status_log(batch_id, from_status, to_status, operator_name, reason) values
(1, N'已计划', N'已播种', N'农技员小林', N'穴盘育苗完成，移栽至A1号温室'),
(1, N'已播种', N'生长期', N'农技员小林', N'幼苗成活率95%，进入生长期'),
(2, N'已计划', N'已播种', N'农技员小林', N'直播完成，覆盖地膜'),
(2, N'已播种', N'生长期', N'农技员小林', N'出苗整齐，进入生长期'),
(2, N'生长期', N'待采收', N'农技员小林', N'果实饱满，达到商品采收标准'),
(4, N'已计划', N'已播种', N'农技员小林', N'直播大白菜完成'),
(4, N'已播种', N'生长期', N'农技员小林', N'出苗率达90%'),
(4, N'生长期', N'待采收', N'农技员小林', N'包心紧实，可采收'),
(4, N'待采收', N'采收中', N'农技员小林', N'开始采收作业'),
(4, N'采收中', N'已完结', N'农技员小林', N'采收完成，地块清园'),
(5, N'已计划', N'已播种', N'农技员小林', N'辣椒直播完成'),
(5, N'已播种', N'生长期', N'田间工人阿强', N'出苗70%，部分缺苗'),
(5, N'生长期', N'已废弃', N'农技员小林', N'大面积疫病爆发，救治无效，决定废弃');

insert into dbo.plant_operation(batch_id, type, operation_date, worker_name, note) values
-- 2026春-番茄-1号 农事记录
(1, N'播种', '2026-03-01', N'田间工人阿强', N'穴盘育苗，基质消毒后移栽至A1号温室，株距40cm'),
(1, N'灌溉', '2026-03-05', N'田间工人阿强', N'定根水浇透，滴灌运行2小时'),
(1, N'施肥', '2026-03-28', N'田间工人阿强', N'苗期追施有机复合肥30kg，配合滴灌施肥'),
(1, N'灌溉', '2026-04-10', N'田间工人阿强', N'花期滴灌，保持土壤湿润'),
(1, N'病虫害巡检', '2026-04-18', N'农技员小林', N'巡检发现少量蚜虫，标记待处理'),
(1, N'施药', '2026-04-20', N'田间工人阿强', N'喷施吡虫啉防治蚜虫，重点处理嫩梢'),
(1, N'除草', '2026-05-05', N'田间工人阿强', N'人工清除温室杂草，结合中耕松土'),
-- 2026春-黄瓜-2号 农事记录
(2, N'播种', '2026-03-20', N'田间工人阿强', N'直播黄瓜种子，覆黑色地膜，每穴2粒'),
(2, N'灌溉', '2026-03-25', N'田间工人阿强', N'出苗后滴灌浇水，保持床面湿润'),
(2, N'施肥', '2026-04-10', N'田间工人阿强', N'追施有机复合肥50kg，配合农用硫酸钾20kg'),
(2, N'病虫害巡检', '2026-04-28', N'农技员小林', N'巡检发现霜霉病早期症状，建议预防性施药'),
(2, N'施药', '2026-05-01', N'田间工人阿强', N'喷施代森锰锌预防霜霉病，覆盖全株'),
(2, N'灌溉', '2026-05-08', N'田间工人阿强', N'膨瓜期加大滴灌量，每2天浇一次'),
(2, N'除草', '2026-05-12', N'田间工人阿强', N'清除垄间杂草，改善通风透光'),
-- 2026春-白菜-1号 农事记录
(4, N'播种', '2026-03-05', N'田间工人阿强', N'直播大白菜种子，条播，覆土1cm'),
(4, N'灌溉', '2026-03-12', N'田间工人阿强', N'出苗后浇水，保持土壤湿润'),
(4, N'施肥', '2026-03-25', N'田间工人阿强', N'追施有机复合肥40kg，促进莲座期生长'),
(4, N'病虫害巡检', '2026-04-05', N'农技员小林', N'巡查菜青虫情况，未见异常'),
(4, N'灌溉', '2026-04-15', N'田间工人阿强', N'包心期滴灌浇水，保证水分充足'),
(4, N'采收', '2026-04-28', N'田间工人阿强', N'采收大白菜，单株均重约2.5kg，品质良好'),
-- 2026春-辣椒-1号 农事记录
(5, N'播种', '2026-03-15', N'田间工人阿强', N'辣椒种子直播，行距50cm，覆土后镇压'),
(5, N'灌溉', '2026-03-22', N'田间工人阿强', N'出苗后滴灌，出苗率偏低约70%'),
(5, N'病虫害巡检', '2026-04-10', N'农技员小林', N'巡检发现大面积辣椒疫病，茎基部变褐腐烂');

insert into dbo.plant_operation_material(operation_id, material_id, quantity) values
-- 番茄播种: 番茄种子 2袋
(1, 1, 2),
-- 番茄施肥: 有机复合肥 30kg
(3, 2, 30),
-- 番茄施药: 吡虫啉 60g
(6, 3, 60),
-- 黄瓜播种: 黄瓜种子 3袋
(8, 5, 3),
-- 黄瓜施肥: 有机复合肥 50kg + 硫酸钾 20kg
(10, 2, 50),
(10, 10, 20),
-- 黄瓜施药: 代森锰锌 60g
(12, 4, 60),
-- 白菜播种: 白菜种子 3袋
(15, 7, 3),
-- 白菜施肥: 有机复合肥 40kg
(17, 2, 40),
-- 辣椒播种: 辣椒种子 2袋
(21, 8, 2);

insert into dbo.trace_public_field(field_key, public_enabled) values
('cropName', 1),
('plotName', 1),
('harvestDate', 1),
('operationTimeline', 1),
('materialBrand', 0),
('cost', 0);

insert into dbo.harvest_record(batch_id, harvest_date, quantity_kg, quality_grade, trace_code) values
(4, '2026-04-28', 800, N'一级', 'FPMS-BC-20260428-004');

insert into dbo.ai_provider(name, provider_type, base_url, api_key_mask, default_model, scene, priority, enabled) values
(N'深度求索对话示例', 'OPENAI_COMPATIBLE', N'https://api.deepseek.com', N'未配置', N'deepseek-chat', 'CHAT', 10, 0),
(N'图片识别示例', 'OPENAI_COMPATIBLE', N'https://your-vision-api.example.com/v1', N'未配置', N'qwen-vl-plus', 'VISION', 10, 0);

insert into dbo.sale_order(batch_id, customer_name, product_name, quantity_kg, unit_price, total_amount, sale_date, note) values
(4, N'农贸市场-张老板', N'胶州大白菜', 500, 2.50, 1250.00, '2026-04-29', N'包心紧实品质好'),
(4, N'社区团购-李姐', N'胶州大白菜', 200, 3.00, 600.00, '2026-05-02', N'社区直供新鲜蔬菜'),
(4, N'永辉超市', N'胶州大白菜', 100, 3.20, 320.00, '2026-05-03', N'超市上架销售');
go

create trigger dbo.trg_operation_block_update
on dbo.plant_operation
after update, delete
as
begin
    raiserror(N'农事作业事件不可修改或删除，请追加“红冲修正”事件。', 16, 1);
    rollback transaction;
end;
go

set ansi_nulls on;
go
set quoted_identifier on;
go

create view dbo.v_batch_yield_summary
with schemabinding
as
select
    h.batch_id,
    b.crop_id,
    b.plot_id,
    count_big(*) as row_count,
    sum(h.quantity_kg) as total_quantity_kg
from dbo.harvest_record h
join dbo.plant_batch b on b.id = h.batch_id
group by h.batch_id, b.crop_id, b.plot_id;
go

set ansi_nulls on;
set quoted_identifier on;
set ansi_padding on;
set ansi_warnings on;
set concat_null_yields_null on;
set arithabort on;
set numeric_roundabort off;
go

create unique clustered index ix_v_batch_yield_summary
on dbo.v_batch_yield_summary(batch_id);
go
