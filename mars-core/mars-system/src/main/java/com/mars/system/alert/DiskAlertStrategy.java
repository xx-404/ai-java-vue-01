package com.mars.system.alert;

import com.mars.system.entity.SysAlertConfig;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Component
public class DiskAlertStrategy implements AlertMetricStrategy {

    @Override
    public String getType() {
        return "disk";
    }

    @Override
    public String getDisplayName() {
        return "磁盘";
    }

    @Override
    public BigDecimal extractValue(Map<String, Object> serverInfo, SysAlertConfig config) {
        Object disksObj = serverInfo.get("disks");
        if (!(disksObj instanceof List)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> disks = (List<Map<String, Object>>) disksObj;

        String metricKey = config.getMetricKey();
        if (StringUtils.hasText(metricKey)) {
            for (Map<String, Object> disk : disks) {
                String path = String.valueOf(disk.getOrDefault("path", ""));
                if (metricKey.equals(path)) {
                    return parseDiskPercent(disk);
                }
            }
            return null;
        }

        BigDecimal max = null;
        for (Map<String, Object> disk : disks) {
            BigDecimal pct = parseDiskPercent(disk);
            if (pct != null && (max == null || pct.compareTo(max) > 0)) {
                max = pct;
            }
        }
        return max;
    }

    private BigDecimal parseDiskPercent(Map<String, Object> disk) {
        String percentStr = String.valueOf(disk.getOrDefault("usedPercent", "0"));
        try {
            return new BigDecimal(percentStr);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
