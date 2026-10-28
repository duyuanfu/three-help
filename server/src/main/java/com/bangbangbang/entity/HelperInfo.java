package com.bangbangbang.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 帮手（志愿者）信息，对应前端 HelperInfo
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HelperInfo {
    private String id;
    private String name;
    private String tag;
    private String avatar;
    private String phone;
}
