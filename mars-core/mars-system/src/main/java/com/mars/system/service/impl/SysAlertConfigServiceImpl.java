package com.mars.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.mars.system.entity.SysAlertConfig;
import com.mars.system.mapper.SysAlertConfigMapper;
import com.mars.system.service.SysAlertConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysAlertConfigServiceImpl extends ServiceImpl<SysAlertConfigMapper, SysAlertConfig> implements SysAlertConfigService {

    @Override
    public List<SysAlertConfig> listEnabled() {
        return lambdaQuery().eq(SysAlertConfig::getEnabled, 1).list();
    }

    @Override
    public List<SysAlertConfig> listAll() {
        return lambdaQuery()
                .orderByAsc(SysAlertConfig::getAlertType)
                .orderByAsc(SysAlertConfig::getMetricKey)
                .list();
    }

    @Override
    public void updateConfig(SysAlertConfig config) {
        lambdaUpdate()
                .eq(SysAlertConfig::getId, config.getId())
                .set(config.getThreshold() != null, SysAlertConfig::getThreshold, config.getThreshold())
                .set(config.getEnabled() != null, SysAlertConfig::getEnabled, config.getEnabled())
                .update();
    }

    @Override
    @Transactional
    public void updateBatch(List<SysAlertConfig> configs) {
        for (SysAlertConfig config : configs) {
            if (config.getId() != null) {
                updateConfig(config);
            }
        }
    }

    @Override
    public SysAlertConfig getByTypeAndKey(String alertType, String metricKey) {
        return lambdaQuery()
                .eq(SysAlertConfig::getAlertType, alertType)
                .and(w -> w.isNull(SysAlertConfig::getMetricKey).or().eq(SysAlertConfig::getMetricKey, metricKey == null ? "" : metricKey))
                .one();
    }
}
