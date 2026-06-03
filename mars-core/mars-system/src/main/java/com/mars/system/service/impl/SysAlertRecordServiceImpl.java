package com.mars.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mars.system.entity.SysAlertConfig;
import com.mars.system.entity.SysAlertRecord;
import com.mars.system.mapper.SysAlertRecordMapper;
import com.mars.system.service.SysAlertConfigService;
import com.mars.system.service.SysAlertRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysAlertRecordServiceImpl extends ServiceImpl<SysAlertRecordMapper, SysAlertRecord> implements SysAlertRecordService {

    private final SysAlertConfigService alertConfigService;

    @Override
    public Page<SysAlertRecord> activePage(Page<SysAlertRecord> page, String alertType) {
        return lambdaQuery()
                .eq(SysAlertRecord::getStatus, 1)
                .eq(StringUtils.hasText(alertType), SysAlertRecord::getAlertType, alertType)
                .orderByDesc(SysAlertRecord::getTriggerTime)
                .page(page);
    }

    @Override
    public Page<SysAlertRecord> historyPage(Page<SysAlertRecord> page, String alertType) {
        return lambdaQuery()
                .eq(SysAlertRecord::getStatus, 2)
                .eq(StringUtils.hasText(alertType), SysAlertRecord::getAlertType, alertType)
                .orderByDesc(SysAlertRecord::getRecoverTime)
                .page(page);
    }

    @Override
    public List<SysAlertRecord> activeList() {
        return lambdaQuery()
                .eq(SysAlertRecord::getStatus, 1)
                .orderByDesc(SysAlertRecord::getTriggerTime)
                .list();
    }

    @Override
    public long activeCount() {
        return lambdaQuery().eq(SysAlertRecord::getStatus, 1).count();
    }

    @Override
    public List<SysAlertRecord> checkAndAlert(Map<String, Object> serverInfo) {
        List<SysAlertConfig> configs = alertConfigService.listEnabled();
        List<SysAlertRecord> newAlerts = new ArrayList<>();

        Map<String, BigDecimal> metrics = extractMetrics(serverInfo);

        for (SysAlertConfig config : configs) {
            BigDecimal currentValue = resolveMetricValue(config, metrics, serverInfo);
            if (currentValue == null) {
                continue;
            }

            String metricKey = config.getMetricKey();

            SysAlertRecord activeRecord = lambdaQuery()
                    .eq(SysAlertRecord::getAlertType, config.getAlertType())
                    .and(w -> {
                        if (metricKey == null) {
                            w.isNull(SysAlertRecord::getMetricKey).or().eq(SysAlertRecord::getMetricKey, "");
                        } else {
                            w.eq(SysAlertRecord::getMetricKey, metricKey);
                        }
                    })
                    .eq(SysAlertRecord::getStatus, 1)
                    .one();

            if (currentValue.compareTo(config.getThreshold()) >= 0) {
                if (activeRecord == null) {
                    SysAlertRecord record = new SysAlertRecord();
                    record.setAlertType(config.getAlertType());
                    record.setMetricKey(metricKey);
                    record.setCurrentValue(currentValue);
                    record.setThreshold(config.getThreshold());
                    record.setStatus(1);
                    record.setTriggerTime(LocalDateTime.now());
                    this.save(record);
                    newAlerts.add(record);
                    log.warn("告警触发: {} 值={}% 阈值={}%",
                            config.getAlertType() + (metricKey != null ? ":" + metricKey : ""),
                            currentValue, config.getThreshold());
                } else {
                    activeRecord.setCurrentValue(currentValue);
                    this.updateById(activeRecord);
                }
            } else {
                if (activeRecord != null) {
                    activeRecord.setStatus(2);
                    activeRecord.setCurrentValue(currentValue);
                    activeRecord.setRecoverTime(LocalDateTime.now());
                    this.updateById(activeRecord);
                    log.info("告警恢复: {} 值={}% 阈值={}%",
                            config.getAlertType() + (metricKey != null ? ":" + metricKey : ""),
                            currentValue, config.getThreshold());
                }
            }
        }

        return newAlerts;
    }

    private Map<String, BigDecimal> extractMetrics(Map<String, Object> serverInfo) {
        Map<String, BigDecimal> metrics = new HashMap<>();

        Object cpuObj = serverInfo.get("cpu");
        if (cpuObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> cpu = (Map<String, Object>) cpuObj;
            Object loadAvg = cpu.get("systemLoadAverage");
            if (loadAvg instanceof Number) {
                double load = ((Number) loadAvg).doubleValue();
                int processors = cpu.get("availableProcessors") instanceof Number
                        ? ((Number) cpu.get("availableProcessors")).intValue() : 1;
                double cpuPercent = processors > 0 ? Math.min(load / processors * 100, 100) : 0;
                metrics.put("cpu", BigDecimal.valueOf(Math.round(cpuPercent * 100) / 100.0));
            }
        }

        Object memObj = serverInfo.get("memory");
        if (memObj instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> memory = (Map<String, Object>) memObj;
            double heapUsed = parseMemoryBytes(String.valueOf(memory.getOrDefault("heapUsed", "0")));
            double heapMax = parseMemoryBytes(String.valueOf(memory.getOrDefault("heapMax", "1")));
            double memPercent = heapMax > 0 ? (heapUsed / heapMax) * 100 : 0;
            metrics.put("memory", BigDecimal.valueOf(Math.round(memPercent * 100) / 100.0));
        }

        Object disksObj = serverInfo.get("disks");
        if (disksObj instanceof List) {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> disks = (List<Map<String, Object>>) disksObj;
            for (Map<String, Object> disk : disks) {
                String path = String.valueOf(disk.getOrDefault("path", ""));
                String percentStr = String.valueOf(disk.getOrDefault("usedPercent", "0"));
                try {
                    metrics.put("disk:" + path, new BigDecimal(percentStr));
                } catch (NumberFormatException ignored) {}
            }
        }

        return metrics;
    }

    private BigDecimal resolveMetricValue(SysAlertConfig config, Map<String, BigDecimal> metrics, Map<String, Object> serverInfo) {
        String type = config.getAlertType();
        String key = config.getMetricKey();

        if ("disk".equals(type)) {
            if (StringUtils.hasText(key)) {
                return metrics.get("disk:" + key);
            }
            return metrics.entrySet().stream()
                    .filter(e -> e.getKey().startsWith("disk:"))
                    .map(Map.Entry::getValue)
                    .max(BigDecimal::compareTo)
                    .orElse(null);
        }

        return metrics.get(type);
    }

    private double parseMemoryBytes(String text) {
        if (text == null) return 0;
        text = text.trim();
        try {
            double value;
            if (text.endsWith(" GB")) {
                value = Double.parseDouble(text.replace(" GB", "")) * 1024 * 1024 * 1024;
            } else if (text.endsWith(" MB")) {
                value = Double.parseDouble(text.replace(" MB", "")) * 1024 * 1024;
            } else if (text.endsWith(" KB")) {
                value = Double.parseDouble(text.replace(" KB", "")) * 1024;
            } else if (text.endsWith(" B")) {
                value = Double.parseDouble(text.replace(" B", ""));
            } else {
                value = Double.parseDouble(text.replaceAll("[^\\d.]", ""));
            }
            return value;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
