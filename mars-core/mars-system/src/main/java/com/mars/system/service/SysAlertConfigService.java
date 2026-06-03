package com.mars.system.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.mars.system.entity.SysAlertConfig;

import java.util.List;

public interface SysAlertConfigService extends IService<SysAlertConfig> {

    List<SysAlertConfig> listEnabled();

    List<SysAlertConfig> listAll();

    void updateConfig(SysAlertConfig config);

    void updateBatch(List<SysAlertConfig> configs);

    SysAlertConfig getByTypeAndKey(String alertType, String metricKey);
}
