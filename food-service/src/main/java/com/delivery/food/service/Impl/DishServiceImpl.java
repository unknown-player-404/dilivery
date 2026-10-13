package com.delivery.food.service.Impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
import com.delivery.food.entity.Dish;
import com.delivery.food.mapper.DishMapper;
import com.delivery.food.service.DishService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DishServiceImpl extends ServiceImpl<DishMapper,Dish> implements DishService {
    /**
     * 构造器注入
     */
    private final DishMapper dishMapper;

    public DishServiceImpl(DishMapper dishMapper){
        this.dishMapper=dishMapper;
    }

    @Override
    public List<Dish> list(){
        return dishMapper.selectList(
                new LambdaQueryWrapper<Dish>().orderByAsc(Dish::getId)
        );
    }
}
