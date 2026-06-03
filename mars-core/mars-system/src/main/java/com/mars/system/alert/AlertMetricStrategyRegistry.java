package com.mars.system.alert;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class AlertMetricStrategyRegistry {

    private final Map<String, AlertMetricStrategy> strategyMap = new ConcurrentHashMap<>();

    public AlertMetricStrategyRegistry(List<AlertMetricStrategy> strategies) {
        for (AlertMetricStrategy strategy : strategies) {
            strategyMap.put(strategy.getType(), strategy);
            log.info("注册告警策略: {} -> {}", strategy.getType(), strategy.getClass().getSimpleName());
        }
    }

    public Optional<AlertMetricStrategy> getStrategy(String alertType) {
        return Optional.ofNullable(strategyMap.get(alertType));
    }

    public String getDisplayName(String alertType) {
        return getStrategy(alertType)
                .map(AlertMetricStrategy::getDisplayName)
                .orElse(alertType);
    }

    public Map<String, AlertMetricStrategy> getAllStrategies() {
        return Map.copyOf(strategyMap);
    }
}
