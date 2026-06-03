package com.mars.system.service;

import com.mars.system.entity.SysUserTablePreference;

/**
 * 用户表格偏好设置 Service
 */
public interface SysUserTablePreferenceService {

    /**
     * 获取用户指定页面的偏好设置
     */
    SysUserTablePreference getPreference(Long userId, String pageKey);

    /**
     * 保存用户指定页面的偏好设置
     */
    void savePreference(Long userId, String pageKey, String columnsConfig, Integer pageSize);

    /**
     * 保存列配置
     */
    void saveColumnsConfig(Long userId, String pageKey, String columnsConfig);

    /**
     * 保存分页大小
     */
    void savePageSize(Long userId, String pageKey, Integer pageSize);

    /**
     * 重置用户指定页面的偏好设置
     */
    void resetPreference(Long userId, String pageKey);

    /**
     * 重置用户所有页面的偏好设置
     */
    void resetAllPreferences(Long userId);
}
