package com.mars.system.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.mars.system.entity.SysAlertRecord;

import java.util.List;
import java.util.Map;

public interface SysAlertRecordService extends IService<SysAlertRecord> {

    Page<SysAlertRecord> activePage(Page<SysAlertRecord> page, String alertType);

    Page<SysAlertRecord> historyPage(Page<SysAlertRecord> page, String alertType);

    List<SysAlertRecord> activeList();

    List<SysAlertRecord> checkAndAlert(Map<String, Object> serverInfo);

    Map<String, List<SysAlertRecord>> checkAndAlertWithResult(Map<String, Object> serverInfo);

    long activeCount();
}
