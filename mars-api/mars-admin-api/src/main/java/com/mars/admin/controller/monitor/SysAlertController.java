package com.mars.admin.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mars.admin.websocket.MessageWebSocketHandler;
import com.mars.common.result.Result;
import com.mars.system.alert.AlertMetricStrategy;
import com.mars.system.alert.AlertMetricStrategyRegistry;
import com.mars.system.entity.SysAlertConfig;
import com.mars.system.entity.SysAlertRecord;
import com.mars.system.service.SysAlertConfigService;
import com.mars.system.service.SysAlertRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/monitor/alert")
@RequiredArgsConstructor
public class SysAlertController {

    private final SysAlertConfigService alertConfigService;
    private final SysAlertRecordService alertRecordService;
    private final MessageWebSocketHandler webSocketHandler;
    private final AlertMetricStrategyRegistry strategyRegistry;

    @GetMapping("/config/list")
    @SaCheckPermission("monitor:server:list")
    public Result<List<SysAlertConfig>> configList() {
        return Result.ok(alertConfigService.listAll());
    }

    @PutMapping("/config")
    @SaCheckPermission("monitor:server:edit")
    public Result<Void> updateConfig(@RequestBody SysAlertConfig config) {
        alertConfigService.updateConfig(config);
        return Result.ok();
    }

    @PutMapping("/config/batch")
    @SaCheckPermission("monitor:server:edit")
    public Result<Void> updateConfigBatch(@RequestBody List<SysAlertConfig> configs) {
        alertConfigService.updateBatch(configs);
        return Result.ok();
    }

    @GetMapping("/active/list")
    @SaCheckPermission("monitor:server:list")
    public Result<List<SysAlertRecord>> activeList() {
        return Result.ok(alertRecordService.activeList());
    }

    @GetMapping("/active/page")
    @SaCheckPermission("monitor:server:list")
    public Result<Page<SysAlertRecord>> activePage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String alertType) {
        return Result.ok(alertRecordService.activePage(new Page<>(page, pageSize), alertType));
    }

    @GetMapping("/history/page")
    @SaCheckPermission("monitor:server:list")
    public Result<Page<SysAlertRecord>> historyPage(
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) String alertType) {
        return Result.ok(alertRecordService.historyPage(new Page<>(page, pageSize), alertType));
    }

    @GetMapping("/count")
    @SaCheckPermission("monitor:server:list")
    public Result<Long> activeCount() {
        return Result.ok(alertRecordService.activeCount());
    }

    @PostMapping("/check")
    @SaCheckPermission("monitor:server:list")
    public Result<List<SysAlertRecord>> checkAndNotify(@RequestBody Map<String, Object> serverInfo) {
        Map<String, List<SysAlertRecord>> alertResult = alertRecordService.checkAndAlertWithResult(serverInfo);
        List<SysAlertRecord> newAlerts = alertResult.get("newAlerts");
        List<SysAlertRecord> recovered = alertResult.get("recovered");

        for (SysAlertRecord alert : newAlerts) {
            AlertMetricStrategy strategy = strategyRegistry.getStrategy(alert.getAlertType()).orElse(null);
            String content = strategy != null
                    ? strategy.formatAlertContent(alert.getCurrentValue(), alert.getThreshold())
                    : String.format("%s值 %.1f%% 超过阈值 %.1f%%", alert.getAlertType(), alert.getCurrentValue().doubleValue(), alert.getThreshold().doubleValue());
            String title = strategy != null ? strategy.formatAlertTitle() : "服务器告警 - " + alert.getAlertType();
            webSocketHandler.sendAlert(null, title, content, alert.getAlertType(), false);
        }

        for (SysAlertRecord record : recovered) {
            AlertMetricStrategy strategy = strategyRegistry.getStrategy(record.getAlertType()).orElse(null);
            String content = strategy != null
                    ? strategy.formatRecoverContent(record.getCurrentValue(), record.getThreshold())
                    : String.format("%s值已恢复至 %.1f%%（阈值 %.1f%%）", record.getAlertType(), record.getCurrentValue().doubleValue(), record.getThreshold().doubleValue());
            String title = strategy != null ? strategy.formatRecoverTitle() : "告警恢复 - " + record.getAlertType();
            webSocketHandler.sendAlert(null, title, content, record.getAlertType(), true);
        }

        return Result.ok(newAlerts);
    }

    @DeleteMapping("/record/{id}")
    @SaCheckPermission("monitor:server:remove")
    public Result<Void> deleteRecord(@PathVariable Long id) {
        alertRecordService.removeById(id);
        return Result.ok();
    }

    @DeleteMapping("/record/clean")
    @SaCheckPermission("monitor:server:remove")
    public Result<Void> cleanHistory() {
        alertRecordService.lambdaUpdate()
                .eq(SysAlertRecord::getStatus, 2)
                .remove();
        return Result.ok();
    }
}
