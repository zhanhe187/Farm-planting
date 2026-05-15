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
    image_url nvarchar(300)
);

create table dbo.farm_material (
    id bigint identity(1,1) primary key,
    name nvarchar(100) not null,
    category varchar(40) not null,
    unit nvarchar(20) not null,
    safe_interval_days int not null default 0,
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
    used_ua nvarchar(300)
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
(N'A1 号温室', 8.50, N'壤土', 3, 42, 35, 180, 108, 'GROWING'),
(N'B2 露天地', 15.20, N'沙壤土', 3, 255, 55, 245, 126, 'READY_HARVEST'),
(N'C3 试验田', 6.80, N'黑土', 2, 95, 190, 160, 110, 'PLANNED'),
(N'D4 果蔬区', 12.00, N'黏壤土', 2, 295, 220, 205, 130, 'GROWING');

insert into dbo.farm_crop(name, variety, min_growth_days, image_url) values
(N'番茄', N'粉果 618', 80, N'https://images.unsplash.com/photo-1592924357228-91a4daadcfea?auto=format&fit=crop&w=900&q=80'),
(N'黄瓜', N'津优 35', 55, N'https://images.unsplash.com/photo-1449300079323-02e209d9d3a6?auto=format&fit=crop&w=900&q=80'),
(N'草莓', N'红颜', 95, N'https://images.unsplash.com/photo-1464965911861-746a04b4bca6?auto=format&fit=crop&w=900&q=80');

insert into dbo.farm_material(name, category, unit, safe_interval_days) values
(N'番茄种子', 'SEED', N'袋', 0),
(N'有机复合肥', 'FERTILIZER', N'kg', 0),
(N'吡虫啉水分散粒剂', 'PESTICIDE', N'g', 7),
(N'代森锰锌可湿性粉剂', 'PESTICIDE', N'g', 10);

insert into dbo.stock_inventory(material_id, quantity, safety_stock, version) values
(1, 40, 8, 0),
(2, 520, 100, 0),
(3, 900, 200, 0),
(4, 650, 120, 0);

insert into dbo.plant_batch(batch_no, plot_id, crop_id, status, planned_area_mu, sow_date, expected_harvest_date, owner_id, trace_code) values
('BATCH-20260501-TOMATO', 1, 1, 'GROWING', 8.50, '2026-03-01', '2026-05-28', 3, null),
('BATCH-20260415-CUCUMBER', 2, 2, 'READY_HARVEST', 15.20, '2026-03-20', '2026-05-16', 3, null),
('BATCH-20260510-STRAWBERRY', 3, 3, 'PLANNED', 6.80, null, '2026-08-20', 2, null);

insert into dbo.batch_status_log(batch_id, from_status, to_status, operator_name, reason) values
(1, 'PLANNED', 'SOWED', N'农技员小林', N'完成播种'),
(1, 'SOWED', 'GROWING', N'农技员小林', N'出苗稳定'),
(2, 'GROWING', 'READY_HARVEST', N'农技员小林', N'达到采收标准');

insert into dbo.plant_operation(batch_id, type, operation_date, worker_name, note) values
(1, 'SOWING', '2026-03-01', N'田间工人阿强', N'穴盘育苗后移栽'),
(1, 'FERTILIZE', '2026-04-16', N'田间工人阿强', N'追施有机复合肥，长势良好'),
(1, 'PESTICIDE', '2026-05-10', N'田间工人阿强', N'发现蚜虫点片发生，局部防治'),
(2, 'SOWING', '2026-03-20', N'田间工人阿强', N'黄瓜直播'),
(2, 'PESTICIDE', '2026-05-01', N'田间工人阿强', N'霜霉病预防性处理');

insert into dbo.plant_operation_material(operation_id, material_id, quantity) values
(1, 1, 2),
(2, 2, 45),
(3, 3, 80),
(5, 4, 60);

insert into dbo.trace_public_field(field_key, public_enabled) values
('cropName', 1),
('plotName', 1),
('harvestDate', 1),
('operationTimeline', 1),
('materialBrand', 0),
('cost', 0);

insert into dbo.ai_provider(name, provider_type, base_url, api_key_mask, default_model, scene, priority, enabled) values
(N'本地-Chat', 'LOCAL_FALLBACK', N'http://localhost:8080/mock-ai', 'local-****-key', N'fpms-local-advisor', 'CHAT', 10, 1),
(N'本地-Vision', 'LOCAL_FALLBACK', N'http://localhost:8080/mock-vision', 'local-****-key', N'fpms-local-vision', 'VISION', 10, 1);

insert into dbo.sale_order(batch_id, customer_name, product_name, quantity_kg, unit_price, total_amount, sale_date, note) values
(2, N'永辉超市', N'津优35黄瓜', 500, 4.50, 2250.00, '2026-05-12', N'首批采收供应'),
(2, N'社区团购-李姐', N'津优35黄瓜', 120, 5.00, 600.00, '2026-05-14', N'社区直供'),
(1, N'农贸市场-张老板', N'粉果618番茄', 200, 6.00, 1200.00, '2026-05-10', N'品质优选');
go

create trigger dbo.trg_operation_block_update
on dbo.plant_operation
after update, delete
as
begin
    raiserror('Operation log is immutable. Insert a CORRECTION event instead.', 16, 1);
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
