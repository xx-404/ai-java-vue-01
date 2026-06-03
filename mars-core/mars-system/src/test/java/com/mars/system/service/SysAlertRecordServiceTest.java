package com.mars.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mars.system.alert.AlertMetricStrategy;
import com.mars.system.alert.AlertMetricStrategyRegistry;
import com.mars.system.entity.SysAlertConfig;
import com.mars.system.entity.SysAlertRecord;
import com.mars.system.mapper.SysAlertRecordMapper;
import com.mars.system.service.impl.SysAlertConfigServiceImpl;
import com.mars.system.service.impl.SysAlertRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("告警检测服务单元测试")
class SysAlertRecordServiceTest {

    @Mock
    private SysAlertRecordMapper alertRecordMapper;

    @Mock
    private SysAlertConfigServiceImpl alertConfigService;

    @Mock
    private AlertMetricStrategyRegistry strategyRegistry;

    @Mock
    private AlertMetricStrategy cpuStrategy;

    @Mock
    private AlertMetricStrategy memoryStrategy;

    @Mock
    private AlertMetricStrategy diskStrategy;

    @Spy
    @InjectMocks
    private SysAlertRecordServiceImpl alertRecordService;

    private SysAlertConfig cpuConfig;
    private SysAlertConfig memoryConfig;
    private SysAlertConfig diskConfig;

    @BeforeEach
    void setUp() {
        cpuConfig = new SysAlertConfig();
        cpuConfig.setId(1L);
        cpuConfig.setAlertType("cpu");
        cpuConfig.setThreshold(new BigDecimal("80.00"));
        cpuConfig.setEnabled(1);

        memoryConfig = new SysAlertConfig();
        memoryConfig.setId(2L);
        memoryConfig.setAlertType("memory");
        memoryConfig.setThreshold(new BigDecimal("85.00"));
        memoryConfig.setEnabled(1);

        diskConfig = new SysAlertConfig();
        diskConfig.setId(3L);
        diskConfig.setAlertType("disk");
        diskConfig.setThreshold(new BigDecimal("90.00"));
        diskConfig.setEnabled(1);

        lenient().doReturn(true).when(alertRecordService).save(any());
        lenient().doReturn(true).when(alertRecordService).updateById(any());

        lenient().when(cpuStrategy.getType()).thenReturn("cpu");
        lenient().when(cpuStrategy.getDisplayName()).thenReturn("CPU");
        lenient().when(memoryStrategy.getType()).thenReturn("memory");
        lenient().when(memoryStrategy.getDisplayName()).thenReturn("内存");
        lenient().when(diskStrategy.getType()).thenReturn("disk");
        lenient().when(diskStrategy.getDisplayName()).thenReturn("磁盘");

        lenient().when(strategyRegistry.getStrategy("cpu")).thenReturn(Optional.of(cpuStrategy));
        lenient().when(strategyRegistry.getStrategy("memory")).thenReturn(Optional.of(memoryStrategy));
        lenient().when(strategyRegistry.getStrategy("disk")).thenReturn(Optional.of(diskStrategy));
    }

    @Test
    @DisplayName("CPU使用率超过阈值 - 触发新告警")
    void testCpuExceedThreshold_createNewAlert() {
        when(alertConfigService.listEnabled()).thenReturn(List.of(cpuConfig));
        doReturn(null).when(alertRecordService).findActiveRecord(eq("cpu"), isNull());
        when(cpuStrategy.extractValue(any(), eq(cpuConfig))).thenReturn(new BigDecimal("125.00"));

        Map<String, Object> serverInfo = Map.of(
                "cpu", Map.of("systemLoadAverage", 10.0, "availableProcessors", 8),
                "memory", Map.of("heapUsed", "500 MB", "heapMax", "2048 MB"),
                "disks", List.of()
        );

        List<SysAlertRecord> result = alertRecordService.checkAndAlert(serverInfo);

        assertEquals(1, result.size());
        assertEquals("cpu", result.get(0).getAlertType());
        assertEquals(1, result.get(0).getStatus());
        verify(alertRecordService, times(1)).save(any(SysAlertRecord.class));
    }

