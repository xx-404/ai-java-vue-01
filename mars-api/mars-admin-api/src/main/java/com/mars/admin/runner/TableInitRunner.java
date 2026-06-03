package com.mars.admin.runner;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 数据库表初始化 Runner
 * 启动时自动创建缺少的表
 */
@Slf4j
@Component
@Order(1)
@RequiredArgsConstructor
public class TableInitRunner implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    @Override
    public void run(String... args) {
        initUserTablePreference();
    }

    /**
     * 初始化用户表格偏好设置表
     */
    private void initUserTablePreference() {
        try {
            String checkTableSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_user_table_preference'";
            Integer count = jdbcTemplate.queryForObject(checkTableSql, Integer.class);
            if (count != null && count > 0) {
                log.info("表 sys_user_table_preference 已存在，跳过创建");
                return;
            }

            String createTableSql = """
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
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表格偏好设置表'
                """;

            jdbcTemplate.execute(createTableSql);
            log.info("表 sys_user_table_preference 创建成功");
        } catch (Exception e) {
            log.error("初始化表 sys_user_table_preference 失败", e);
        }
    }
}
