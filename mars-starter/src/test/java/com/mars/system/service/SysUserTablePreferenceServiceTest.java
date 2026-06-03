package com.mars.system.service;

import com.mars.system.entity.SysUserTablePreference;
import com.mars.system.mapper.SysUserTablePreferenceMapper;
import com.mars.system.service.impl.SysUserTablePreferenceServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("用户表格偏好设置 Service 单元测试")
class SysUserTablePreferenceServiceTest {

    @Mock
    private SysUserTablePreferenceMapper preferenceMapper;

    @InjectMocks
    private SysUserTablePreferenceServiceImpl preferenceService;

    private static final Long USER_ID = 1L;
    private static final String PAGE_KEY = "system/user";
    private static final String COLUMNS_CONFIG = "[{\"key\":\"id\",\"visible\":true},{\"key\":\"username\",\"visible\":true},{\"key\":\"email\",\"visible\":false}]";

    private SysUserTablePreference buildExistingPreference() {
        SysUserTablePreference pref = new SysUserTablePreference();
        pref.setId(100L);
        pref.setUserId(USER_ID);
        pref.setPageKey(PAGE_KEY);
        pref.setColumnsConfig(COLUMNS_CONFIG);
        pref.setPageSize(20);
        pref.setDeleted(0);
        return pref;
    }

    @Nested
    @DisplayName("getPreference - 获取偏好设置")
    class GetPreferenceTests {

        @Test
        @DisplayName("应正确返回已存在的偏好设置")
        void shouldReturnExistingPreference() {
            SysUserTablePreference expected = buildExistingPreference();
            when(preferenceMapper.selectByUserAndPage(USER_ID, PAGE_KEY)).thenReturn(expected);

            SysUserTablePreference result = preferenceService.getPreference(USER_ID, PAGE_KEY);

            assertNotNull(result);
            assertEquals(USER_ID, result.getUserId());
            assertEquals(PAGE_KEY, result.getPageKey());
            assertEquals(COLUMNS_CONFIG, result.getColumnsConfig());
            assertEquals(20, result.getPageSize());
            verify(preferenceMapper).selectByUserAndPage(USER_ID, PAGE_KEY);
        }

        @Test
        @DisplayName("当偏好设置不存在时应返回null")
        void shouldReturnNullWhenNotExists() {
            when(preferenceMapper.selectByUserAndPage(USER_ID, PAGE_KEY)).thenReturn(null);

            SysUserTablePreference result = preferenceService.getPreference(USER_ID, PAGE_KEY);

            assertNull(result);
        }

        @Test
        @DisplayName("不同用户获取偏好设置应传不同userId")
        void shouldQueryWithDifferentUserId() {
            Long anotherUserId = 2L;
            when(preferenceMapper.selectByUserAndPage(anotherUserId, PAGE_KEY)).thenReturn(null);

            preferenceService.getPreference(anotherUserId, PAGE_KEY);

            verify(preferenceMapper).selectByUserAndPage(anotherUserId, PAGE_KEY);
        }
    }

    @Nested
    @DisplayName("savePreference - 保存偏好设置")
    class SavePreferenceTests {

        @Test
        @DisplayName("当偏好设置不存在时应新增记录")
        void shouldInsertWhenNotExists() {
            when(preferenceMapper.selectByUserAndPage(USER_ID, PAGE_KEY)).thenReturn(null);
            when(preferenceMapper.insert(any(SysUserTablePreference.class))).thenReturn(1);

            preferenceService.savePreference(USER_ID, PAGE_KEY, COLUMNS_CONFIG, 15);

            ArgumentCaptor<SysUserTablePreference> captor = ArgumentCaptor.forClass(SysUserTablePreference.class);
            verify(preferenceMapper).insert(captor.capture());
            SysUserTablePreference inserted = captor.getValue();
            assertEquals(USER_ID, inserted.getUserId());
            assertEquals(PAGE_KEY, inserted.getPageKey());
            assertEquals(COLUMNS_CONFIG, inserted.getColumnsConfig());
            assertEquals(15, inserted.getPageSize());
        }