    @Test
    @DisplayName("CPU使用率低于阈值 - 不触发告警")
    void testCpuBelowThreshold_noAlert() {
        when(alertConfigService.listEnabled()).thenReturn(List.of(cpuConfig));
        doReturn(null).when(alertRecordService).findActiveRecord(eq("cpu"), isNull());
        when(cpuStrategy.extractValue(any(), eq(cpuConfig))).thenReturn(new BigDecimal("50.00"));

        Map<String, Object> serverInfo = Map.of(
                "cpu", Map.of("systemLoadAverage", 4.0, "availableProcessors", 8),
                "memory", Map.of("heapUsed", "500 MB", "heapMax", "2048 MB"),
                "disks", List.of()
        );

        List<SysAlertRecord> result = alertRecordService.checkAndAlert(serverInfo);

        assertEquals(0, result.size());
        verify(alertRecordService, never()).save(any(SysAlertRecord.class));
    }

    @Test
    @DisplayName("CPU超过阈值且存在活跃告警 - 仅更新当前值不创建新告警")
    void testCpuExceedThreshold_withExistingActiveAlert() {
        SysAlertRecord activeAlert = new SysAlertRecord();
        activeAlert.setId(1L);
        activeAlert.setAlertType("cpu");
        activeAlert.setStatus(1);
        activeAlert.setCurrentValue(new BigDecimal("85"));

        when(alertConfigService.listEnabled()).thenReturn(List.of(cpuConfig));
        doReturn(activeAlert).when(alertRecordService).findActiveRecord(eq("cpu"), isNull());
        when(cpuStrategy.extractValue(any(), eq(cpuConfig))).thenReturn(new BigDecimal("110.00"));

        Map<String, Object> serverInfo = Map.of(
                "cpu", Map.of("systemLoadAverage", 8.8, "availableProcessors", 8),
                "memory", Map.of("heapUsed", "500 MB", "heapMax", "2048 MB"),
                "disks", List.of()
        );

        List<SysAlertRecord> result = alertRecordService.checkAndAlert(serverInfo);

        assertEquals(0, result.size());
        verify(alertRecordService, never()).save(any(SysAlertRecord.class));
        verify(alertRecordService, times(1)).updateById(eq(activeAlert));
    }

    @Test
    @DisplayName("内存使用率恢复正常 - 告警自动恢复")
    void testMemoryRecover_alertStatusChangeToRecovered() {
        SysAlertRecord activeAlert = new SysAlertRecord();
        activeAlert.setId(2L);
        activeAlert.setAlertType("memory");
        activeAlert.setStatus(1);
        activeAlert.setCurrentValue(new BigDecimal("90"));

        when(alertConfigService.listEnabled()).thenReturn(List.of(memoryConfig));
        doReturn(activeAlert).when(alertRecordService).findActiveRecord(eq("memory"), isNull());
        when(memoryStrategy.extractValue(any(), eq(memoryConfig))).thenReturn(new BigDecimal("39.06"));

        Map<String, Object> serverInfo = Map.of(
                "cpu", Map.of("systemLoadAverage", 2.0, "availableProcessors", 8),
                "memory", Map.of("heapUsed", "800 MB", "heapMax", "2048 MB"),
                "disks", List.of()
        );

        Map<String, List<SysAlertRecord>> result = alertRecordService.checkAndAlertWithResult(serverInfo);

        assertTrue(result.get("newAlerts").isEmpty());
        assertEquals(1, result.get("recovered").size());
        assertEquals(2, activeAlert.getStatus());
        assertNotNull(activeAlert.getRecoverTime());
        verify(alertRecordService, times(1)).updateById(eq(activeAlert));
    }

