package com.mars.admin.controller.system;

import cn.dev33.satoken.stp.StpUtil;
import com.mars.common.result.Result;
import com.mars.system.entity.SysUserTablePreference;
import com.mars.system.service.SysUserTablePreferenceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户表格偏好设置 Controller 单元测试")
class SysUserTablePreferenceControllerTest {

    @Mock
    private SysUserTablePreferenceService preferenceService;

    @InjectMocks
    private SysUserTablePreferenceController controller;

    private static final Long USER_ID = 1L;
    private static final String PAGE_KEY = "system/user";

    private SysUserTablePreference buildPreference() {
        SysUserTablePreference pref = new SysUserTablePreference();
        pref.setId(100L);
        pref.setUserId(USER_ID);
        pref.setPageKey(PAGE_KEY);
        pref.setColumnsConfig("[{\"key\":\"id\",\"visible\":true}]");
        pref.setPageSize(20);
        pref.setDeleted(0);
        return pref;
    }

    @Nested
    @DisplayName("getPreference - 获取偏好设置")
    class GetPreferenceTests {

        @Test
        @DisplayName("应正确返回已保存的偏好设置")
        void shouldReturnSavedPreference() {
            SysUserTablePreference pref = buildPreference();
            when(preferenceService.getPreference(USER_ID, PAGE_KEY)).thenReturn(pref);

            try (MockedStatic<StpUtil> mockedStp = mockStatic(StpUtil.class)) {
                mockedStp.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                Result<SysUserTablePreferenceController.PreferenceVO> result = controller.getPreference(PAGE_KEY);

                assertEquals(200, result.getCode());
                assertNotNull(result.getData());
                assertEquals(pref.getColumnsConfig(), result.getData().getColumnsConfig());
                assertEquals(20, result.getData().getPageSize());
            }
        }

        @Test
        @DisplayName("当偏好设置不存在时应返回空的VO")
        void shouldReturnEmptyVoWhenNotExists() {
            when(preferenceService.getPreference(USER_ID, PAGE_KEY)).thenReturn(null);

            try (MockedStatic<StpUtil> mockedStp = mockStatic(StpUtil.class)) {
                mockedStp.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                Result<SysUserTablePreferenceController.PreferenceVO> result = controller.getPreference(PAGE_KEY);

                assertEquals(200, result.getCode());
                assertNotNull(result.getData());
                assertNull(result.getData().getColumnsConfig());
                assertNull(result.getData().getPageSize());
            }
        }

        @Test
        @DisplayName("应使用当前登录用户ID查询")
        void shouldUseCurrentUserId() {
            when(preferenceService.getPreference(anyLong(), anyString())).thenReturn(null);

            try (MockedStatic<StpUtil> mockedStp = mockStatic(StpUtil.class)) {
                mockedStp.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                controller.getPreference(PAGE_KEY);

                verify(preferenceService).getPreference(USER_ID, PAGE_KEY);
            }
        }
    }

    @Nested
    @DisplayName("savePreference - 保存偏好设置")
    class SavePreferenceTests {

        @Test
        @DisplayName("应正确保存偏好设置")
        void shouldSavePreference() {
            SysUserTablePreferenceController.SavePreferenceDTO dto = new SysUserTablePreferenceController.SavePreferenceDTO();
            dto.setColumnsConfig("[{\"key\":\"id\",\"visible\":true}]");
            dto.setPageSize(15);

            try (MockedStatic<StpUtil> mockedStp = mockStatic(StpUtil.class)) {
                mockedStp.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                Result<Void> result = controller.savePreference(PAGE_KEY, dto);

                assertEquals(200, result.getCode());
                verify(preferenceService).savePreference(USER_ID, PAGE_KEY, dto.getColumnsConfig(), 15);
            }
        }

        @Test
        @DisplayName("应使用当前登录用户ID保存")
        void shouldSaveWithCurrentUserId() {
            Long anotherUserId = 2L;
            SysUserTablePreferenceController.SavePreferenceDTO dto = new SysUserTablePreferenceController.SavePreferenceDTO();
            dto.setColumnsConfig("[]");
            dto.setPageSize(10);

            try (MockedStatic<StpUtil> mockedStp = mockStatic(StpUtil.class)) {
                mockedStp.when(StpUtil::getLoginIdAsLong).thenReturn(anotherUserId);

                controller.savePreference(PAGE_KEY, dto);

                verify(preferenceService).savePreference(anotherUserId, PAGE_KEY, "[]", 10);
            }
        }
    }

    @Nested
    @DisplayName("saveColumnsConfig - 仅保存列配置")
    class SaveColumnsConfigTests {

