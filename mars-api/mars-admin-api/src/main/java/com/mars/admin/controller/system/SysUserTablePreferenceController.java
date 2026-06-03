package com.mars.admin.controller.system;

import cn.dev33.satoken.stp.StpUtil;
import com.mars.common.result.Result;
import com.mars.system.entity.SysUserTablePreference;
import com.mars.system.service.SysUserTablePreferenceService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户表格偏好设置控制器
 */
@RestController
@RequestMapping("/sys/table-preference")
@RequiredArgsConstructor
public class SysUserTablePreferenceController {

    private final SysUserTablePreferenceService preferenceService;

    /**
     * 获取当前用户指定页面的偏好设置
     */
    @GetMapping("/{pageKey}")
    public Result<PreferenceVO> getPreference(@PathVariable String pageKey) {
        Long userId = StpUtil.getLoginIdAsLong();
        SysUserTablePreference preference = preferenceService.getPreference(userId, pageKey);
        PreferenceVO vo = new PreferenceVO();
        if (preference != null) {
            vo.setColumnsConfig(preference.getColumnsConfig());
            vo.setPageSize(preference.getPageSize());
        }
        return Result.ok(vo);
    }

    /**
     * 保存当前用户指定页面的偏好设置
     */
    @PostMapping("/{pageKey}")
    public Result<Void> savePreference(@PathVariable String pageKey, @RequestBody SavePreferenceDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        preferenceService.savePreference(userId, pageKey, dto.getColumnsConfig(), dto.getPageSize());
        return Result.ok();
    }

    /**
     * 保存列配置
     */
    @PostMapping("/{pageKey}/columns")
    public Result<Void> saveColumnsConfig(@PathVariable String pageKey, @RequestBody SaveColumnsDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        preferenceService.saveColumnsConfig(userId, pageKey, dto.getColumnsConfig());
        return Result.ok();
    }

    /**
     * 保存分页大小
     */
    @PostMapping("/{pageKey}/page-size")
    public Result<Void> savePageSize(@PathVariable String pageKey, @RequestBody SavePageSizeDTO dto) {
        Long userId = StpUtil.getLoginIdAsLong();
        preferenceService.savePageSize(userId, pageKey, dto.getPageSize());
        return Result.ok();
    }

    /**
     * 重置当前用户指定页面的偏好设置
     */
    @DeleteMapping("/{pageKey}")
    public Result<Void> resetPreference(@PathVariable String pageKey) {
        Long userId = StpUtil.getLoginIdAsLong();
        preferenceService.resetPreference(userId, pageKey);
        return Result.ok();
    }

    /**
     * 重置当前用户所有页面的偏好设置
     */
    @DeleteMapping("/all")
    public Result<Void> resetAllPreferences() {
        Long userId = StpUtil.getLoginIdAsLong();
        preferenceService.resetAllPreferences(userId);
        return Result.ok();
    }

    @Data
    public static class PreferenceVO {
        private String columnsConfig;
        private Integer pageSize;
    }

    @Data
    public static class SavePreferenceDTO {
        private String columnsConfig;
        private Integer pageSize;
    }

    @Data
    public static class SaveColumnsDTO {
        private String columnsConfig;
    }

    @Data
    public static class SavePageSizeDTO {
        private Integer pageSize;
    }
}
