-- 用户表格偏好设置表
-- 存储不同用户在不同菜单页面的表格列配置、分页设置等个性化习惯

DROP TABLE IF EXISTS `sys_user_table_preference`;
CREATE TABLE `sys_user_table_preference` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id` bigint NOT NULL COMMENT '用户ID',
    `page_key` varchar(100) NOT NULL COMMENT '页面标识(如: system/user)',
    `columns_config` text COMMENT '列配置JSON',
    `page_size` int DEFAULT 10 COMMENT '每页条数',
    `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    `create_by` bigint COMMENT '创建人',
    `update_by` bigint COMMENT '更新人',
    `deleted` tinyint DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_page` (`user_id`, `page_key`, `deleted`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_page_key` (`page_key`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表格偏好设置表';
