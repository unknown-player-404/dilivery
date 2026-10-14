package com.delivery.food.controller;

import com.delivery.food.common.Result;
import com.delivery.food.entity.Dish;
import com.delivery.food.service.DishService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/dish")
public class DishController {
    private final DishService dishService;

    public DishController(DishService dishService) {
        this.dishService = dishService;
    }

    @GetMapping("/list")
    public Result<List<Dish>> list() {
        return Result.success(dishService.list());
    }

    @GetMapping("/{id}")
    public Result<Dish> getById(@PathVariable Long id) {
        Dish dish = dishService.getById(id);
        if (dish == null) {
            return Result.fail("菜品不存在");
        }
        return Result.success(dish);
    }

    @PostMapping
    public Result<Void> add(@RequestBody Dish dish) {
        if (dish.getName() == null || dish.getName().isBlank()) {
            return Result.fail("菜品名称不能为空");
        }
        if (dish.getPrice() == null || dish.getPrice().compareTo(BigDecimal.ZERO) <=0) {
            return Result.fail("菜品价格必须大于0");
        }
        return dishService.save(dish) ? Result.success() : Result.fail("新增失败");
    }

    @PutMapping
    public Result<Void> update(@RequestBody Dish dish){
        return dishService.updateById(dish) ? Result.success() : Result.fail("更新失败");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id){
        return dishService.removeById(id) ? Result.success() : Result.fail("删除失败");
    }
}
