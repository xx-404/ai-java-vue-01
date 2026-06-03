package com.mars.system.alert;

import com.mars.system.entity.SysAlertConfig;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
public class MemoryAlertStrategy implements AlertMetricStrategy {

    @Override
    public String getType() {
        return "memory";
    }

    @Override
    public String getDisplayName() {
        return "内存";
    }

    @Override
    public BigDecimal extractValue(Map<String, Object> serverInfo, SysAlertConfig config) {
        Object memObj = serverInfo.get("memory");
        if (!(memObj instanceof Map)) {
            return null;
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> memory = (Map<String, Object>) memObj;
        double heapUsed = parseMemoryBytes(String.valueOf(memory.getOrDefault("heapUsed", "0")));
        double heapMax = parseMemoryBytes(String.valueOf(memory.getOrDefault("heapMax", "1")));
        double memPercent = heapMax > 0 ? (heapUsed / heapMax) * 100 : 0;
        return BigDecimal.valueOf(memPercent).setScale(2, RoundingMode.HALF_UP);
    }

    private double parseMemoryBytes(String text) {
        if (text == null) return 0;
        text = text.trim();
        try {
            if (text.endsWith(" GB")) return Double.parseDouble(text.replace(" GB", "")) * 1024 * 1024 * 1024;
            if (text.endsWith(" MB")) return Double.parseDouble(text.replace(" MB", "")) * 1024 * 1024;
            if (text.endsWith(" KB")) return Double.parseDouble(text.replace(" KB", "")) * 1024;
            if (text.endsWith(" B")) return Double.parseDouble(text.replace(" B", ""));
            return Double.parseDouble(text.replaceAll("[^\\d.]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
