package com.bangbangbang.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.bangbangbang.entity.CartItem;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CartItemMapper extends BaseMapper<CartItem> {
}
