-- SQL Server AI support tables from the planning document.
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
    prompt_summary nvarchar(500) null,
    success bit not null,
    elapsed_ms int not null,
    error_code nvarchar(120) null,
    created_at datetime2 not null default sysdatetime()
);

create table dbo.ai_bind_token (
    id bigint identity(1,1) primary key,
    token varchar(80) not null unique,
    pc_session_id varchar(120) not null,
    expires_at datetime2 not null,
    used bit not null default 0,
    used_ip varchar(80) null,
    used_ua nvarchar(300) null
);
