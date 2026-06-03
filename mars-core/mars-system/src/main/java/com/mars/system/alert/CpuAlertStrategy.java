package com.mars.system.alert;

import com.mars.system.entity.SysAlertConfig;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
public class CpuAlertStrategy implements AlertMetricStrategy {

    @Override
    public String getType() {
        return "cpu";
    }

    @Override
    public String getDisplayName() {
        return "CPU";
    }

    @Override
    public BigDecimal extractValue(Map<String, Object> serverInfo, SysAlertConfig config) {
        Object cpuObj = serverInfo.get("cpu");
        if (!(cpuObj instanceof Map)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> cpu = (Map<String, Object>) cpuObj;
        Object loadAvg = cpu.get("systemLoadAverage");
        if (!(loadAvg instanceof Number)) {
            return null;
        }
        double load = ((Number) loadAvg).doubleValue();
        int processors = cpu.get("availableProcessors") instanceof Number
                ? ((Number) cpu.get("availableProcessors")).intValue() : 1;
        double cpuPercent = processors > 0 ? Math.min(load / processors * 100, 100) : 0;
        return BigDecimal.valueOf(cpuPercent).setScale(2, RoundingMode.HALF_UP);
    }
}
