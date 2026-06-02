package com.mars.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.mars.MarsAdminApplication;
import com.mars.system.entity.SysRole;
import com.mars.system.entity.SysRoleDept;
import com.mars.system.entity.SysRoleMenu;
import com.mars.system.mapper.SysRoleDeptMapper;
import com.mars.system.mapper.SysRoleMenuMapper;
import com.mars.system.mapper.SysRoleMapper;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
@SpringBootTest(classes = MarsAdminApplication.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SysRoleServiceCopyTest {

    @Autowired
    private SysRoleService roleService;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysRoleMenuMapper roleMenuMapper;

    @Autowired
    private SysRoleDeptMapper roleDeptMapper;

    private static Long sourceRoleId;
    private static Long copiedRoleId;
    private static String sourceRoleCode = "test_source_role_" + System.currentTimeMillis();
    private static String copiedRoleCode = "test_copied_role_" + System.currentTimeMillis();

    @BeforeEach
    void setUp() {
        log.info("========== 测试开始 ==========");
    }

    @AfterEach
    void tearDown() {
        log.info("========== 测试结束 ==========");
    }

    @Test
    @Order(1)
    @DisplayName("1. 准备测试数据 - 创建源角色")
    void test1_CreateSourceRole() {
        log.info("步骤1: 创建源测试角色");
        
        SysRole role = new SysRole();
        role.setName("测试源角色");
        role.setCode(sourceRoleCode);
        role.setSort(100);
        role.setStatus(1);
        role.setDataScope(2);
        role.setRemark("单元测试源角色");
        
        roleService.save(role);
        sourceRoleId = role.getId();
        
        assertNotNull(sourceRoleId, "源角色ID不应为空");
        log.info("源角色创建成功，ID: {}, Code: {}", sourceRoleId, sourceRoleCode);

        List<Long> menuIds = List.of(1L, 2L, 3L, 6L, 7L, 8L);
        for (Long menuId : menuIds) {
            SysRoleMenu roleMenu = new SysRoleMenu();
            roleMenu.setRoleId(sourceRoleId);
            roleMenu.setMenuId(menuId);
            roleMenuMapper.insert(roleMenu);
        }
        log.info("源角色菜单权限设置成功，菜单数量: {}", menuIds.size());

        List<Long> deptIds = List.of(1L, 2L);
        for (Long deptId : deptIds) {
            SysRoleDept roleDept = new SysRoleDept();
            roleDept.setRoleId(sourceRoleId);
            roleDept.setDeptId(deptId);
            roleDeptMapper.insert(roleDept);
        }
        log.info("源角色部门权限设置成功，部门数量: {}", deptIds.size());
    }

    @Test
    @Order(2)
    @DisplayName("2. 获取源角色详情用于复制")
    void test2_GetCopyDetail() {
        log.info("步骤2: 获取源角色详情用于复制");
        
        SysRole role = roleService.getCopyDetail(sourceRoleId);
        
        assertNotNull(role, "源角色详情不应为空");
        assertEquals(sourceRoleCode, role.getCode(), "角色编码应匹配");
        assertEquals("测试源角色", role.getName(), "角色名称应匹配");
        assertEquals(2, role.getDataScope(), "数据范围应匹配");
        
        log.info("源角色详情获取成功: name={}, code={}, dataScope={}", 
                role.getName(), role.getCode(), role.getDataScope());
    }

    @Test
    @Order(3)
    @DisplayName("3. 执行角色复制")
    @Transactional
    void test3_CopyRole() {
        log.info("步骤3: 执行角色复制");
        
        SysRole sourceRole = roleService.getCopyDetail(sourceRoleId);
        assertNotNull(sourceRole, "源角色不应为空");

        List<Long> sourceMenuIds = roleService.getMenuIds(sourceRoleId);
        log.info("源角色菜单ID列表: {}", sourceMenuIds);
        assertFalse(sourceMenuIds.isEmpty(), "源角色菜单不应为空");

        List<Long> sourceDeptIds = roleDeptMapper.selectList(
                new LambdaQueryWrapper<SysRoleDept>()
                        .eq(SysRoleDept::getRoleId, sourceRoleId)
        ).stream().map(SysRoleDept::getDeptId).collect(Collectors.toList());
        log.info("源角色部门ID列表: {}", sourceDeptIds);

        SysRole newRole = new SysRole();
        newRole.setName("复制的测试角色");
        newRole.setCode(copiedRoleCode);
        newRole.setSort(sourceRole.getSort());
        newRole.setStatus(sourceRole.getStatus());
        newRole.setDataScope(sourceRole.getDataScope());
        newRole.setRemark("复制自: " + sourceRole.getName());

        roleService.copy(newRole, sourceMenuIds, sourceDeptIds);
        
        copiedRoleId = newRole.getId();
        assertNotNull(copiedRoleId, "复制后的角色ID不应为空");
        log.info("角色复制成功，新角色ID: {}", copiedRoleId);
    }

    @Test
    @Order(4)
    @DisplayName("4. 验证复制后的角色基本信息")
    void test4_VerifyCopiedRoleBasicInfo() {
        log.info("步骤4: 验证复制后的角色基本信息");
        
        SysRole copiedRole = roleService.getById(copiedRoleId);
        assertNotNull(copiedRole, "复制后的角色不应为空");
        
        assertEquals("复制的测试角色", copiedRole.getName(), "角色名称应匹配");
        assertEquals(copiedRoleCode, copiedRole.getCode(), "角色编码应匹配");
        assertEquals(1, copiedRole.getStatus(), "状态应匹配");
        assertEquals(2, copiedRole.getDataScope(), "数据范围应匹配");
        assertTrue(copiedRole.getRemark().contains("复制自"), "备注应包含复制标识");
        
        assertNotEquals(sourceRoleId, copiedRoleId, "复制后的角色ID应不同于源角色");
        
        log.info("复制角色基本信息验证通过: name={}, code={}, status={}, dataScope={}",
                copiedRole.getName(), copiedRole.getCode(), 
                copiedRole.getStatus(), copiedRole.getDataScope());
    }

    @Test
    @Order(5)
    @DisplayName("5. 验证复制后的菜单权限")
    void test5_VerifyCopiedRoleMenuPermissions() {
        log.info("步骤5: 验证复制后的菜单权限");
        
        List<Long> sourceMenuIds = roleService.getMenuIds(sourceRoleId);
        List<Long> copiedMenuIds = roleService.getMenuIds(copiedRoleId);
        
        assertNotNull(sourceMenuIds, "源角色菜单ID列表不应为空");
        assertNotNull(copiedMenuIds, "复制角色菜单ID列表不应为空");
        assertEquals(sourceMenuIds.size(), copiedMenuIds.size(), "菜单数量应相同");
        
        sourceMenuIds.sort(Long::compareTo);
        copiedMenuIds.sort(Long::compareTo);
        assertArrayEquals(sourceMenuIds.toArray(), copiedMenuIds.toArray(), "菜单ID列表应完全相同");
        
        log.info("菜单权限验证通过: 源角色菜单数={}, 复制角色菜单数={}", 
                sourceMenuIds.size(), copiedMenuIds.size());
        log.info("菜单ID列表: {}", copiedMenuIds);
    }

    @Test
    @Order(6)
    @DisplayName("6. 验证复制后的部门权限")
    void test6_VerifyCopiedRoleDeptPermissions() {
        log.info("步骤6: 验证复制后的部门权限");
        
        List<Long> sourceDeptIds = roleDeptMapper.selectList(
                new LambdaQueryWrapper<SysRoleDept>()
                        .eq(SysRoleDept::getRoleId, sourceRoleId)
        ).stream().map(SysRoleDept::getDeptId).collect(Collectors.toList());
        
        List<Long> copiedDeptIds = roleDeptMapper.selectList(
                new LambdaQueryWrapper<SysRoleDept>()
                        .eq(SysRoleDept::getRoleId, copiedRoleId)
        ).stream().map(SysRoleDept::getDeptId).collect(Collectors.toList());
        
        assertEquals(sourceDeptIds.size(), copiedDeptIds.size(), "部门数量应相同");
        
        sourceDeptIds.sort(Long::compareTo);
        copiedDeptIds.sort(Long::compareTo);
        assertArrayEquals(sourceDeptIds.toArray(), copiedDeptIds.toArray(), "部门ID列表应完全相同");
        
        log.info("部门权限验证通过: 源角色部门数={}, 复制角色部门数={}",
                sourceDeptIds.size(), copiedDeptIds.size());
        log.info("部门ID列表: {}", copiedDeptIds);
    }

    @Test
    @Order(7)
    @DisplayName("7. 验证复制角色编码唯一性检查")
    void test7_VerifyRoleCodeUniqueCheck() {
        log.info("步骤7: 验证复制角色编码唯一性检查");
        
        SysRole sourceRole = roleService.getCopyDetail(sourceRoleId);
        List<Long> menuIds = roleService.getMenuIds(sourceRoleId);
        List<Long> deptIds = roleDeptMapper.selectList(
                new LambdaQueryWrapper<SysRoleDept>()
                        .eq(SysRoleDept::getRoleId, sourceRoleId)
        ).stream().map(SysRoleDept::getDeptId).collect(Collectors.toList());

        SysRole duplicateRole = new SysRole();
        duplicateRole.setName("重复编码测试角色");
        duplicateRole.setCode(sourceRoleCode);
        duplicateRole.setSort(99);
        duplicateRole.setStatus(1);
        duplicateRole.setDataScope(1);

        assertThrows(Exception.class, () -> {
            roleService.copy(duplicateRole, menuIds, deptIds);
        }, "使用已存在的角色编码应该抛出异常");
        
        log.info("角色编码唯一性检查验证通过");
    }

    @Test
    @Order(8)
    @DisplayName("8. 清理测试数据")
    void test8_CleanupTestData() {
        log.info("步骤8: 清理测试数据");
        
        if (copiedRoleId != null) {
            roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                    .eq(SysRoleMenu::getRoleId, copiedRoleId));
            roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>()
                    .eq(SysRoleDept::getRoleId, copiedRoleId));
            roleService.removeById(copiedRoleId);
            log.info("已清理复制的测试角色: {}", copiedRoleId);
        }

        if (sourceRoleId != null) {
            roleMenuMapper.delete(new LambdaQueryWrapper<SysRoleMenu>()
                    .eq(SysRoleMenu::getRoleId, sourceRoleId));
            roleDeptMapper.delete(new LambdaQueryWrapper<SysRoleDept>()
                    .eq(SysRoleDept::getRoleId, sourceRoleId));
            roleService.removeById(sourceRoleId);
            log.info("已清理源测试角色: {}", sourceRoleId);
        }
        
        log.info("测试数据清理完成");
    }
}
