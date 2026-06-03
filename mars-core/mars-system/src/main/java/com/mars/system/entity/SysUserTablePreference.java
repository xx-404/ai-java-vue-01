package com.mars.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mars.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户表格偏好设置
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_table_preference")
public class SysUserTablePreference extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 页面标识(如: system/user)
     */
    private String pageKey;

    /**
     * 列配置JSON
     */
    private String columnsConfig;

    /**
     * 每页条数
     */
    private Integer pageSize;
}
