package com.delivery.food.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.delivery.food.entity.Dish;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface DishMapper extends BaseMapper<Dish> {
    /**
     * 扣库存：库存不够时影响行数为0，不会扣成负数
     */
    @Update("UPDATE dish SET stock = stock- #{num} WHERE id = #{id} AND stock >= #{num}")
    int deductStock(@Param("id") Long id, @Param("num") Integer num);

}
