package com.mars.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.mars.system.entity.SysUserTablePreference;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface SysUserTablePreferenceMapper extends BaseMapper<SysUserTablePreference> {

    @Select("SELECT * FROM sys_user_table_preference WHERE user_id = #{userId} AND page_key = #{pageKey} AND deleted = 0")
    SysUserTablePreference selectByUserAndPage(@Param("userId") Long userId, @Param("pageKey") String pageKey);

    @Update("UPDATE sys_user_table_preference SET deleted = 1, update_time = NOW() WHERE user_id = #{userId} AND page_key = #{pageKey} AND deleted = 0")
    int softDeleteByUserAndPage(@Param("userId") Long userId, @Param("pageKey") String pageKey);

    @Update("UPDATE sys_user_table_preference SET deleted = 1, update_time = NOW() WHERE user_id = #{userId} AND deleted = 0")
    int softDeleteByUser(@Param("userId") Long userId);
}
