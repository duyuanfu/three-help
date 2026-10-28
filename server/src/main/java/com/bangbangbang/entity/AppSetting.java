package com.bangbangbang.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 适老设置（原 db.ts 的 setting 单行表）
 */
@Data
@TableName("app_setting")
public class AppSetting {

    @TableId(type = IdType.INPUT)
    private Integer id;

    private Integer fontScaleLevel;
    private BigDecimal fontScale;
    private LocalDateTime updatedAt;
}