        @Test
        @DisplayName("当偏好设置已存在时应更新记录")
        void shouldUpdateWhenExists() {
            SysUserTablePreference existing = buildExistingPreference();
            when(preferenceMapper.selectByUserAndPage(USER_ID, PAGE_KEY)).thenReturn(existing);
            when(preferenceMapper.updateById(any(SysUserTablePreference.class))).thenReturn(1);

            String newColumnsConfig = "[{\"key\":\"id\",\"visible\":false}]";
            preferenceService.savePreference(USER_ID, PAGE_KEY, newColumnsConfig, 50);

            verify(preferenceMapper).updateById(existing);
            assertEquals(newColumnsConfig, existing.getColumnsConfig());
            assertEquals(50, existing.getPageSize());
            verify(preferenceMapper, never()).insert(any());
        }

        @Test
        @DisplayName("保存null列配置时应正确处理")
        void shouldHandleNullColumnsConfig() {
            when(preferenceMapper.selectByUserAndPage(USER_ID, PAGE_KEY)).thenReturn(null);
            when(preferenceMapper.insert(any(SysUserTablePreference.class))).thenReturn(1);

            preferenceService.savePreference(USER_ID, PAGE_KEY, null, 10);

            ArgumentCaptor<SysUserTablePreference> captor = ArgumentCaptor.forClass(SysUserTablePreference.class);
            verify(preferenceMapper).insert(captor.capture());
            assertNull(captor.getValue().getColumnsConfig());
        }

        @Test
        @DisplayName("保存空字符串列配置时应正确处理")
        void shouldHandleEmptyColumnsConfig() {
            when(preferenceMapper.selectByUserAndPage(USER_ID, PAGE_KEY)).thenReturn(null);
            when(preferenceMapper.insert(any(SysUserTablePreference.class))).thenReturn(1);

            preferenceService.savePreference(USER_ID, PAGE_KEY, "", 10);

            ArgumentCaptor<SysUserTablePreference> captor = ArgumentCaptor.forClass(SysUserTablePreference.class);
            verify(preferenceMapper).insert(captor.capture());
            assertEquals("", captor.getValue().getColumnsConfig());
        }
    }

    @Nested
    @DisplayName("saveColumnsConfig - 仅保存列配置")
    class SaveColumnsConfigTests {

        @Test
        @DisplayName("当偏好设置不存在时新增记录，默认pageSize为10")
        void shouldInsertWithDefaultPageSize() {
            when(preferenceMapper.selectByUserAndPage(USER_ID, PAGE_KEY)).thenReturn(null);
            when(preferenceMapper.insert(any(SysUserTablePreference.class))).thenReturn(1);

            preferenceService.saveColumnsConfig(USER_ID, PAGE_KEY, COLUMNS_CONFIG);

            ArgumentCaptor<SysUserTablePreference> captor = ArgumentCaptor.forClass(SysUserTablePreference.class);
            verify(preferenceMapper).insert(captor.capture());
            assertEquals(COLUMNS_CONFIG, captor.getValue().getColumnsConfig());
            assertEquals(10, captor.getValue().getPageSize());
        }

        @Test
        @DisplayName("当偏好设置已存在时仅更新列配置，不修改pageSize")
        void shouldOnlyUpdateColumnsConfig() {
            SysUserTablePreference existing = buildExistingPreference();
            when(preferenceMapper.selectByUserAndPage(USER_ID, PAGE_KEY)).thenReturn(existing);

            String newConfig = "[{\"key\":\"id\",\"visible\":true}]";
            preferenceService.saveColumnsConfig(USER_ID, PAGE_KEY, newConfig);

            verify(preferenceMapper).updateById(existing);
            assertEquals(newConfig, existing.getColumnsConfig());
            assertEquals(20, existing.getPageSize());
            verify(preferenceMapper, never()).insert(any());
        }
    }

    @Nested
    @DisplayName("savePageSize - 仅保存分页大小")
    class SavePageSizeTests {

        @Test
        @DisplayName("当偏好设置不存在时新增记录")
        void shouldInsertWhenNotExists() {
            when(preferenceMapper.selectByUserAndPage(USER_ID, PAGE_KEY)).thenReturn(null);
            when(preferenceMapper.insert(any(SysUserTablePreference.class))).thenReturn(1);

            preferenceService.savePageSize(USER_ID, PAGE_KEY, 25);

            ArgumentCaptor<SysUserTablePreference> captor = ArgumentCaptor.forClass(SysUserTablePreference.class);
            verify(preferenceMapper).insert(captor.capture());
            assertEquals(25, captor.getValue().getPageSize());
            assertNull(captor.getValue().getColumnsConfig());
        }

