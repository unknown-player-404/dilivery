package com.delivery.food.service;

import com.baomidou.mybatisplus.spring.service.IService;
import com.delivery.food.entity.Dish;

/**
 * IService覆盖
 */
public interface DishService extends IService<Dish> {
    /**扣库存：库存不足时抛异常（触发全局事务回滚）*/
    void deductStock(Long id,Integer quantity);
}
