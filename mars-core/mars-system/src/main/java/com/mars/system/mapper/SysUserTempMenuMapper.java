package com.mars.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mars.system.entity.SysUserTempMenu;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 临时外协用户菜单权限Mapper
 */
@Mapper
public interface SysUserTempMenuMapper extends BaseMapper<SysUserTempMenu> {

    /**
     * 根据用户ID获取菜单ID列表
     */
    @Select("SELECT menu_id FROM sys_user_temp_menu WHERE user_id = #{userId} AND deleted = 0")
    List<Long> selectMenuIdsByUserId(@Param("userId") Long userId);

    /**
     * 根据用户ID删除所有菜单关联
     */
    void deleteByUserId(@Param("userId") Long userId);

    /**
     * 根据菜单ID删除所有关联
     */
    void deleteByMenuId(@Param("menuId") Long menuId);
}