        @Test
        @DisplayName("当偏好设置已存在时仅更新分页大小")
        void shouldOnlyUpdatePageSize() {
            SysUserTablePreference existing = buildExistingPreference();
            when(preferenceMapper.selectByUserAndPage(USER_ID, PAGE_KEY)).thenReturn(existing);

            preferenceService.savePageSize(USER_ID, PAGE_KEY, 50);

            verify(preferenceMapper).updateById(existing);
            assertEquals(50, existing.getPageSize());
            assertEquals(COLUMNS_CONFIG, existing.getColumnsConfig());
        }

        @Test
        @DisplayName("保存不同分页大小时应正确设置")
        void shouldSaveDifferentPageSizes() {
            when(preferenceMapper.selectByUserAndPage(USER_ID, PAGE_KEY)).thenReturn(null);
            when(preferenceMapper.insert(any(SysUserTablePreference.class))).thenReturn(1);

            int[] sizes = {5, 10, 20, 50, 100};
            for (int size : sizes) {
                preferenceService.savePageSize(USER_ID, PAGE_KEY, size);
            }

            verify(preferenceMapper, times(5)).insert(any(SysUserTablePreference.class));
        }
    }

    @Nested
    @DisplayName("resetPreference - 重置指定页面偏好")
    class ResetPreferenceTests {

        @Test
        @DisplayName("应调用Mapper的逻辑删除方法")
        void shouldCallSoftDelete() {
            when(preferenceMapper.softDeleteByUserAndPage(USER_ID, PAGE_KEY)).thenReturn(1);

            preferenceService.resetPreference(USER_ID, PAGE_KEY);

            verify(preferenceMapper).softDeleteByUserAndPage(USER_ID, PAGE_KEY);
        }

        @Test
        @DisplayName("重置不存在的偏好设置不应抛异常")
        void shouldNotThrowWhenNotExists() {
            when(preferenceMapper.softDeleteByUserAndPage(999L, "nonexistent/page")).thenReturn(0);

            assertDoesNotThrow(() -> preferenceService.resetPreference(999L, "nonexistent/page"));
        }
    }

    @Nested
    @DisplayName("resetAllPreferences - 重置所有页面偏好")
    class ResetAllPreferencesTests {

        @Test
        @DisplayName("应调用Mapper的批量逻辑删除方法")
        void shouldCallSoftDeleteByUser() {
            when(preferenceMapper.softDeleteByUser(USER_ID)).thenReturn(3);

            preferenceService.resetAllPreferences(USER_ID);

            verify(preferenceMapper).softDeleteByUser(USER_ID);
        }

        @Test
        @DisplayName("重置所有偏好设置应只影响指定用户")
        void shouldOnlyAffectCurrentUser() {
            when(preferenceMapper.softDeleteByUser(USER_ID)).thenReturn(2);

            preferenceService.resetAllPreferences(USER_ID);

            verify(preferenceMapper).softDeleteByUser(USER_ID);
            verify(preferenceMapper, never()).softDeleteByUser(2L);
        }
    }

    @Nested
    @DisplayName("多用户数据隔离测试")
    class UserIsolationTests {

        @Test
        @DisplayName("不同用户保存相同页面偏好应各自独立")
        void shouldIsolateDifferentUsers() {
            Long userId1 = 1L;
            Long userId2 = 2L;

            when(preferenceMapper.selectByUserAndPage(userId1, PAGE_KEY)).thenReturn(null);
            when(preferenceMapper.selectByUserAndPage(userId2, PAGE_KEY)).thenReturn(null);
            when(preferenceMapper.insert(any(SysUserTablePreference.class))).thenReturn(1);

            preferenceService.savePreference(userId1, PAGE_KEY, "[{\"key\":\"id\",\"visible\":true}]", 10);
            preferenceService.savePreference(userId2, PAGE_KEY, "[{\"key\":\"id\",\"visible\":false}]", 20);

            verify(preferenceMapper, times(2)).insert(any(SysUserTablePreference.class));
        }

        @Test
        @DisplayName("同一用户不同页面偏好应各自独立")
        void shouldIsolateDifferentPages() {
            String pageKey1 = "system/user";
            String pageKey2 = "system/role";

            when(preferenceMapper.selectByUserAndPage(USER_ID, pageKey1)).thenReturn(null);
            when(preferenceMapper.selectByUserAndPage(USER_ID, pageKey2)).thenReturn(null);
            when(preferenceMapper.insert(any(SysUserTablePreference.class))).thenReturn(1);

            preferenceService.savePreference(USER_ID, pageKey1, COLUMNS_CONFIG, 10);
            preferenceService.savePreference(USER_ID, pageKey2, "[{\"key\":\"name\",\"visible\":true}]", 20);

            verify(preferenceMapper, times(2)).insert(any(SysUserTablePreference.class));
        }
    }

