package com.mars.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class InitAlertTables {

    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/test01?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8&allowMultiQueries=true";
        String username = "root";
        String password = "root123";

        String sql = """
            CREATE TABLE IF NOT EXISTS `sys_alert_config` (
                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                `alert_type` varchar(50) NOT NULL COMMENT '告警类型(cpu/memory/disk)',
                `metric_key` varchar(100) DEFAULT NULL COMMENT '指标键(如disk路径C:\\\\)',
                `threshold` decimal(10,2) NOT NULL COMMENT '阈值(百分比)',
                `enabled` tinyint DEFAULT 1 COMMENT '是否启用(0-禁用 1-启用)',
                `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                `create_by` bigint COMMENT '创建人',
                `update_by` bigint COMMENT '更新人',
                `deleted` tinyint DEFAULT 0 COMMENT '删除标识(0-未删除 1-已删除)',
                PRIMARY KEY (`id`),
                UNIQUE KEY `uk_type_key` (`alert_type`, `metric_key`, `deleted`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警阈值配置表';

            CREATE TABLE IF NOT EXISTS `sys_alert_record` (
                `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
                `alert_type` varchar(50) NOT NULL COMMENT '告警类型(cpu/memory/disk)',
                `metric_key` varchar(100) DEFAULT NULL COMMENT '指标键',
                `current_value` decimal(10,2) NOT NULL COMMENT '当前值',
                `threshold` decimal(10,2) NOT NULL COMMENT '阈值',
                `status` tinyint NOT NULL DEFAULT 1 COMMENT '状态(1-告警中 2-已恢复)',
                `trigger_time` datetime NOT NULL COMMENT '触发时间',
                `recover_time` datetime DEFAULT NULL COMMENT '恢复时间',
                `create_time` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                `update_time` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                PRIMARY KEY (`id`),
                KEY `idx_type_status` (`alert_type`, `status`),
                KEY `idx_trigger_time` (`trigger_time`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='告警记录表';

            INSERT IGNORE INTO `sys_alert_config` (`alert_type`, `metric_key`, `threshold`, `enabled`) VALUES
            ('cpu', NULL, 80.00, 1),
            ('memory', NULL, 85.00, 1),
            ('disk', NULL, 90.00, 1);
            """;

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {

            System.out.println("正在连接数据库...");

            stmt.execute(sql);
            System.out.println("✅ 告警相关表创建成功！");

        } catch (Exception e) {
            System.err.println("❌ 数据库操作失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
