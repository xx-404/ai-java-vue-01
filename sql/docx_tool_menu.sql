-- DOCX工具 菜单 SQL
-- 菜单类型: 1-目录 2-菜单 3-按钮

-- 获取开发工具菜单ID
SET @toolMenuId = (SELECT IFNULL((SELECT id FROM sys_menu WHERE name = '开发工具' AND deleted = 0 LIMIT 1), 0));

-- DOCX工具 菜单 (type=2)
INSERT INTO sys_menu (parent_id, name, type, path, component, permission, icon, sort, visible, status, is_frame, deleted)
VALUES (@toolMenuId, 'DOCX工具', 2, '/tool/docx', '/tool/docx/index', '', 'DocumentTextOutline', 2, 1, 1, 0, 0);
