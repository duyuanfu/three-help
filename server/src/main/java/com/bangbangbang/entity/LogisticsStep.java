package com.bangbangbang.entity;

import lombok.Data;

/**
 * 物流追踪节点，对应前端 LogisticsStep
 */
@Data
public class LogisticsStep {
    private String time;
    private String title;
    private String subtitle;
    private String code;
    private Boolean isCurrent;
}
