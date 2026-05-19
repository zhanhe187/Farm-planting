insert into sys_user(username, password, display_name, role_code, data_scope) values
('admin', '123456', '系统管理员', 'SUPER_ADMIN', 'ALL'),
('owner', '123456', '青禾农场主', 'FARM_OWNER', 'ALL'),
('agri', '123456', '农技员小林', 'AGRI_TECH', 'OWN_PLOT'),
('warehouse', '123456', '仓库管理员', 'WAREHOUSE', 'ALL'),
('worker', '123456', '田间工人阿强', 'FIELD_WORKER', 'ASSIGNED_TASK'),
('customer', '123456', '经销客户', 'CUSTOMER', 'OWN_CUSTOMER');

insert into farm_plot(name, area_mu, soil_type, owner_id, layout_x, layout_y, width, height, status) values
('A1 号温室', 8.50, '壤土', 3, 42, 35, 180, 108, '生长期'),
('B2 露天地', 15.20, '沙壤土', 3, 255, 55, 245, 126, '待采收'),
('C3 试验田', 6.80, '黑土', 2, 95, 190, 160, 110, '已计划'),
('D4 果蔬区', 12.00, '黏壤土', 2, 295, 220, 205, 130, '生长期');

insert into farm_crop(name, variety, min_growth_days, sale_price_per_kg, image_url) values
('番茄', '粉果 618', 80, 8.80, 'https://images.unsplash.com/photo-1592924357228-91a4daadcfea?auto=format&fit=crop&w=900&q=80'),
('黄瓜', '津优 35', 55, 5.20, 'https://images.unsplash.com/photo-1449300079323-02e209d9d3a6?auto=format&fit=crop&w=900&q=80'),
('草莓', '红颜', 95, 22.00, 'https://images.unsplash.com/photo-1464965911861-746a04b4bca6?auto=format&fit=crop&w=900&q=80');

insert into farm_material(name, category, unit, safe_interval_days, unit_price, crop_id) values
('番茄种子', '种子', '袋', 0, 18.50, 1),
('有机复合肥', '化肥', 'kg', 0, 2.60, null),
('吡虫啉水分散粒剂', '农药', 'g', 7, 0.18, null),
('代森锰锌可湿性粉剂', '农药', 'g', 10, 0.12, null);

insert into stock_inventory(material_id, quantity, safety_stock, version) values
(1, 40, 8, 0),
(2, 520, 100, 0),
(3, 900, 200, 0),
(4, 650, 120, 0);

insert into stock_in_order(material_id, quantity, unit_price, total_amount) values
(1, 40, 18.50, 740.00),
(2, 520, 2.60, 1352.00),
(3, 900, 0.18, 162.00),
(4, 650, 0.12, 78.00);

insert into plant_batch(batch_no, plot_id, crop_id, status, planned_area_mu, sow_date, expected_harvest_date, owner_id, trace_code) values
('2026春-番茄-1号', 1, 1, '生长期', 8.50, '2026-03-01', '2026-05-28', 3, null),
('2026春-黄瓜-2号', 2, 2, '待采收', 15.20, '2026-03-20', '2026-05-16', 3, null),
('2026秋-草莓-1号', 3, 3, '已计划', 6.80, null, '2026-08-20', 2, null);

insert into batch_status_log(batch_id, from_status, to_status, operator_name, reason) values
(1, '已计划', '已播种', '农技员小林', '完成播种'),
(1, '已播种', '生长期', '农技员小林', '出苗稳定'),
(2, '生长期', '待采收', '农技员小林', '达到采收标准');

insert into plant_operation(batch_id, type, operation_date, worker_name, note) values
(1, '播种', '2026-03-01', '田间工人阿强', '穴盘育苗后移栽'),
(1, '施肥', '2026-04-16', '田间工人阿强', '追施有机复合肥，长势良好'),
(1, '施药', '2026-05-10', '田间工人阿强', '发现蚜虫点片发生，局部防治'),
(2, '播种', '2026-03-20', '田间工人阿强', '黄瓜直播'),
(2, '施药', '2026-05-01', '田间工人阿强', '霜霉病预防性处理');

insert into plant_operation_material(operation_id, material_id, quantity) values
(1, 1, 2),
(2, 2, 45),
(3, 3, 80),
(5, 4, 60);

insert into trace_public_field(field_key, public_enabled) values
('cropName', 1),
('plotName', 1),
('harvestDate', 1),
('operationTimeline', 1),
('materialBrand', 0),
('cost', 0);

insert into ai_provider(name, provider_type, base_url, api_key_mask, default_model, scene, priority, enabled) values
('深度求索对话示例', 'OPENAI_COMPATIBLE', 'https://api.deepseek.com', '未配置', 'deepseek-chat', 'CHAT', 10, 0),
('图片识别示例', 'OPENAI_COMPATIBLE', 'https://your-vision-api.example.com/v1', '未配置', 'qwen-vl-plus', 'VISION', 10, 0);
