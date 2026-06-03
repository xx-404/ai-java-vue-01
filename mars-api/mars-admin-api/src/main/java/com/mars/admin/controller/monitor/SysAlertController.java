package com.mars.admin.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mars.admin.websocket.MessageWebSocketHandler;
import com.mars.common.result.Result;
import com.mars.system.entity.SysAlertConfig;
import com.mars.system.entity.SysAlertRecord;
import com.mars.system.service.SysAlertConfigService;
import com.mars.system.service.SysAlertRecordService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/monitor/alert")
@RequiredArgsConstructor
public class SysAlertController {

    private final SysAlertConfigService alertConfigService;
    private final SysAlertRecordService alertRecordService;
    private final MessageWebSocketHandler webSocketHandler;

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
    public Result<List<SysAlertRecord>> checkAndNotify(@RequestBody java.util.Map<String, Object> serverInfo) {
        List<SysAlertRecord> newAlerts = alertRecordService.checkAndAlert(serverInfo);

        for (SysAlertRecord alert : newAlerts) {
            String typeName = getTypeName(alert.getAlertType());
            String content = String.format("%s使用率 %.1f%% 超过阈值 %.1f%%",
                    typeName, alert.getCurrentValue().doubleValue(), alert.getThreshold().doubleValue());
            webSocketHandler.sendNotice(null, "服务器告警 - " + typeName, content);
        }

        List<SysAlertRecord> recovered = alertRecordService.lambdaQuery()
                .eq(SysAlertRecord::getStatus, 2)
                .ge(SysAlertRecord::getRecoverTime, java.time.LocalDateTime.now().minusMinutes(1))
                .list();

        for (SysAlertRecord record : recovered) {
            String typeName = getTypeName(record.getAlertType());
            String content = String.format("%s使用率已恢复至 %.1f%%（阈值 %.1f%%）",
                    typeName, record.getCurrentValue().doubleValue(), record.getThreshold().doubleValue());
            webSocketHandler.sendNotice(null, "告警恢复 - " + typeName, content);
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

    private String getTypeName(String type) {
        return switch (type) {
            case "cpu" -> "CPU";
            case "memory" -> "内存";
            case "disk" -> "磁盘";
            default -> type;
        };
    }
}
