package com.mars.system.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("sys_alert_record")
public class SysAlertRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String alertType;

    private String metricKey;

    private BigDecimal currentValue;

    private BigDecimal threshold;

    private Integer status;

    private LocalDateTime triggerTime;

    private LocalDateTime recoverTime;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}
