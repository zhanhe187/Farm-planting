-- SQL Server: keep farm operation events append-only.
-- Run after db-sqlserver.sql.
if object_id('dbo.trg_operation_block_update', 'TR') is not null
    drop trigger dbo.trg_operation_block_update;
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