    @Test
    @DisplayName("磁盘使用率超过阈值 - 触发告警")
    void testDiskExceedThreshold_createAlert() {
        when(alertConfigService.listEnabled()).thenReturn(List.of(diskConfig));
        doReturn(null).when(alertRecordService).findActiveRecord(eq("disk"), isNull());
        when(diskStrategy.extractValue(any(), eq(diskConfig))).thenReturn(new BigDecimal("95"));

        Map<String, Object> serverInfo = Map.of(
                "cpu", Map.of("systemLoadAverage", 2.0, "availableProcessors", 8),
                "memory", Map.of("heapUsed", "500 MB", "heapMax", "2048 MB"),
                "disks", List.of(Map.of("path", "C:", "usedPercent", "95"))
        );

        List<SysAlertRecord> result = alertRecordService.checkAndAlert(serverInfo);

        assertEquals(1, result.size());
        assertEquals("disk", result.get(0).getAlertType());
    }

    @Test
    @DisplayName("告警配置禁用 - 跳过检测")
    void testAlertConfigDisabled_skipCheck() {
        when(alertConfigService.listEnabled()).thenReturn(List.of());

        Map<String, Object> serverInfo = Map.of(
                "cpu", Map.of("systemLoadAverage", 10.0, "availableProcessors", 8),
                "memory", Map.of("heapUsed", "500 MB", "heapMax", "2048 MB"),
                "disks", List.of()
        );

        List<SysAlertRecord> result = alertRecordService.checkAndAlert(serverInfo);

        assertEquals(0, result.size());
        verify(alertRecordService, never()).save(any(SysAlertRecord.class));
    }

    @Test
    @DisplayName("多指标同时超标 - 同时触发多个告警")
    void testMultipleMetricsExceedThreshold() {
        when(alertConfigService.listEnabled()).thenReturn(List.of(cpuConfig, memoryConfig, diskConfig));
        doReturn(null).when(alertRecordService).findActiveRecord(anyString(), isNull());
        when(cpuStrategy.extractValue(any(), eq(cpuConfig))).thenReturn(new BigDecimal("112.50"));
        when(memoryStrategy.extractValue(any(), eq(memoryConfig))).thenReturn(new BigDecimal("87.89"));
        when(diskStrategy.extractValue(any(), eq(diskConfig))).thenReturn(new BigDecimal("92"));

        Map<String, Object> serverInfo = Map.of(
                "cpu", Map.of("systemLoadAverage", 9.0, "availableProcessors", 8),
                "memory", Map.of("heapUsed", "1800 MB", "heapMax", "2048 MB"),
                "disks", List.of(Map.of("path", "C:", "usedPercent", "92"))
        );

        Map<String, List<SysAlertRecord>> result = alertRecordService.checkAndAlertWithResult(serverInfo);

        assertEquals(3, result.get("newAlerts").size());
        assertEquals(0, result.get("recovered").size());
        verify(alertRecordService, times(3)).save(any(SysAlertRecord.class));
    }

    @Test
    @DisplayName("部分指标恢复、部分指标超标 - 混合场景")
    void testMixedScenario_someRecoverSomeExceed() {
        SysAlertRecord cpuAlert = new SysAlertRecord();
        cpuAlert.setId(1L);
        cpuAlert.setAlertType("cpu");
        cpuAlert.setStatus(1);

        when(alertConfigService.listEnabled()).thenReturn(List.of(cpuConfig, memoryConfig));
        doReturn(cpuAlert).when(alertRecordService).findActiveRecord(eq("cpu"), isNull());
        doReturn(null).when(alertRecordService).findActiveRecord(eq("memory"), isNull());
        when(cpuStrategy.extractValue(any(), eq(cpuConfig))).thenReturn(new BigDecimal("50.00"));
        when(memoryStrategy.extractValue(any(), eq(memoryConfig))).thenReturn(new BigDecimal("87.89"));

        Map<String, Object> serverInfo = Map.of(
                "cpu", Map.of("systemLoadAverage", 4.0, "availableProcessors", 8),
                "memory", Map.of("heapUsed", "1800 MB", "heapMax", "2048 MB"),
                "disks", List.of()
        );

        Map<String, List<SysAlertRecord>> result = alertRecordService.checkAndAlertWithResult(serverInfo);

        assertEquals(1, result.get("newAlerts").size());
        assertEquals("memory", result.get("newAlerts").get(0).getAlertType());
        assertEquals(1, result.get("recovered").size());
        assertEquals("cpu", result.get("recovered").get(0).getAlertType());
        verify(alertRecordService, times(1)).save(any(SysAlertRecord.class));
        verify(alertRecordService, times(1)).updateById(any(SysAlertRecord.class));
    }

