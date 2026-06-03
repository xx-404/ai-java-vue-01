package com.mars.system.service.impl;

import com.mars.system.entity.SysUserTablePreference;
import com.mars.system.mapper.SysUserTablePreferenceMapper;
import com.mars.system.service.SysUserTablePreferenceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysUserTablePreferenceServiceImpl implements SysUserTablePreferenceService {

    private final SysUserTablePreferenceMapper preferenceMapper;

    @Override
    public SysUserTablePreference getPreference(Long userId, String pageKey) {
        return preferenceMapper.selectByUserAndPage(userId, pageKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePreference(Long userId, String pageKey, String columnsConfig, Integer pageSize) {
        SysUserTablePreference existing = preferenceMapper.selectByUserAndPage(userId, pageKey);
        if (existing != null) {
            existing.setColumnsConfig(columnsConfig);
            existing.setPageSize(pageSize);
            preferenceMapper.updateById(existing);
        } else {
            SysUserTablePreference preference = new SysUserTablePreference();
            preference.setUserId(userId);
            preference.setPageKey(pageKey);
            preference.setColumnsConfig(columnsConfig);
            preference.setPageSize(pageSize);
            preferenceMapper.insert(preference);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveColumnsConfig(Long userId, String pageKey, String columnsConfig) {
        SysUserTablePreference existing = preferenceMapper.selectByUserAndPage(userId, pageKey);
        if (existing != null) {
            existing.setColumnsConfig(columnsConfig);
            preferenceMapper.updateById(existing);
        } else {
            SysUserTablePreference preference = new SysUserTablePreference();
            preference.setUserId(userId);
            preference.setPageKey(pageKey);
            preference.setColumnsConfig(columnsConfig);
            preference.setPageSize(10);
            preferenceMapper.insert(preference);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void savePageSize(Long userId, String pageKey, Integer pageSize) {
        SysUserTablePreference existing = preferenceMapper.selectByUserAndPage(userId, pageKey);
        if (existing != null) {
            existing.setPageSize(pageSize);
            preferenceMapper.updateById(existing);
        } else {
            SysUserTablePreference preference = new SysUserTablePreference();
            preference.setUserId(userId);
            preference.setPageKey(pageKey);
            preference.setPageSize(pageSize);
            preferenceMapper.insert(preference);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPreference(Long userId, String pageKey) {
        preferenceMapper.softDeleteByUserAndPage(userId, pageKey);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetAllPreferences(Long userId) {
        preferenceMapper.softDeleteByUser(userId);
    }
}
