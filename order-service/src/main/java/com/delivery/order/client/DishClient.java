package com.delivery.order.client;


import com.delivery.order.common.result.Result;
import com.delivery.order.dto.DishDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * 对照 food-service 的 DishController 写：
 *   @FeignClient 的 name  = 对方 spring.application.name
 *   方法上的路径和注解     = 对方接口的原样复制
 * 只写声明，不发请求；真正的 HTTP 调由 Spring 运行时代理完成。
 */

@FeignClient(name = "food-service")
public interface DishClient {
    /**
     * 查dish
     */
    @GetMapping("/dish/{id}")
    Result<DishDTO> getDishById(@PathVariable("id") Long id);

    /**
     * 扣库存
     */
    @PostMapping("/dish/deductStock")
    Result<Void> deductStock(@RequestParam("id") Long id,
                             @RequestParam("quantity") Integer quantity);
}
