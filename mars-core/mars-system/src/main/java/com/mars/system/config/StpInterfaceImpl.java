package com.mars.system.config;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpInterface;
import cn.dev33.satoken.stp.StpUtil;
import com.mars.system.entity.SysUser;
import com.mars.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Sa-Token权限认证实现
 * 使用 SaSession 缓存权限和角色数据，避免每次请求都查询数据库
 * 缓存生命周期与用户会话一致，会话过期时自动清除
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class StpInterfaceImpl implements StpInterface {

    private final SysUserService userService;

    private static final String CACHE_KEY_PERMISSIONS = "user_permissions";
    private static final String CACHE_KEY_ROLES = "user_roles";
    private static final String CACHE_KEY_IS_TEMP = "user_is_temp";

    /**
     * 获取权限列表（优先从 Session 缓存读取）
     * 临时外协用户只返回其指定的菜单权限
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        return session.get(CACHE_KEY_PERMISSIONS, () -> {
            log.debug("从数据库加载用户权限: userId={}", loginId);
            Long userId = Long.parseLong(loginId.toString());
            
            // 检查是否为临时用户
            Boolean isTemp = session.get(CACHE_KEY_IS_TEMP, () -> {
                SysUser user = userService.getById(userId);
                return user != null && user.getIsTemp() == 1;
            });
            
            if (Boolean.TRUE.equals(isTemp)) {
                // 临时用户返回其指定的菜单权限
                return userService.getTempUserPermissions(userId);
            } else {
                // 普通用户返回角色权限
                return userService.getPermissions(userId);
            }
        });
    }

    /**
     * 获取角色列表（优先从 Session 缓存读取）
     * 临时外协用户不分配角色
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        SaSession session = StpUtil.getSessionByLoginId(loginId);
        return session.get(CACHE_KEY_ROLES, () -> {
            log.debug("从数据库加载用户角色: userId={}", loginId);
            Long userId = Long.parseLong(loginId.toString());
            
            // 检查是否为临时用户
            Boolean isTemp = session.get(CACHE_KEY_IS_TEMP, () -> {
                SysUser user = userService.getById(userId);
                return user != null && user.getIsTemp() == 1;
            });
            
            if (Boolean.TRUE.equals(isTemp)) {
                // 临时用户不分配角色
                return new ArrayList<>();
            } else {
                return userService.getRoleCodes(userId);
            }
        });
    }

    /**
     * 清除指定用户的权限和角色缓存
     * 在用户角色变更、角色权限变更时调用
     */
    public static void clearPermissionCache(Long userId) {
        try {
            SaSession session = StpUtil.getSessionByLoginId(userId, false);
            if (session != null) {
                session.delete(CACHE_KEY_PERMISSIONS);
                session.delete(CACHE_KEY_ROLES);
                session.delete(CACHE_KEY_IS_TEMP);
                log.debug("已清除用户权限缓存: userId={}", userId);
            }
        } catch (Exception e) {
            // 用户未登录时获取不到session，忽略即可
        }
    }

    /**
     * 清除所有在线用户的权限缓存
     * 在系统权限变更时调用
     */
    public static void clearAllPermissionCache() {
        try {
            List<String> sessionIds = StpUtil.searchSessionId("", 0, -1, false);
            for (String sessionId : sessionIds) {
                try {
                    SaSession session = StpUtil.getSessionBySessionId(sessionId);
                    if (session != null) {
                        session.delete(CACHE_KEY_PERMISSIONS);
                        session.delete(CACHE_KEY_ROLES);
                        session.delete(CACHE_KEY_IS_TEMP);
                    }
                } catch (Exception e) {
                    // 忽略单个session的清除错误
                }
            }
            log.debug("已清除所有在线用户的权限缓存，session数量: {}", sessionIds.size());
        } catch (Exception e) {
            log.error("清除所有权限缓存失败", e);
        }
    }
}
