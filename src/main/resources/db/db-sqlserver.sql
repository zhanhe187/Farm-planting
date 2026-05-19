-- Core SQL Server schema sketch. The complete runnable setup is db/init-daihaojie-xm.sql.
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

create table dbo.stock_in_order (
    id bigint identity(1,1) primary key,
    material_id bigint not null,
    quantity decimal(14,2) not null,
    unit_price decimal(14,2) not null,
    total_amount decimal(14,2) not null,
    created_at datetime2 not null default sysdatetime()
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
