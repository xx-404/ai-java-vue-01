package com.mars.system.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mars.system.alert.AlertMetricStrategy;
import com.mars.system.alert.AlertMetricStrategyRegistry;
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
    private final AlertMetricStrategyRegistry strategyRegistry;

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
        return doCheckAndAlert(serverInfo).get("newAlerts");
    }

    @Override
    public Map<String, List<SysAlertRecord>> checkAndAlertWithResult(Map<String, Object> serverInfo) {
        return doCheckAndAlert(serverInfo);
    }

    private Map<String, List<SysAlertRecord>> doCheckAndAlert(Map<String, Object> serverInfo) {
        List<SysAlertConfig> configs = alertConfigService.listEnabled();
        List<SysAlertRecord> newAlerts = new ArrayList<>();
        List<SysAlertRecord> recovered = new ArrayList<>();

        for (SysAlertConfig config : configs) {
            AlertMetricStrategy strategy = strategyRegistry.getStrategy(config.getAlertType()).orElse(null);
            if (strategy == null) {
                log.debug("未找到告警策略: {}, 跳过", config.getAlertType());
                continue;
            }

            BigDecimal currentValue = strategy.extractValue(serverInfo, config);
            if (currentValue == null) {
                continue;
            }

            String metricKey = config.getMetricKey();
            SysAlertRecord activeRecord = findActiveRecord(config.getAlertType(), metricKey);

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
                    recovered.add(activeRecord);
                    log.info("告警恢复: {} 值={}% 阈值={}%",
                            config.getAlertType() + (metricKey != null ? ":" + metricKey : ""),
                            currentValue, config.getThreshold());
                }
            }
        }

        return Map.of("newAlerts", newAlerts, "recovered", recovered);
    }

    public SysAlertRecord findActiveRecord(String alertType, String metricKey) {
        return lambdaQuery()
                .eq(SysAlertRecord::getAlertType, alertType)
                .and(w -> {
                    if (metricKey == null) {
                        w.isNull(SysAlertRecord::getMetricKey).or().eq(SysAlertRecord::getMetricKey, "");
                    } else {
                        w.eq(SysAlertRecord::getMetricKey, metricKey);
                    }
                })
                .eq(SysAlertRecord::getStatus, 1)
                .one();
    }
}
