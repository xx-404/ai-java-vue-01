package com.mars.system.config;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mars.system.entity.SysMenu;
import com.mars.system.mapper.SysMenuMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SystemInitConfig implements CommandLineRunner {

    private final SysMenuMapper menuMapper;

    @Override
    public void run(String... args) {
        initCopyRolePermission();
    }

    private void initCopyRolePermission() {
        log.info("检查复制角色权限是否存在...");
        try {
            LambdaQueryWrapper<SysMenu> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysMenu::getPermission, "sys:role:copy");
            Long count = menuMapper.selectCount(wrapper);
            if (count == null || count == 0) {
                log.info("复制角色权限不存在，开始添加...");
                SysMenu menu = new SysMenu();
                menu.setParentId(6L);
                menu.setName("复制角色");
                menu.setType(3);
                menu.setPermission("sys:role:copy");
                menu.setSort(4);
                menu.setStatus(1);
                menu.setVisible(1);
                menu.setDeleted(0);
                menu.setCreateTime(LocalDateTime.now());
                menu.setUpdateTime(LocalDateTime.now());
                menuMapper.insert(menu);
                log.info("复制角色权限添加成功，权限标识: sys:role:copy");
                
                log.info("清除权限缓存...");
                StpInterfaceImpl.clearAllPermissionCache();
                log.info("权限缓存已清除");
            } else {
                log.info("复制角色权限已存在");
            }
        } catch (Exception e) {
            log.error("初始化复制角色权限失败", e);
        }
    }
}
