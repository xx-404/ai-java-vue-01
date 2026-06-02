package com.mars.admin.controller.system;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.mars.common.result.PageResult;
import com.mars.common.result.Result;
import com.mars.system.annotation.Log;
import com.mars.system.annotation.Log.BusinessType;
import com.mars.system.entity.SysUser;
import com.mars.system.service.SysUserService;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 临时外协用户控制器
 */
@RestController
@RequestMapping("/sys/temp-user")
@RequiredArgsConstructor
public class TempUserController {

    private final SysUserService userService;

    /**
     * 分页查询临时外协用户
     */
    @GetMapping("/page")
    @SaCheckPermission("sys:tempUser:list")
    public Result<PageResult<SysUser>> page(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) Integer status) {
        return Result.ok(userService.pageTempUsers(page, pageSize, username, status));
    }

    /**
     * 获取临时用户详情
     */
    @GetMapping("/{id}")
    @SaCheckPermission("sys:tempUser:list")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        SysUser user = userService.getTempUserDetail(id);
        List<Long> menuIds = userService.getTempUserMenuIds(id);
        
        Map<String, Object> result = Map.of(
                "user", user,
                "menuIds", menuIds
        );
        return Result.ok(result);
    }

    /**
     * 创建临时外协账号
     */
    @PostMapping
    @SaCheckPermission("sys:tempUser:add")
    @Log(title = "临时外协用户", businessType = BusinessType.INSERT)
    public Result<Void> create(@RequestBody TempUserRequest request) {
        userService.createTempUser(request.getUser(), request.getMenuIds());
        return Result.ok();
    }

    /**
     * 更新临时外协账号
     */
    @PutMapping
    @SaCheckPermission("sys:tempUser:edit")
    @Log(title = "临时外协用户", businessType = BusinessType.UPDATE)
    public Result<Void> update(@RequestBody TempUserRequest request) {
        userService.updateTempUser(request.getUser(), request.getMenuIds());
        return Result.ok();
    }

    /**
     * 删除临时外协账号
     */
    @DeleteMapping("/{id}")
    @SaCheckPermission("sys:tempUser:delete")
    @Log(title = "临时外协用户", businessType = BusinessType.DELETE)
    public Result<Void> delete(@PathVariable Long id) {
        userService.deleteTempUser(id);
        return Result.ok();
    }

    /**
     * 手动检查并禁用过期账号
     */
    @PostMapping("/check-expired")
    @SaCheckPermission("sys:tempUser:edit")
    @Log(title = "临时外协用户", businessType = BusinessType.UPDATE)
    public Result<Void> checkExpired() {
        userService.disableExpiredTempUsers();
        return Result.ok();
    }

    @Data
    public static class TempUserRequest {
        private SysUser user;
        private List<Long> menuIds;
    }
}
