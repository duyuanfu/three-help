package com.bangbangbang.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 订单评价，对应前端 HelpOrder.review
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Review {
    private Integer stars;
    private String comment;
}
