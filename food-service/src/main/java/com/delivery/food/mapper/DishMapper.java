package com.delivery.food.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delivery.food.entity.Dish;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {
}