        @Test
        @DisplayName("应正确保存列配置")
        void shouldSaveColumnsConfig() {
            SysUserTablePreferenceController.SaveColumnsDTO dto = new SysUserTablePreferenceController.SaveColumnsDTO();
            dto.setColumnsConfig("[{\"key\":\"name\",\"visible\":false}]");

            try (MockedStatic<StpUtil> mockedStp = mockStatic(StpUtil.class)) {
                mockedStp.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                Result<Void> result = controller.saveColumnsConfig(PAGE_KEY, dto);

                assertEquals(200, result.getCode());
                verify(preferenceService).saveColumnsConfig(USER_ID, PAGE_KEY, dto.getColumnsConfig());
            }
        }
    }

    @Nested
    @DisplayName("savePageSize - 仅保存分页大小")
    class SavePageSizeTests {

        @Test
        @DisplayName("应正确保存分页大小")
        void shouldSavePageSize() {
            SysUserTablePreferenceController.SavePageSizeDTO dto = new SysUserTablePreferenceController.SavePageSizeDTO();
            dto.setPageSize(50);

            try (MockedStatic<StpUtil> mockedStp = mockStatic(StpUtil.class)) {
                mockedStp.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                Result<Void> result = controller.savePageSize(PAGE_KEY, dto);

                assertEquals(200, result.getCode());
                verify(preferenceService).savePageSize(USER_ID, PAGE_KEY, 50);
            }
        }
    }

    @Nested
    @DisplayName("resetPreference - 重置偏好设置")
    class ResetPreferenceTests {

        @Test
        @DisplayName("应正确重置指定页面的偏好设置")
        void shouldResetPreference() {
            try (MockedStatic<StpUtil> mockedStp = mockStatic(StpUtil.class)) {
                mockedStp.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                Result<Void> result = controller.resetPreference(PAGE_KEY);

                assertEquals(200, result.getCode());
                verify(preferenceService).resetPreference(USER_ID, PAGE_KEY);
            }
        }
    }

    @Nested
    @DisplayName("resetAllPreferences - 重置所有偏好设置")
    class ResetAllPreferencesTests {

        @Test
        @DisplayName("应正确重置当前用户所有偏好设置")
        void shouldResetAllPreferences() {
            try (MockedStatic<StpUtil> mockedStp = mockStatic(StpUtil.class)) {
                mockedStp.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                Result<Void> result = controller.resetAllPreferences();

                assertEquals(200, result.getCode());
                verify(preferenceService).resetAllPreferences(USER_ID);
            }
        }
    }

    @Nested
    @DisplayName("DTO 内部类测试")
    class DtoTests {

        @Test
        @DisplayName("PreferenceVO 应正确设置和获取属性")
        void testPreferenceVO() {
            SysUserTablePreferenceController.PreferenceVO vo = new SysUserTablePreferenceController.PreferenceVO();
            vo.setColumnsConfig("test-config");
            vo.setPageSize(25);

            assertEquals("test-config", vo.getColumnsConfig());
            assertEquals(25, vo.getPageSize());
        }

        @Test
        @DisplayName("SavePreferenceDTO 应正确设置和获取属性")
        void testSavePreferenceDTO() {
            SysUserTablePreferenceController.SavePreferenceDTO dto = new SysUserTablePreferenceController.SavePreferenceDTO();
            dto.setColumnsConfig("config");
            dto.setPageSize(10);

            assertEquals("config", dto.getColumnsConfig());
            assertEquals(10, dto.getPageSize());
        }

        @Test
        @DisplayName("SaveColumnsDTO 应正确设置和获取属性")
        void testSaveColumnsDTO() {
            SysUserTablePreferenceController.SaveColumnsDTO dto = new SysUserTablePreferenceController.SaveColumnsDTO();
            dto.setColumnsConfig("columns");

            assertEquals("columns", dto.getColumnsConfig());
        }

        @Test
        @DisplayName("SavePageSizeDTO 应正确设置和获取属性")
        void testSavePageSizeDTO() {
            SysUserTablePreferenceController.SavePageSizeDTO dto = new SysUserTablePreferenceController.SavePageSizeDTO();
            dto.setPageSize(30);

            assertEquals(30, dto.getPageSize());
        }
    }

    @Nested
    @DisplayName("不同页面标识测试")
    class DifferentPageKeyTests {

        @Test
        @DisplayName("应支持各种页面标识格式")
        void shouldSupportVariousPageKeyFormats() {
            String[] pageKeys = {
                "system/user",
                "system/role",
                "org/dept",
                "log/operlog",
                "monitor/job",
                "tool/gen"
            };

            when(preferenceService.getPreference(anyLong(), anyString())).thenReturn(null);

            try (MockedStatic<StpUtil> mockedStp = mockStatic(StpUtil.class)) {
                mockedStp.when(StpUtil::getLoginIdAsLong).thenReturn(USER_ID);

                for (String key : pageKeys) {
                    controller.getPreference(key);
                }

                verify(preferenceService, times(pageKeys.length)).getPreference(eq(USER_ID), anyString());
            }
        }
    }
}
