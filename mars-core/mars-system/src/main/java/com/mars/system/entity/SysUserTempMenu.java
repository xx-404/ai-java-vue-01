package com.mars.system.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.mars.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 临时外协用户菜单权限表
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_temp_menu")
public class SysUserTempMenu extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 菜单ID
     */
    private Long menuId;
}