    @Test
    @DisplayName("未注册策略的告警类型 - 跳过检测")
    void testUnregisteredStrategy_skipCheck() {
        SysAlertConfig unknownConfig = new SysAlertConfig();
        unknownConfig.setId(99L);
        unknownConfig.setAlertType("redis");
        unknownConfig.setThreshold(new BigDecimal("80.00"));
        unknownConfig.setEnabled(1);

        when(alertConfigService.listEnabled()).thenReturn(List.of(unknownConfig));
        when(strategyRegistry.getStrategy("redis")).thenReturn(Optional.empty());

        Map<String, Object> serverInfo = Map.of("redis", Map.of("connectedClients", 100));

        List<SysAlertRecord> result = alertRecordService.checkAndAlert(serverInfo);

        assertEquals(0, result.size());
        verify(alertRecordService, never()).save(any(SysAlertRecord.class));
    }

    @Test
    @DisplayName("策略提取值为null - 跳过该指标")
    void testStrategyReturnsNull_skipMetric() {
        when(alertConfigService.listEnabled()).thenReturn(List.of(cpuConfig));
        when(cpuStrategy.extractValue(any(), eq(cpuConfig))).thenReturn(null);

        Map<String, Object> serverInfo = Map.of();

        List<SysAlertRecord> result = alertRecordService.checkAndAlert(serverInfo);

        assertEquals(0, result.size());
        verify(alertRecordService, never()).save(any(SysAlertRecord.class));
    }

    @Test
    @DisplayName("获取活跃告警列表")
    void testActiveList() {
        doReturn(List.of(new SysAlertRecord())).when(alertRecordService).activeList();

        List<SysAlertRecord> result = alertRecordService.activeList();

        assertEquals(1, result.size());
    }

    @Test
    @DisplayName("获取活跃告警数量")
    void testActiveCount() {
        doReturn(3L).when(alertRecordService).activeCount();

        long count = alertRecordService.activeCount();

        assertEquals(3L, count);
    }

    @Test
    @DisplayName("活跃告警分页查询")
    void testActivePage() {
        Page<SysAlertRecord> mockPage = new Page<>();
        mockPage.setRecords(List.of(new SysAlertRecord()));
        mockPage.setTotal(5);

        doReturn(mockPage).when(alertRecordService).activePage(any(Page.class), any());

        Page<SysAlertRecord> result = alertRecordService.activePage(new Page<>(1, 10), "cpu");

        assertEquals(5, result.getTotal());
        assertEquals(1, result.getRecords().size());
    }

    @Test
    @DisplayName("历史告警分页查询")
    void testHistoryPage() {
        Page<SysAlertRecord> mockPage = new Page<>();
        mockPage.setRecords(List.of(new SysAlertRecord()));
        mockPage.setTotal(20);

        doReturn(mockPage).when(alertRecordService).historyPage(any(Page.class), any());

        Page<SysAlertRecord> result = alertRecordService.historyPage(new Page<>(1, 10), null);

        assertEquals(20, result.getTotal());
    }

    @Test
    @DisplayName("空服务器信息 - 不抛出异常")
    void testEmptyServerInfo_noException() {
        when(alertConfigService.listEnabled()).thenReturn(List.of(cpuConfig));
        when(cpuStrategy.extractValue(any(), eq(cpuConfig))).thenReturn(null);

        assertDoesNotThrow(() -> alertRecordService.checkAndAlert(Map.of()));
    }
}
