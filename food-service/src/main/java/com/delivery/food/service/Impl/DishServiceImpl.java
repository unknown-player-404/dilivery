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

    @Override
    public void deductStock(Long id, Integer quantity) {
        //影响的行数
        int rows = dishMapper.deductStock(id, quantity);
        //rows<=0说明有异常
        if (rows<=0){
            //抛异常=告诉全局事务”这个分支失败了“，Seata会把订单和库存一起回滚
            throw new RuntimeException("菜品库存不足");
        }
    }
}