    @Nested
    @DisplayName("列配置JSON边界测试")
    class ColumnsConfigEdgeCaseTests {

        @Test
        @DisplayName("保存复杂嵌套的列配置JSON应正确处理")
        void shouldHandleComplexJson() {
            String complexJson = "[{\"key\":\"id\",\"visible\":true,\"width\":100},{\"key\":\"actions\",\"visible\":true,\"fixed\":\"right\",\"render\":{}}]";
            when(preferenceMapper.selectByUserAndPage(USER_ID, PAGE_KEY)).thenReturn(null);
            when(preferenceMapper.insert(any(SysUserTablePreference.class))).thenReturn(1);

            preferenceService.saveColumnsConfig(USER_ID, PAGE_KEY, complexJson);

            ArgumentCaptor<SysUserTablePreference> captor = ArgumentCaptor.forClass(SysUserTablePreference.class);
            verify(preferenceMapper).insert(captor.capture());
            assertEquals(complexJson, captor.getValue().getColumnsConfig());
        }

        @Test
        @DisplayName("保存空数组列配置应正确处理")
        void shouldHandleEmptyArrayJson() {
            when(preferenceMapper.selectByUserAndPage(USER_ID, PAGE_KEY)).thenReturn(null);
            when(preferenceMapper.insert(any(SysUserTablePreference.class))).thenReturn(1);

            preferenceService.saveColumnsConfig(USER_ID, PAGE_KEY, "[]");

            ArgumentCaptor<SysUserTablePreference> captor = ArgumentCaptor.forClass(SysUserTablePreference.class);
            verify(preferenceMapper).insert(captor.capture());
            assertEquals("[]", captor.getValue().getColumnsConfig());
        }

        @Test
        @DisplayName("保存包含中文的列配置应正确处理")
        void shouldHandleChineseCharacters() {
            String chineseConfig = "[{\"key\":\"name\",\"visible\":true,\"title\":\"用户名\"}]";
            when(preferenceMapper.selectByUserAndPage(USER_ID, PAGE_KEY)).thenReturn(null);
            when(preferenceMapper.insert(any(SysUserTablePreference.class))).thenReturn(1);

            preferenceService.saveColumnsConfig(USER_ID, PAGE_KEY, chineseConfig);

            ArgumentCaptor<SysUserTablePreference> captor = ArgumentCaptor.forClass(SysUserTablePreference.class);
            verify(preferenceMapper).insert(captor.capture());
            assertEquals(chineseConfig, captor.getValue().getColumnsConfig());
        }
    }

    @Nested
    @DisplayName("Upsert逻辑测试")
    class UpsertLogicTests {

        @Test
        @DisplayName("首次保存后再次保存应走更新逻辑")
        void shouldInsertFirstThenUpdate() {
            when(preferenceMapper.selectByUserAndPage(USER_ID, PAGE_KEY))
                    .thenReturn(null)
                    .thenReturn(buildExistingPreference());
            when(preferenceMapper.insert(any(SysUserTablePreference.class))).thenReturn(1);
            when(preferenceMapper.updateById(any(SysUserTablePreference.class))).thenReturn(1);

            preferenceService.savePreference(USER_ID, PAGE_KEY, COLUMNS_CONFIG, 10);
            verify(preferenceMapper).insert(any());
            verify(preferenceMapper, never()).updateById(any());

            preferenceService.savePreference(USER_ID, PAGE_KEY, "new config", 20);
            verify(preferenceMapper).updateById(any());
            verify(preferenceMapper, times(1)).insert(any());
        }

        @Test
        @DisplayName("分别保存列配置和分页大小应互不影响")
        void shouldSaveColumnsAndPageSizeIndependently() {
            SysUserTablePreference existing = buildExistingPreference();
            when(preferenceMapper.selectByUserAndPage(USER_ID, PAGE_KEY)).thenReturn(existing);

            preferenceService.saveColumnsConfig(USER_ID, PAGE_KEY, "new columns");
            assertEquals("new columns", existing.getColumnsConfig());
            assertEquals(20, existing.getPageSize());

            preferenceService.savePageSize(USER_ID, PAGE_KEY, 30);
            assertEquals("new columns", existing.getColumnsConfig());
            assertEquals(30, existing.getPageSize());
        }
    }
}
