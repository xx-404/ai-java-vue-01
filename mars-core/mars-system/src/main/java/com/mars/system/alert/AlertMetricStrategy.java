package com.mars.system.alert;

import com.mars.system.entity.SysAlertConfig;

import java.math.BigDecimal;
import java.util.Map;

public interface AlertMetricStrategy {

    String getType();

    String getDisplayName();

    BigDecimal extractValue(Map<String, Object> serverInfo, SysAlertConfig config);

    default String formatAlertContent(BigDecimal currentValue, BigDecimal threshold) {
        return String.format("%s使用率 %.1f%% 超过阈值 %.1f%%",
                getDisplayName(), currentValue.doubleValue(), threshold.doubleValue());
    }

    default String formatRecoverContent(BigDecimal currentValue, BigDecimal threshold) {
        return String.format("%s使用率已恢复至 %.1f%%（阈值 %.1f%%）",
                getDisplayName(), currentValue.doubleValue(), threshold.doubleValue());
    }

    default String formatAlertTitle() {
        return "服务器告警 - " + getDisplayName();
    }

    default String formatRecoverTitle() {
        return "告警恢复 - " + getDisplayName();
    }
}
