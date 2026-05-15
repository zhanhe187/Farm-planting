-- SQL Server Indexed View sample for dashboard acceleration.
if object_id('dbo.v_batch_yield_summary', 'V') is not null
    drop view dbo.v_batch_yield_summary;
go

create view dbo.v_batch_yield_summary
with schemabinding
as
select
    b.id as batch_id,
    b.crop_id,
    b.plot_id,
    count_big(*) as row_count,
    sum(isnull(h.quantity_kg, 0)) as total_quantity_kg
from dbo.plant_batch b
left join dbo.harvest_record h on h.batch_id = b.id
group by b.id, b.crop_id, b.plot_id;
go

create unique clustered index ix_v_batch_yield_summary
on dbo.v_batch_yield_summary(batch_id);
go
