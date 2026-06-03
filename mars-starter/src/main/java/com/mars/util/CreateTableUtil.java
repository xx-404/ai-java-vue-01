package com.mars.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class CreateTableUtil {

    public static void main(String[] args) {
        String url = "jdbc:mysql://localhost:3306/test01?useUnicode=true&characterEncoding=utf8&zeroDateTimeBehavior=convertToNull&useSSL=true&serverTimezone=GMT%2B8&allowMultiQueries=true";
        String username = "root";
        String password = "root123";

        String sql = """
            CREATE TABLE IF NOT EXISTS `sys_user_table_preference` (
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

        try (Connection conn = DriverManager.getConnection(url, username, password);
             Statement stmt = conn.createStatement()) {

            System.out.println("正在连接数据库...");
            System.out.println("URL: " + url);
            System.out.println("用户: " + username);

            // 检查表是否已存在
            String checkSql = "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'sys_user_table_preference'";
            var rs = stmt.executeQuery(checkSql);
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("表 sys_user_table_preference 已存在，跳过创建");
                System.out.println("\n✅ 数据库初始化完成！");
                return;
            }

            // 执行建表
            stmt.execute(sql);
            System.out.println("✅ 表 sys_user_table_preference 创建成功！");
            System.out.println("\n✅ 数据库初始化完成！");

        } catch (Exception e) {
            System.err.println("❌ 数据库操作失败: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
