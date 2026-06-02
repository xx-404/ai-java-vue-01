package com.mars.system.task;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import com.mars.system.entity.SysUser;
import com.mars.system.service.SysUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 临时外协账号定时任务
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TempUserExpireTask {

    private final SysUserService userService;

    /**
     * 每分钟检查一次过期账号
     * 禁用过期的临时外协账号并踢掉在线会话
     */
    @Scheduled(cron = "0 * * * * ?")
    public void checkExpiredTempUsers() {
        try {
            log.debug("开始检查过期的临时外协账号...");
            userService.disableExpiredTempUsers();
            log.debug("临时外协账号过期检查完成");
        } catch (Exception e) {
            log.error("检查过期临时外协账号失败", e);
        }
    }

    /**
     * 每5分钟检查一次在线的过期账号并强制踢下线
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void kickoutExpiredOnlineUsers() {
        try {
            List<String> sessionIds = StpUtil.searchSessionId("", 0, -1, false);
            for (String sessionId : sessionIds) {
                try {
                    SaSession session = StpUtil.getSessionBySessionId(sessionId);
                    if (session != null) {
                        Object loginId = session.getId();
                        if (loginId != null) {
                            Long userId = Long.parseLong(loginId.toString());
                            if (userService.isTempUserExpired(userId)) {
                                String tokenValue = getTokenValue(session, loginId);
                                if (StringUtils.hasText(tokenValue)) {
                                    StpUtil.kickoutByTokenValue(tokenValue);
                                }
                                log.info("已强制踢掉过期临时账号的在线会话: userId={}", userId);
                            }
                        }
                    }
                } catch (Exception e) {
                    // 忽略无效的session
                }
            }
        } catch (Exception e) {
            log.error("踢掉过期临时账号会话失败", e);
        }
    }

    private String getTokenValue(SaSession session, Object loginId) {
        String token = session.getToken();
        if (StringUtils.hasText(token)) {
            return token;
        }
        try {
            return StpUtil.getTokenValueByLoginId(loginId);
        } catch (Exception e) {
            return "";
        }
    }
}
